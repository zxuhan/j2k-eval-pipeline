package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.model.StructuralMetrics
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class StructuralAnalyzer {

    fun analyze(file: KtFile): StructuralMetrics {
        var classes = 0
        var objects = 0
        var interfaces = 0
        var functions = 0
        var properties = 0

        val bodyDepths = mutableListOf<Int>()

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                when {
                    klass.isInterface() -> interfaces++
                    else -> classes++
                }
                super.visitClass(klass)
            }

            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                objects++
                super.visitObjectDeclaration(declaration)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                functions++
                super.visitNamedFunction(function)
            }

            override fun visitProperty(property: KtProperty) {
                properties++
                super.visitProperty(property)
            }

            override fun visitClassBody(classBody: KtClassBody) {
                bodyDepths += classBody.depthFromFile()
                super.visitClassBody(classBody)
            }

            override fun visitBlockExpression(expression: KtBlockExpression) {
                bodyDepths += expression.depthFromFile()
                super.visitBlockExpression(expression)
            }
        })

        val maxNesting = bodyDepths.maxOrNull() ?: 0
        val meanNesting = if (bodyDepths.isEmpty()) 0.0 else bodyDepths.average()

        val (codeLines, commentLines) = countLines(file)

        return StructuralMetrics(
            classes = classes,
            objects = objects,
            interfaces = interfaces,
            functions = functions,
            properties = properties,
            maxNestingDepth = maxNesting,
            meanNestingDepth = meanNesting,
            codeLines = codeLines,
            commentLines = commentLines,
        )
    }

    private fun PsiElement.depthFromFile(): Int {
        var depth = 0
        var current: PsiElement? = parent
        while (current != null && current !is KtFile) {
            if (current is KtClassBody || current is KtBlockExpression) depth++
            current = current.parent
        }
        return depth
    }

    private fun countLines(file: KtFile): Pair<Int, Int> {
        val text = file.text
        if (text.isEmpty()) return 0 to 0

        val commentLineSet = mutableSetOf<Int>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitComment(comment: PsiComment) {
                val range = comment.textRange
                val start = lineIndex(text, range.startOffset)
                val end = lineIndex(text, range.endOffset - 1).coerceAtLeast(start)
                for (line in start..end) commentLineSet += line
            }
        })

        var codeLines = 0
        var commentLines = 0
        text.split('\n').forEachIndexed { index, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            if (index in commentLineSet) commentLines++ else codeLines++
        }
        return codeLines to commentLines
    }

    private fun lineIndex(text: String, offset: Int): Int {
        if (offset <= 0) return 0
        var line = 0
        for (i in 0 until offset.coerceAtMost(text.length)) {
            if (text[i] == '\n') line++
        }
        return line
    }
}
