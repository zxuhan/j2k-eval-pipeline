package io.github.zxuhan.j2k.eval.psi

import org.jetbrains.kotlin.psi.KtFile

fun KotlinPsiFactory.parseResource(path: String): KtFile {
    val resource = javaClass.classLoader.getResource(path)
        ?: error("Test resource not found: $path")
    val name = path.substringAfterLast('/')
    return parse(name, resource.readText())
}
