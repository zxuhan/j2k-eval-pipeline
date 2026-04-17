package io.github.zxuhan.j2k.eval

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.github.zxuhan.j2k.eval.analysis.IdiomAnalyzer
import io.github.zxuhan.j2k.eval.analysis.JavaIsmAnalyzer
import io.github.zxuhan.j2k.eval.analysis.StructuralAnalyzer
import io.github.zxuhan.j2k.eval.model.AggregateAnalysis
import io.github.zxuhan.j2k.eval.model.FileAnalysis
import io.github.zxuhan.j2k.eval.model.IdiomMetrics
import io.github.zxuhan.j2k.eval.model.JavaIsmMetrics
import io.github.zxuhan.j2k.eval.model.StructuralMetrics
import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.walk
import kotlin.io.path.writeText

fun main(args: Array<String>) = Root().subcommands(Analyze()).main(args)

private class Root : NoOpCliktCommand(name = "j2k-eval")

private class Analyze : CliktCommand(name = "analyze") {

    private val input by option("--input", help = "Directory of converted Kotlin files")
        .path(mustExist = true, canBeFile = false, canBeDir = true)
        .required()

    private val out by option("--out", help = "Write JSON to this path instead of stdout")
        .path(canBeFile = true, canBeDir = false)

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
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
            val json = Json.encode(aggregate)
            out?.writeText(json) ?: println(json)
        }
    }

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    private fun collectKotlinFiles(root: Path): List<Path> =
        if (!root.isDirectory()) emptyList()
        else root.walk().filter { it.extension == "kt" }.sorted().toList()
}

private object Json {
    fun encode(a: AggregateAnalysis): String = buildString {
        append("{\n")
        append("  \"fileCount\": ").append(a.fileCount).append(",\n")
        append("  \"totalCodeLines\": ").append(a.totalCodeLines).append(",\n")
        append("  \"totalNotNullAssertions\": ").append(a.totalNotNullAssertions).append(",\n")
        append("  \"files\": [\n")
        a.files.forEachIndexed { i, f ->
            append(encodeFile(f, indent = "    "))
            if (i != a.files.lastIndex) append(",")
            append("\n")
        }
        append("  ]\n}")
    }

    private fun encodeFile(f: FileAnalysis, indent: String): String = buildString {
        append(indent).append("{\n")
        append(indent).append("  \"path\": ").append(str(f.path)).append(",\n")
        append(indent).append("  \"structural\": ").append(encode(f.structural, "$indent  ")).append(",\n")
        append(indent).append("  \"idiom\": ").append(encode(f.idiom, "$indent  ")).append(",\n")
        append(indent).append("  \"javaIsm\": ").append(encode(f.javaIsm, "$indent  ")).append("\n")
        append(indent).append("}")
    }

    private fun encode(m: StructuralMetrics, indent: String): String = obj(indent,
        "classes" to m.classes,
        "objects" to m.objects,
        "interfaces" to m.interfaces,
        "functions" to m.functions,
        "properties" to m.properties,
        "maxNestingDepth" to m.maxNestingDepth,
        "meanNestingDepth" to m.meanNestingDepth,
        "codeLines" to m.codeLines,
        "commentLines" to m.commentLines,
        "commentRatio" to m.commentRatio,
    )

    private fun encode(m: IdiomMetrics, indent: String): String = obj(indent,
        "dataClasses" to m.dataClasses,
        "objectSingletons" to m.objectSingletons,
        "companionObjects" to m.companionObjects,
        "lambdaArguments" to m.lambdaArguments,
        "samObjectExpressions" to m.samObjectExpressions,
        "trailingLambdas" to m.trailingLambdas,
        "whenExpressions" to m.whenExpressions,
        "ifElseChains" to m.ifElseChains,
        "stringTemplates" to m.stringTemplates,
        "scopeFunctionCalls" to m.scopeFunctionCalls,
        "propertyAccessors" to m.propertyAccessors,
    )

    private fun encode(m: JavaIsmMetrics, indent: String): String = obj(indent,
        "notNullAssertions" to m.notNullAssertions,
        "notNullAssertionsPerKloc" to m.notNullAssertionsPerKloc,
        "explicitGetterSetterPairs" to m.explicitGetterSetterPairs,
        "javaUtilCollectionRefs" to m.javaUtilCollectionRefs,
        "ifNotNullGuards" to m.ifNotNullGuards,
        "cStyleForLoops" to m.cStyleForLoops,
        "synchronizedAnnotations" to m.synchronizedAnnotations,
        "platformTypeReferences" to m.platformTypeReferences,
    )

    private fun obj(indent: String, vararg entries: Pair<String, Any>): String = buildString {
        append("{\n")
        entries.forEachIndexed { i, (k, v) ->
            append(indent).append("  ").append(str(k)).append(": ").append(scalar(v))
            if (i != entries.lastIndex) append(",")
            append("\n")
        }
        append(indent).append("}")
    }

    private fun scalar(v: Any): String = when (v) {
        is Double -> if (v.isFinite()) "%.4f".format(v) else "0"
        is Number, is Boolean -> v.toString()
        else -> str(v.toString())
    }

    private fun str(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}
