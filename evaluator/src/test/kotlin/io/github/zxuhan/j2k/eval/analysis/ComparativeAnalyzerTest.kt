package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComparativeAnalyzerTest {

    private lateinit var psi: KotlinPsiFactory
    private lateinit var analyzer: ComparativeAnalyzer

    @BeforeAll
    fun setUp() {
        psi = KotlinPsiFactory()
        analyzer = ComparativeAnalyzer(psi)
    }

    @AfterAll
    fun tearDown() {
        psi.close()
    }

    @Test
    fun `near-identical files score high similarity`() {
        val converted = newRoot("converted-near")
        val reference = newRoot("reference-near")

        val body = """
            |package demo
            |
            |class Request(val url: String, val method: String) {
            |    fun header(name: String): String? = null
            |    fun body(): String = ""
            |}
        """.trimMargin()
        writeFile(converted, "demo/Request.kt", body)
        writeFile(reference, "demo/Request.kt", body)

        val report = analyzer.analyze(converted, reference)
        report.pairCount shouldBe 1
        report.unmatchedConvertedCount shouldBe 0
        report.unmatchedReferenceCount shouldBe 0
        report.files[0].similarity shouldBeGreaterThan 0.99
    }

    @Test
    fun `diverged files score low similarity`() {
        val converted = newRoot("converted-diverged")
        val reference = newRoot("reference-diverged")

        writeFile(converted, "demo/Client.kt", """
            |package demo
            |
            |class Client {
            |    fun getName(): String? { return null }
            |    fun setName(v: String?) { }
            |    fun getAge(): Int { return 0 }
            |}
        """.trimMargin())
        writeFile(reference, "demo/Client.kt", """
            |package demo
            |
            |data class Client(val name: String, val age: Int)
        """.trimMargin())

        val report = analyzer.analyze(converted, reference)
        report.pairCount shouldBe 1
        report.files[0].similarity shouldBeLessThan 0.5
    }

    @Test
    fun `unmatched files are counted separately`() {
        val converted = newRoot("converted-unmatched")
        val reference = newRoot("reference-unmatched")

        writeFile(converted, "a/A.kt", "package a\nclass A")
        writeFile(converted, "b/B.kt", "package b\nclass B")
        writeFile(reference, "a/A.kt", "package a\nclass A")
        writeFile(reference, "c/C.kt", "package c\nclass C")

        val report = analyzer.analyze(converted, reference)
        report.pairCount shouldBe 1
        report.unmatchedConvertedCount shouldBe 1 // b/B.kt
        report.unmatchedReferenceCount shouldBe 1 // c/C.kt
    }

    @Test
    fun `empty roots yield zero pairs`() {
        val converted = newRoot("empty-converted")
        val reference = newRoot("empty-reference")

        val report = analyzer.analyze(converted, reference)
        report.pairCount shouldBe 0
        report.meanSimilarity shouldBe 0.0
    }

    private fun newRoot(prefix: String): Path =
        Files.createTempDirectory("j2k-compare-$prefix").also {
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { it.toFile().deleteRecursively() } })
        }

    private fun writeFile(root: Path, relPath: String, content: String) {
        val target = root.resolve(relPath)
        target.parent.createDirectories()
        target.writeText(content)
    }
}
