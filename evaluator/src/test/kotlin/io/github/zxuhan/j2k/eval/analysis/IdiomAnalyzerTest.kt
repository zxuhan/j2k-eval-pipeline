package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import io.github.zxuhan.j2k.eval.psi.parseResource
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdiomAnalyzerTest {

    private lateinit var psi: KotlinPsiFactory
    private val analyzer = IdiomAnalyzer()

    @BeforeAll fun setUp() { psi = KotlinPsiFactory() }
    @AfterAll fun tearDown() { psi.close() }

    @Test
    fun `detects data classes, objects, and companion separately`() {
        val file = psi.parseResource("fixtures/idiom/Idioms.kt")
        val metrics = analyzer.analyze(file)

        metrics.dataClasses shouldBe 1
        metrics.objectSingletons shouldBe 1
        metrics.companionObjects shouldBe 1
    }

    @Test
    fun `counts trailing lambdas, scope fn calls, string templates`() {
        val file = psi.parseResource("fixtures/idiom/Idioms.kt")
        val metrics = analyzer.analyze(file)

        metrics.trailingLambdas shouldBeGreaterThanOrEqual 5
        metrics.scopeFunctionCalls shouldBeGreaterThanOrEqual 2 // let + apply
        metrics.stringTemplates shouldBeGreaterThanOrEqual 1
        metrics.whenExpressions shouldBe 1
    }

    @Test
    fun `counts SAM anonymous object expressions`() {
        val file = psi.parseResource("fixtures/idiom/Idioms.kt")
        val metrics = analyzer.analyze(file)

        metrics.samObjectExpressions shouldBe 1 // object : Runnable
    }

    @Test
    fun `counts if-else chains and property accessors`() {
        val file = psi.parseResource("fixtures/idiom/Idioms.kt")
        val metrics = analyzer.analyze(file)

        metrics.ifElseChains shouldBeGreaterThanOrEqual 1
        metrics.propertyAccessors shouldBeGreaterThanOrEqual 1 // val display with getter
    }
}
