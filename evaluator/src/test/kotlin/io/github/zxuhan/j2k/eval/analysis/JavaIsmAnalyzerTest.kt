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
class JavaIsmAnalyzerTest {

    private lateinit var psi: KotlinPsiFactory
    private val analyzer = JavaIsmAnalyzer()

    @BeforeAll fun setUp() { psi = KotlinPsiFactory() }
    @AfterAll fun tearDown() { psi.close() }

    @Test
    fun `counts not-null assertions and per-kloc density`() {
        val file = psi.parseResource("fixtures/javaism/JavaIsms.kt")
        val metrics = analyzer.analyze(file)

        metrics.notNullAssertions shouldBeGreaterThanOrEqual 2
        metrics.notNullAssertionsPerKloc shouldBeGreaterThan 0.0
    }

    @Test
    fun `detects getter-setter pairs`() {
        val file = psi.parseResource("fixtures/javaism/JavaIsms.kt")
        val metrics = analyzer.analyze(file)

        metrics.explicitGetterSetterPairs shouldBe 1 // getName/setName; getEnabled has no setter
    }

    @Test
    fun `counts java util collection refs`() {
        val file = psi.parseResource("fixtures/javaism/JavaIsms.kt")
        val metrics = analyzer.analyze(file)

        metrics.javaUtilCollectionRefs shouldBeGreaterThanOrEqual 2
    }

    @Test
    fun `counts if-not-null guards, c-style for, synchronized`() {
        val file = psi.parseResource("fixtures/javaism/JavaIsms.kt")
        val metrics = analyzer.analyze(file)

        metrics.ifNotNullGuards shouldBeGreaterThanOrEqual 1
        metrics.cStyleForLoops shouldBeGreaterThanOrEqual 1
        metrics.synchronizedAnnotations shouldBe 1
    }
}
