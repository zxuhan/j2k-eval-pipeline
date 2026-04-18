package io.github.zxuhan.j2k.eval.postprocess

import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotationImportFixerTest {

    private lateinit var psi: KotlinPsiFactory
    private val fixer = AnnotationImportFixer()

    @BeforeAll
    fun setUp() { psi = KotlinPsiFactory() }

    @AfterAll
    fun tearDown() { psi.close() }

    @Test
    fun `injects missing javax annotation Nullable import`() {
        val source = """
            package okhttp3

            import okhttp3.Route

            interface Authenticator {
                @Nullable
                fun authenticate(@Nullable route: Route?): String?
            }
        """.trimIndent()

        val result = fixer.fix(psi.parse("Authenticator.kt", source), source)

        result.changed.shouldBeTrue()
        result.addedImports shouldBe listOf("javax.annotation.Nullable")
        result.newSource shouldContain "import javax.annotation.Nullable"
        result.newSource shouldContain "import okhttp3.Route"
    }

    @Test
    fun `does not inject when annotation already imported`() {
        val source = """
            package okhttp3

            import javax.annotation.Nullable

            interface Foo {
                @Nullable
                fun thing(): String?
            }
        """.trimIndent()

        val result = fixer.fix(psi.parse("Foo.kt", source), source)

        result.changed.shouldBeFalse()
        result.addedImports.shouldBeEmpty()
    }

    @Test
    fun `does not inject when short name already imported from different package`() {
        val source = """
            package okhttp3

            import org.jetbrains.annotations.Nullable

            interface Foo {
                @Nullable
                fun thing(): String?
            }
        """.trimIndent()

        val result = fixer.fix(psi.parse("Foo.kt", source), source)

        // Already imported as short name `Nullable` — do not override even though
        // the package differs. Trust the (possibly intentional) user-chosen source.
        result.changed.shouldBeFalse()
    }

    @Test
    fun `injects multiple missing imports alphabetically`() {
        val source = """
            package okhttp3

            @CheckReturnValue
            @ParametersAreNonnullByDefault
            class Foo {
                @Nullable
                fun a(): String? = null
            }
        """.trimIndent()

        val result = fixer.fix(psi.parse("Foo.kt", source), source)

        result.changed.shouldBeTrue()
        result.addedImports shouldBe listOf(
            "javax.annotation.CheckReturnValue",
            "javax.annotation.Nullable",
            "javax.annotation.ParametersAreNonnullByDefault",
        )
    }

    @Test
    fun `injects after package when no existing imports`() {
        val source = """
            package okhttp3

            class Foo {
                @Nullable
                fun a(): String? = null
            }
        """.trimIndent()

        val result = fixer.fix(psi.parse("Foo.kt", source), source)

        result.changed.shouldBeTrue()
        val lines = result.newSource.lines()
        val pkgIdx = lines.indexOfFirst { it.startsWith("package ") }
        val importIdx = lines.indexOfFirst { it.startsWith("import javax.annotation.Nullable") }
        (importIdx > pkgIdx).shouldBeTrue()
    }

    @Test
    fun `no-op when file uses no known annotations`() {
        val source = """
            package okhttp3

            class Foo {
                fun a(): String = "x"
            }
        """.trimIndent()

        val result = fixer.fix(psi.parse("Foo.kt", source), source)

        result.changed.shouldBeFalse()
        result.newSource shouldBe source
    }
}
