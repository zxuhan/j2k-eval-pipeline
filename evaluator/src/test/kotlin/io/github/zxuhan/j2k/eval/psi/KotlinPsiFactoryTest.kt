package io.github.zxuhan.j2k.eval.psi

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.Test

class KotlinPsiFactoryTest {

    @Test
    fun `parses a minimal kotlin file and exposes top-level declarations`() {
        KotlinPsiFactory().use { psi ->
            val file = psi.parse("Hello.kt", "package demo\n\nclass Hello(val name: String)\n")

            file.packageFqName.asString() shouldBe "demo"
            file.declarations.size shouldBe 1

            val ktClass = file.declarations.single() as KtClass
            ktClass.name shouldBe "Hello"
            ktClass.primaryConstructor?.text.orEmpty() shouldContain "val name: String"
        }
    }

    @Test
    fun `parses empty file without throwing`() {
        KotlinPsiFactory().use { psi ->
            val file = psi.parse("Empty.kt", "")
            file.declarations.size shouldBe 0
        }
    }
}
