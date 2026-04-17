package io.github.zxuhan.j2k.eval.report

import io.github.zxuhan.j2k.eval.model.AggregateAnalysis
import io.github.zxuhan.j2k.eval.model.CompareFileResult
import io.github.zxuhan.j2k.eval.model.ComparativeReport
import io.github.zxuhan.j2k.eval.model.CompileFileResult
import io.github.zxuhan.j2k.eval.model.CompileReport
import io.github.zxuhan.j2k.eval.model.FileAnalysis
import io.github.zxuhan.j2k.eval.model.IdiomMetrics
import io.github.zxuhan.j2k.eval.model.JavaIsmMetrics
import io.github.zxuhan.j2k.eval.model.Report
import io.github.zxuhan.j2k.eval.model.StructuralMetrics
import java.util.Locale

/**
 * Hand-rolled JSON writer. Deterministic key order + stable numeric formatting — the
 * `ReporterTest` golden file locks the byte layout, so adding new fields means updating
 * both the encoder and the golden together.
 */
class JsonReporter {

    fun encode(report: Report): String = buildString {
        append("{\n")
        indent(1); key("aggregate"); append(encodeAggregate(report.aggregate, 1)); comma()
        indent(1); key("comparative"); append(encodeNullable(report.comparative, 1, ::encodeComparative)); comma()
        indent(1); key("compile"); append(encodeNullable(report.compile, 1, ::encodeCompile)); nl()
        append("}")
    }

    private fun encodeAggregate(a: AggregateAnalysis, depth: Int): String = buildString {
        append("{\n")
        indent(depth + 1); key("fileCount"); append(a.fileCount); comma()
        indent(depth + 1); key("totalCodeLines"); append(a.totalCodeLines); comma()
        indent(depth + 1); key("totalNotNullAssertions"); append(a.totalNotNullAssertions); comma()
        indent(depth + 1); key("files"); append(encodeList(a.files, depth + 1) { f, d -> encodeFile(f, d) }); nl()
        indent(depth); append("}")
    }

    private fun encodeFile(f: FileAnalysis, depth: Int): String = buildString {
        append("{\n")
        indent(depth + 1); key("path"); append(str(f.path)); comma()
        indent(depth + 1); key("structural"); append(encodeStructural(f.structural, depth + 1)); comma()
        indent(depth + 1); key("idiom"); append(encodeIdiom(f.idiom, depth + 1)); comma()
        indent(depth + 1); key("javaIsm"); append(encodeJavaIsm(f.javaIsm, depth + 1)); nl()
        indent(depth); append("}")
    }

