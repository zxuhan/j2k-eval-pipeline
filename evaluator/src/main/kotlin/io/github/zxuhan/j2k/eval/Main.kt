package io.github.zxuhan.j2k.eval

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.github.zxuhan.j2k.eval.analysis.ComparativeAnalyzer
import io.github.zxuhan.j2k.eval.analysis.CompileAnalyzer
import io.github.zxuhan.j2k.eval.analysis.IdiomAnalyzer
import io.github.zxuhan.j2k.eval.analysis.JavaIsmAnalyzer
import io.github.zxuhan.j2k.eval.analysis.StructuralAnalyzer
import io.github.zxuhan.j2k.eval.model.AggregateAnalysis
import io.github.zxuhan.j2k.eval.model.FileAnalysis
import io.github.zxuhan.j2k.eval.model.Report
import io.github.zxuhan.j2k.eval.postprocess.PostProcessor
import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import io.github.zxuhan.j2k.eval.report.JsonReporter
import io.github.zxuhan.j2k.eval.report.MarkdownReporter
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.walk
import kotlin.io.path.writeText

fun main(args: Array<String>) = Root().subcommands(Analyze(), Postprocess()).main(args)

private class Root : NoOpCliktCommand(name = "j2k-eval")

private class Postprocess : CliktCommand(name = "postprocess") {

    private val input by option("--input", help = "Directory of converted Kotlin files to fix")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .required()

    private val output by option("--output", help = "Directory where the rewritten Kotlin tree is written")
        .path(canBeFile = false, canBeDir = true)
        .required()

    override fun run() {
        KotlinPsiFactory().use { psi ->
            val report = PostProcessor(psi).run(input, output)
            System.err.println("postprocess: ${report.filesChanged}/${report.files.size} files changed, ${report.importsAddedTotal} imports added")
            report.files.filter { it.changed }.take(20).forEach {
                System.err.println("  ${it.path} += ${it.addedImports}")
            }
        }
    }
}

private class Analyze : CliktCommand(name = "analyze") {

    private val input by option("--input", help = "Directory of converted Kotlin files")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .required()

    private val reference by option("--reference", help = "Directory of reference Kotlin files (e.g. hand-migrated OkHttp 4.x) for comparative analysis")
        .path(mustExist = true, canBeFile = false, canBeDir = true)

    private val classpath by option("--classpath", help = "File containing compile classpath (one jar per line); enables compile check")
        .path(mustExist = true, canBeFile = true, canBeDir = false)

    private val outDir by option("--out-dir", help = "Write report.json and report.md into this directory")
        .path(canBeFile = false, canBeDir = true)

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        val structural = StructuralAnalyzer()
        val idiom = IdiomAnalyzer()
        val javaIsm = JavaIsmAnalyzer()

        KotlinPsiFactory().use { psi ->
            val kotlinFiles = collectKotlinFiles(input)
            val fileAnalyses = kotlinFiles.map { path ->
                val ktFile = psi.parse(path)
                FileAnalysis(
                    path = path.toString(),
                    structural = structural.analyze(ktFile),
                    idiom = idiom.analyze(ktFile),
                    javaIsm = javaIsm.analyze(ktFile),
                )
            }
            val aggregate = AggregateAnalysis(fileAnalyses)

            val comparative = reference?.let { ref ->
                ComparativeAnalyzer(psi).analyze(input, ref)
            }
            val compile = classpath?.let { cp ->
                CompileAnalyzer().analyze(input, cp)
            }

            val report = Report(aggregate = aggregate, comparative = comparative, compile = compile)

            val json = JsonReporter().encode(report)
            val md = MarkdownReporter().render(report)

            val dest = outDir
            if (dest == null) {
                println(json)
            } else {
                dest.createDirectories()
                dest.resolve("report.json").writeText(json)
                dest.resolve("report.md").writeText(md)
                System.err.println("wrote ${dest.resolve("report.json")}")
                System.err.println("wrote ${dest.resolve("report.md")}")
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun collectKotlinFiles(root: Path): List<Path> =
        if (!root.isDirectory()) emptyList()
        else root.walk().filter { it.extension == "kt" }.sorted().toList()
}
