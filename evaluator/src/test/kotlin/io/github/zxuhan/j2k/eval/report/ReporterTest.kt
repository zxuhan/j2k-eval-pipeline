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
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReporterTest {

    @Test
    fun `json encoder produces stable output for full report`() {
        val actual = JsonReporter().encode(sampleReport())
        actual shouldBe readGolden("fixtures/reporter/report.json")
    }

    @Test
    fun `markdown renderer produces stable output for full report`() {
        val actual = MarkdownReporter().render(sampleReport())
        actual shouldBe readGolden("fixtures/reporter/report.md")
    }

    @Test
    fun `json encoder emits null for missing optional sections`() {
        val aggregate = AggregateAnalysis(files = emptyList())
        val actual = JsonReporter().encode(Report(aggregate = aggregate))
        actual.contains("\"comparative\": null") shouldBe true
        actual.contains("\"compile\": null") shouldBe true
    }

    private fun sampleReport(): Report {
        val structural = StructuralMetrics(
            classes = 2, objects = 1, interfaces = 0, functions = 4, properties = 3,
            maxNestingDepth = 2, meanNestingDepth = 1.5, codeLines = 40, commentLines = 6,
        )
        val idiom = IdiomMetrics(
            dataClasses = 1, objectSingletons = 1, companionObjects = 0, lambdaArguments = 2,
            samObjectExpressions = 0, trailingLambdas = 2, whenExpressions = 1, ifElseChains = 0,
            stringTemplates = 3, scopeFunctionCalls = 1, propertyAccessors = 0,
        )
        val javaIsm = JavaIsmMetrics(
            notNullAssertions = 3, notNullAssertionsPerKloc = 75.0, explicitGetterSetterPairs = 0,
            javaUtilCollectionRefs = 1, ifNotNullGuards = 2, cStyleForLoops = 0,
            synchronizedAnnotations = 1, platformTypeReferences = 0,
        )
        val file = FileAnalysis(
            path = "build/converted/demo/Hello.kt",
            structural = structural,
            idiom = idiom,
            javaIsm = javaIsm,
        )
        val aggregate = AggregateAnalysis(files = listOf(file))
        val comparative = ComparativeReport(
            referenceRoot = "build/reference",
            pairCount = 1,
            unmatchedConvertedCount = 0,
            unmatchedReferenceCount = 2,
            meanSimilarity = 0.7500,
            medianSimilarity = 0.7500,
            p90Similarity = 0.7500,
            files = listOf(
                CompareFileResult(
                    path = "build/converted/demo/Hello.kt",
                    referencePath = "build/reference/demo/Hello.kt",
                    similarity = 0.7500,
                    convertedTokens = 20,
                    referenceTokens = 25,
                )
            ),
            topDivergent = listOf(
                CompareFileResult(
                    path = "build/converted/demo/Hello.kt",
                    referencePath = "build/reference/demo/Hello.kt",
                    similarity = 0.7500,
                    convertedTokens = 20,
                    referenceTokens = 25,
                )
            ),
        )
        val compile = CompileReport(
            compiledCount = 1,
            failedCount = 0,
            passRate = 1.0,
            files = listOf(
                CompileFileResult(
                    path = "build/converted/demo/Hello.kt",
                    compiled = true,
                    errors = emptyList(),
                )
            ),
        )
        return Report(aggregate = aggregate, comparative = comparative, compile = compile)
    }

    private fun readGolden(path: String): String {
        val resource = javaClass.classLoader.getResource(path)
            ?: error("Golden fixture not found: $path")
        // Normalize to LF so tests pass on CRLF checkouts.
        return resource.readText().replace("\r\n", "\n")
    }
}