    private fun encodeStructural(m: StructuralMetrics, depth: Int): String = obj(depth,
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

    private fun encodeIdiom(m: IdiomMetrics, depth: Int): String = obj(depth,
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

    private fun encodeJavaIsm(m: JavaIsmMetrics, depth: Int): String = obj(depth,
        "notNullAssertions" to m.notNullAssertions,
        "notNullAssertionsPerKloc" to m.notNullAssertionsPerKloc,
        "explicitGetterSetterPairs" to m.explicitGetterSetterPairs,
        "javaUtilCollectionRefs" to m.javaUtilCollectionRefs,
        "ifNotNullGuards" to m.ifNotNullGuards,
        "cStyleForLoops" to m.cStyleForLoops,
        "synchronizedAnnotations" to m.synchronizedAnnotations,
        "platformTypeReferences" to m.platformTypeReferences,
    )

    private fun encodeComparative(c: ComparativeReport, depth: Int): String = buildString {
        append("{\n")
        indent(depth + 1); key("referenceRoot"); append(str(c.referenceRoot)); comma()
        indent(depth + 1); key("pairCount"); append(c.pairCount); comma()
        indent(depth + 1); key("unmatchedConvertedCount"); append(c.unmatchedConvertedCount); comma()
        indent(depth + 1); key("unmatchedReferenceCount"); append(c.unmatchedReferenceCount); comma()
        indent(depth + 1); key("meanSimilarity"); append(num(c.meanSimilarity)); comma()
        indent(depth + 1); key("medianSimilarity"); append(num(c.medianSimilarity)); comma()
        indent(depth + 1); key("p90Similarity"); append(num(c.p90Similarity)); comma()
        indent(depth + 1); key("files"); append(encodeList(c.files, depth + 1) { f, d -> encodeCompareFile(f, d) }); comma()
        indent(depth + 1); key("topDivergent"); append(encodeList(c.topDivergent, depth + 1) { f, d -> encodeCompareFile(f, d) }); nl()
        indent(depth); append("}")
    }

    private fun encodeCompareFile(f: CompareFileResult, depth: Int): String = obj(depth,
        "path" to f.path,
        "referencePath" to f.referencePath,
        "similarity" to f.similarity,
        "convertedTokens" to f.convertedTokens,
        "referenceTokens" to f.referenceTokens,
    )

    private fun encodeCompile(c: CompileReport, depth: Int): String = buildString {
        append("{\n")
        indent(depth + 1); key("compiledCount"); append(c.compiledCount); comma()
        indent(depth + 1); key("failedCount"); append(c.failedCount); comma()
        indent(depth + 1); key("passRate"); append(num(c.passRate)); comma()
        indent(depth + 1); key("files"); append(encodeList(c.files, depth + 1) { f, d -> encodeCompileFile(f, d) }); nl()
        indent(depth); append("}")
    }

    private fun encodeCompileFile(f: CompileFileResult, depth: Int): String = buildString {
        append("{\n")
        indent(depth + 1); key("path"); append(str(f.path)); comma()
        indent(depth + 1); key("compiled"); append(f.compiled.toString()); comma()
        indent(depth + 1); key("errors"); append(encodeStringList(f.errors, depth + 1)); nl()
        indent(depth); append("}")
    }

    private fun <T> encodeNullable(value: T?, depth: Int, encode: (T, Int) -> String): String =
        if (value == null) "null" else encode(value, depth)

    private fun <T> encodeList(items: List<T>, depth: Int, encode: (T, Int) -> String): String = buildString {
        if (items.isEmpty()) {
            append("[]")
            return@buildString
        }
        append("[\n")
        items.forEachIndexed { i, item ->
            indent(depth + 1); append(encode(item, depth + 1))
            if (i != items.lastIndex) append(",")
            nl()
        }
        indent(depth); append("]")
    }

    private fun encodeStringList(items: List<String>, depth: Int): String = buildString {
        if (items.isEmpty()) {
            append("[]")
            return@buildString
        }
        append("[\n")
        items.forEachIndexed { i, item ->
            indent(depth + 1); append(str(item))
            if (i != items.lastIndex) append(",")
            nl()
        }
        indent(depth); append("]")
    }

    private fun obj(depth: Int, vararg entries: Pair<String, Any>): String = buildString {
        append("{\n")
        entries.forEachIndexed { i, (k, v) ->
            indent(depth + 1); key(k); append(scalar(v))
            if (i != entries.lastIndex) append(",")
            nl()
        }
        indent(depth); append("}")
    }

    private fun scalar(v: Any): String = when (v) {
        is Double -> num(v)
        is Float -> num(v.toDouble())
        is Number, is Boolean -> v.toString()
        is String -> str(v)
        else -> str(v.toString())
    }

    private fun num(d: Double): String =
        if (!d.isFinite()) "0" else String.format(Locale.ROOT, "%.4f", d)

    private fun str(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private fun StringBuilder.indent(depth: Int) { repeat(depth) { append("  ") } }
    private fun StringBuilder.key(name: String) { append("\"").append(name).append("\": ") }
    private fun StringBuilder.comma() { append(",\n") }
    private fun StringBuilder.nl() { append("\n") }
}
