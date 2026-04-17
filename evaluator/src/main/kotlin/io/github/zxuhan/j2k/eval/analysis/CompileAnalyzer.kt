package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.model.CompileFileResult
import io.github.zxuhan.j2k.eval.model.CompileReport
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * Runs `K2JVMCompiler` against the converted corpus once and groups diagnostics per file.
 * Mainly feeds hypothesis H5 ("x% of converted files fail to compile").
 *
 * Not a full PSI-level analysis — it's an honest yes/no: "did kotlinc accept this file?"
 * with the resolved external classpath (Okio + annotations etc. for OkHttp). Cascading
 * errors from a broken upstream file will inflate downstream failure counts slightly;
 * that's acceptable for a corpus-wide signal.
 */
class CompileAnalyzer {

    @OptIn(ExperimentalPathApi::class)
    fun analyze(convertedRoot: Path, classpathFile: Path?): CompileReport {
        val kotlinFiles =
            if (!convertedRoot.isDirectory()) emptyList()
            else convertedRoot.walk().filter { it.extension == "kt" }.sorted().toList()
        if (kotlinFiles.isEmpty()) return CompileReport(0, 0, 0.0, emptyList())

        val userClasspath = classpathFile
            ?.let { Files.readAllLines(it).map(String::trim).filter { line -> line.isNotEmpty() } }
            .orEmpty()
        // kotlin-compiler-embeddable doesn't ship with a standalone stdlib jar on its own
        // classpath lookup, so K2JVMCompiler.noStdlib=false still complains about
        // "Cannot access built-in declaration 'kotlin.Any'". Append the stdlib jar we can
        // locate via PathUtil explicitly.
        val stdlibPath = PathUtil.getResourcePathForClass(Unit::class.java).absolutePath
        val fullClasspath = (userClasspath + listOf(stdlibPath)).distinct()

        val messages = CollectingMessageCollector()
        val tempOut = Files.createTempDirectory("j2k-eval-compile-check")

        val args = K2JVMCompilerArguments().apply {
            freeArgs = listOf(convertedRoot.toAbsolutePath().toString())
            destination = tempOut.toAbsolutePath().toString()
            moduleName = "j2k-eval-compile-check"
            classpath = fullClasspath.joinToString(File.pathSeparator)
            jvmTarget = "17"
            noStdlib = true // we added it manually above
            noReflect = true
            suppressWarnings = true
            verbose = false
            allWarningsAsErrors = false
        }

        try {
            K2JVMCompiler().exec(messages, Services.EMPTY, args)
        } finally {
            tempOut.toFile().deleteRecursively()
        }

        val errorsByPath = mutableMapOf<String, MutableList<String>>()
        for (m in messages.all) {
            if (m.severity != CompilerMessageSeverity.ERROR) continue
            val path = m.location?.path ?: continue
            val bucket = errorsByPath.getOrPut(canonical(path)) { mutableListOf() }
            if (bucket.size < MAX_ERRORS_PER_FILE) {
                bucket += m.message.lineSequence().first().take(200)
            }
        }

        val results = kotlinFiles.map { p ->
            val key = canonical(p.toAbsolutePath().toString())
            val errs = errorsByPath[key].orEmpty()
            CompileFileResult(
                path = p.toString(),
                compiled = errs.isEmpty(),
                errors = errs,
            )
        }
        val passed = results.count { it.compiled }
        return CompileReport(
            compiledCount = passed,
            failedCount = results.size - passed,
            passRate = if (results.isEmpty()) 0.0 else passed.toDouble() / results.size,
            files = results,
        )
    }

    private fun canonical(path: String): String =
        runCatching { File(path).canonicalPath }.getOrDefault(path)

    private class CollectingMessageCollector : MessageCollector {
        data class Msg(
            val severity: CompilerMessageSeverity,
            val message: String,
            val location: CompilerMessageSourceLocation?,
        )

        val all = mutableListOf<Msg>()

        override fun clear() { all.clear() }

        override fun hasErrors(): Boolean =
            all.any { it.severity == CompilerMessageSeverity.ERROR }

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            all += Msg(severity, message, location)
        }
    }

    companion object {
        private const val MAX_ERRORS_PER_FILE = 5
    }
}
