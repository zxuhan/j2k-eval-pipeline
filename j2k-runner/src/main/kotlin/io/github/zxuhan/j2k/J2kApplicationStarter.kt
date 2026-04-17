package io.github.zxuhan.j2k

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.J2kConverterExtension
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

/**
 * Headless driver. Wired in plugin.xml as `<appStarter id="j2k-batch" .../>`.
 * Invoked via `./gradlew :j2k-runner:runIde --args="j2k-batch --input ... --output ... --diagnostics ..."`.
 */
class J2kApplicationStarter : ApplicationStarter {

    private val log = Logger.getInstance(J2kApplicationStarter::class.java)

    @Suppress("OVERRIDE_DEPRECATION")
    override val commandName: String get() = "j2k-batch"

    override fun main(args: List<String>) {
        // The first arg is always the commandName itself per ApplicationStarter contract.
        val parsed = Args.parse(args.drop(1))
        val diag = Diagnostics(parsed.converterKind.name, parsed.postProcess)

        // J2K converter asserts background thread, and the bootstrap path needs to schedule
        // write actions back onto EDT — so we MUST NOT block EDT here. Run off-EDT and let
        // the pooled thread own the lifetime: EDT returns immediately, the process exits
        // from the background thread once conversion is done.
        ApplicationManager.getApplication().executeOnPooledThread {
            val exitCode = try {
                convert(parsed, diag)
                diag.writeJson(parsed.diagnosticsFile)
                if (diag.totalErrors() > 0) 1 else 0
            } catch (t: Throwable) {
                log.error("j2k-batch crashed", t)
                System.err.println("j2k-batch crashed: ${t.message}")
                t.printStackTrace(System.err)
                runCatching { diag.writeJson(parsed.diagnosticsFile) }
                2
            }
            exitProcess(exitCode)
        }
    }

    private fun convert(parsed: Args, diag: Diagnostics) {
        val (project, module, inputRoot) = ProjectBootstrap.open(parsed.inputDir)

        val javaFiles: List<PsiJavaFile> = ReadAction.compute<List<PsiJavaFile>, RuntimeException> {
            val psiManager = PsiManager.getInstance(project)
            val collected = mutableListOf<PsiJavaFile>()
            VfsUtilCore.iterateChildrenRecursively(inputRoot, /* filter = */ null) { vf ->
                if (!vf.isDirectory && vf.extension.equals("java", ignoreCase = true)) {
                    (psiManager.findFile(vf) as? PsiJavaFile)?.let { collected += it }
                }
                true
            }
            collected.sortedBy { it.virtualFile.path }
        }

        if (javaFiles.isEmpty()) {
            System.err.println("no .java files found under ${parsed.inputDir}")
            return
        }

        val available = J2kConverterExtension.EP_NAME.extensionList
        val extension = available.firstOrNull { it.kind == parsed.converterKind }
            ?: available.firstOrNull()
            ?: error("no J2kConverterExtension registered at all")
        if (extension.kind != parsed.converterKind) {
            println("requested ${parsed.converterKind} not registered; falling back to ${extension.kind} " +
                "(available: ${available.map { it.kind }})")
        } else {
            println("using converter kind=${extension.kind}")
        }
        diag.converterKind = extension.kind.name

        val settings = ConverterSettings.defaultSettings
        val converter = extension.createJavaToKotlinConverter(project, module, settings, /* targetFile = */ null)
        val postProcessor = if (parsed.postProcess) extension.createPostProcessor(/* formatCode = */ true) else NoopPostProcessor

        val totalMs = measureTimeMillis {
            // Converter manages its own read/write actions; do not wrap.
            val result = converter.filesToKotlin(javaFiles, postProcessor, EmptyProgressIndicator()).results
            check(result.size == javaFiles.size) { "converter returned ${result.size} results for ${javaFiles.size} inputs" }

            for ((javaFile, kotlinSource) in javaFiles.zip(result)) {
                val sourcePath = javaFile.virtualFile.path
                val rel = VfsUtilCore.getRelativePath(javaFile.virtualFile, inputRoot)
                    ?: error("input file not under input root: $sourcePath")
                val outRel = rel.removeSuffix(".java") + ".kt"
                val outPath = parsed.outputDir.resolve(outRel)
                val perFileMs = measureTimeMillis {
                    outPath.createParentDirectories()
                    Files.writeString(outPath, kotlinSource)
                }
                diag.record(sourcePath, perFileMs, warningsCount = 0, errorsCount = 0)
                println("converted $sourcePath -> $outPath")
            }
        }

        println("done. ${javaFiles.size} files in ${totalMs}ms.")
    }
}

private object NoopPostProcessor : org.jetbrains.kotlin.j2k.PostProcessor {
    override val phasesCount: Int = 0
    override fun insertImport(file: org.jetbrains.kotlin.psi.KtFile, fqName: org.jetbrains.kotlin.name.FqName) = Unit
    override fun doAdditionalProcessing(
        target: org.jetbrains.kotlin.j2k.PostProcessingTarget,
        converterContext: org.jetbrains.kotlin.j2k.ConverterContext?,
        onPhaseChanged: ((Int, String) -> Unit)?,
    ) = Unit
}
