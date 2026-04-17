package io.github.zxuhan.j2k.eval.analysis

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class CompileAnalyzerTest {

    @Test
    fun `valid and invalid files are segregated by compile verdict`() {
        val root = Files.createTempDirectory("j2k-compile-test").also {
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { it.toFile().deleteRecursively() } })
        }
        writeFile(root, "demo/Ok.kt", """
            |package demo
            |
            |class Ok(val name: String) {
            |    fun greet(): String = "hi " + name
            |}
        """.trimMargin())
        writeFile(root, "demo/Broken.kt", """
            |package demo
            |
            |class Broken {
            |    fun boom(): String = 42
            |}
        """.trimMargin())

        val report = CompileAnalyzer().analyze(root, classpathFile = null)
        report.files.size shouldBe 2

        val ok = report.files.single { it.path.endsWith("Ok.kt") }
        val broken = report.files.single { it.path.endsWith("Broken.kt") }

        ok.compiled.shouldBeTrue()
        broken.compiled.shouldBeFalse()
        broken.errors.isNotEmpty().shouldBeTrue()

        report.compiledCount shouldBe 1
        report.failedCount shouldBe 1
    }

    @Test
    fun `empty corpus yields zero counts`() {
        val root = Files.createTempDirectory("j2k-compile-empty").also {
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { it.toFile().deleteRecursively() } })
        }
        val report = CompileAnalyzer().analyze(root, classpathFile = null)
        report.compiledCount shouldBe 0
        report.failedCount shouldBe 0
        report.files.isEmpty().shouldBeTrue()
    }

    private fun writeFile(root: Path, relPath: String, content: String) {
        val target = root.resolve(relPath)
        target.parent.createDirectories()
        target.writeText(content)
    }
}
