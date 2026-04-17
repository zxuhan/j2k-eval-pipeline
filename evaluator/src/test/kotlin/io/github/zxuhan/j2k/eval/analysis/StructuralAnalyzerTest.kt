package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import io.github.zxuhan.j2k.eval.psi.parseResource
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StructuralAnalyzerTest {

    private lateinit var psi: KotlinPsiFactory
    private val analyzer = StructuralAnalyzer()

    @BeforeAll
    fun setUp() {
        psi = KotlinPsiFactory()
    }

    @AfterAll
    fun tearDown() {
        psi.close()
    }

    @Test
    fun `counts top-level and nested declarations`() {
        val file = psi.parseResource("fixtures/structural/Declarations.kt")
        val metrics = analyzer.analyze(file)

        metrics.classes shouldBe 2 // TopClass + Inner
        metrics.objects shouldBe 1 // Singleton
        metrics.interfaces shouldBe 1 // Greeter
        metrics.functions shouldBeGreaterThanOrEqual 4 // greet, inside, ping, hello
        metrics.properties shouldBeGreaterThanOrEqual 3 // name, age, x
    }

    @Test
    fun `max nesting depth reflects nested blocks`() {
        val file = psi.parseResource("fixtures/structural/Declarations.kt")
        val metrics = analyzer.analyze(file)

        metrics.maxNestingDepth shouldBeGreaterThanOrEqual 3
        metrics.meanNestingDepth shouldBeGreaterThan 0.0
    }

    @Test
    fun `counts comment vs code lines`() {
        val file = psi.parse("X.kt", """
            |// a comment
            |class X {
            |    // another comment
            |    fun f() = 1
            |}
        """.trimMargin())
        val metrics = analyzer.analyze(file)

        metrics.commentLines shouldBe 2
        metrics.codeLines shouldBe 3
    }

    @Test
    fun `empty file yields zero metrics`() {
        val file = psi.parse("Empty.kt", "")
        val metrics = analyzer.analyze(file)

        metrics.classes shouldBe 0
        metrics.functions shouldBe 0
        metrics.codeLines shouldBe 0
    }
}
