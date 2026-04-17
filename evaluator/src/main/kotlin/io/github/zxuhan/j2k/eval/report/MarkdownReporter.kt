package io.github.zxuhan.j2k.eval.report

import io.github.zxuhan.j2k.eval.model.AggregateAnalysis
import io.github.zxuhan.j2k.eval.model.ComparativeReport
import io.github.zxuhan.j2k.eval.model.CompileReport
import io.github.zxuhan.j2k.eval.model.Report
import java.util.Locale

/**
 * Reviewer-readable rendering of a `Report`. Numbers only — no hypothesis commentary;
 * that lives in the hand-curated `EVALUATION_REPORT.md` at commit 9.
 */
class MarkdownReporter {

    fun render(report: Report): String = buildString {
        appendLine("# j2k evaluation report")
        appendLine()
        renderSummary(report.aggregate)
        appendLine()
        renderStructural(report.aggregate)
        appendLine()
        renderIdiom(report.aggregate)
        appendLine()
        renderJavaIsm(report.aggregate)
        report.comparative?.let {
            appendLine()
            renderComparative(it)
        }
        report.compile?.let {
            appendLine()
            renderCompile(it)
        }
    }

    private fun StringBuilder.renderSummary(a: AggregateAnalysis) {
        appendLine("## Summary")
        appendLine()
        appendLine("- Files: ${a.fileCount}")
        appendLine("- Total code lines: ${a.totalCodeLines}")
        appendLine("- Total `!!` assertions: ${a.totalNotNullAssertions}")
    }

    private fun StringBuilder.renderStructural(a: AggregateAnalysis) {
        val sums = a.files.map { it.structural }
        appendLine("## Structural")
        appendLine()
        appendLine("| metric | total |")
        appendLine("|---|---:|")
        appendLine("| classes | ${sums.sumOf { it.classes }} |")
        appendLine("| objects | ${sums.sumOf { it.objects }} |")
        appendLine("| interfaces | ${sums.sumOf { it.interfaces }} |")
        appendLine("| functions | ${sums.sumOf { it.functions }} |")
        appendLine("| properties | ${sums.sumOf { it.properties }} |")
        appendLine("| code lines | ${sums.sumOf { it.codeLines }} |")
        appendLine("| comment lines | ${sums.sumOf { it.commentLines }} |")
        appendLine("| max nesting depth (max-of-max) | ${sums.maxOfOrNull { it.maxNestingDepth } ?: 0} |")
    }

    private fun StringBuilder.renderIdiom(a: AggregateAnalysis) {
        val sums = a.files.map { it.idiom }
        appendLine("## Idiom")
        appendLine()
        appendLine("| metric | total |")
        appendLine("|---|---:|")
        appendLine("| data classes | ${sums.sumOf { it.dataClasses }} |")
        appendLine("| object singletons | ${sums.sumOf { it.objectSingletons }} |")
        appendLine("| companion objects | ${sums.sumOf { it.companionObjects }} |")
        appendLine("| lambda arguments | ${sums.sumOf { it.lambdaArguments }} |")
        appendLine("| SAM object expressions | ${sums.sumOf { it.samObjectExpressions }} |")
        appendLine("| trailing lambdas | ${sums.sumOf { it.trailingLambdas }} |")
        appendLine("| when expressions | ${sums.sumOf { it.whenExpressions }} |")
        appendLine("| if/else chains | ${sums.sumOf { it.ifElseChains }} |")
        appendLine("| string templates | ${sums.sumOf { it.stringTemplates }} |")
        appendLine("| scope fn calls (let/run/apply/also/with) | ${sums.sumOf { it.scopeFunctionCalls }} |")
        appendLine("| property accessors | ${sums.sumOf { it.propertyAccessors }} |")
    }

    private fun StringBuilder.renderJavaIsm(a: AggregateAnalysis) {
        val sums = a.files.map { it.javaIsm }
        val totalLines = a.files.sumOf { it.structural.codeLines + it.structural.commentLines }
        val nnPerKloc = if (totalLines == 0) 0.0 else sums.sumOf { it.notNullAssertions } * 1000.0 / totalLines
        appendLine("## Java-isms")
        appendLine()
        appendLine("| metric | total |")
        appendLine("|---|---:|")
        appendLine("| `!!` assertions | ${sums.sumOf { it.notNullAssertions }} |")
        appendLine("| `!!` per kloc | ${num(nnPerKloc)} |")
        appendLine("| explicit getter/setter pairs | ${sums.sumOf { it.explicitGetterSetterPairs }} |")
        appendLine("| `java.util.*` collection refs | ${sums.sumOf { it.javaUtilCollectionRefs }} |")
        appendLine("| if-not-null guards | ${sums.sumOf { it.ifNotNullGuards }} |")
        appendLine("| C-style `for (.indices)` loops | ${sums.sumOf { it.cStyleForLoops }} |")
        appendLine("| `@Synchronized` annotations | ${sums.sumOf { it.synchronizedAnnotations }} |")
    }

    private fun StringBuilder.renderComparative(c: ComparativeReport) {
        appendLine("## Comparative vs reference")
        appendLine()
        appendLine("- Reference root: `${c.referenceRoot}`")
        appendLine("- Pair count: ${c.pairCount}")
        appendLine("- Unmatched (converted only): ${c.unmatchedConvertedCount}")
        appendLine("- Unmatched (reference only): ${c.unmatchedReferenceCount}")
        appendLine("- Mean similarity: ${num(c.meanSimilarity)}")
        appendLine("- Median similarity: ${num(c.medianSimilarity)}")
        appendLine("- P90 similarity: ${num(c.p90Similarity)}")
        if (c.topDivergent.isNotEmpty()) {
            appendLine()
            appendLine("### Top 5 most-divergent files")
            appendLine()
            appendLine("| file | similarity | converted tokens | reference tokens |")
            appendLine("|---|---:|---:|---:|")
            for (f in c.topDivergent) {
                appendLine("| `${f.path}` | ${num(f.similarity)} | ${f.convertedTokens} | ${f.referenceTokens} |")
            }
        }
    }

    private fun StringBuilder.renderCompile(c: CompileReport) {
        appendLine("## Compile check")
        appendLine()
        val total = c.compiledCount + c.failedCount
        appendLine("- Pass rate: ${num(c.passRate)} (${c.compiledCount}/${total})")
        appendLine("- Failed: ${c.failedCount}")
        val failed = c.files.filter { !it.compiled }
        if (failed.isNotEmpty()) {
            appendLine()
            appendLine("### Failed files (first ${minOf(failed.size, MAX_FAILED_SHOWN)})")
            appendLine()
            for (f in failed.take(MAX_FAILED_SHOWN)) {
                appendLine("- `${f.path}`")
                for (err in f.errors) {
                    appendLine("  - $err")
                }
            }
            if (failed.size > MAX_FAILED_SHOWN) {
                appendLine()
                appendLine("_…and ${failed.size - MAX_FAILED_SHOWN} more. See `report.json`._")
            }
        }
    }

    private fun num(d: Double): String =
        if (!d.isFinite()) "0.0000" else String.format(Locale.ROOT, "%.4f", d)

    private companion object {
        const val MAX_FAILED_SHOWN = 25
    }
}
