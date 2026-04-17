package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.model.JavaIsmMetrics
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

class JavaIsmAnalyzer {

    private val javaUtilCollections = setOf(
        "ArrayList", "LinkedList", "HashMap", "HashSet", "LinkedHashMap", "LinkedHashSet", "TreeMap", "TreeSet", "Vector", "Stack",
    )

    fun analyze(file: KtFile): JavaIsmMetrics {
        var notNullAssertions = 0
        var getterSetterPairs = 0
        var javaUtilRefs = 0
        var ifNotNullGuards = 0
        var cStyleForLoops = 0
        var synchronizedAnnotations = 0
        var platformTypes = 0

        val functionsByClass = mutableMapOf<KtClass?, MutableMap<String, Boolean>>() // key is function base name; value: hasGetter, merged via seen getter/setter

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitPostfixExpression(expression: KtPostfixExpression) {
                if (expression.operationToken == KtTokens.EXCLEXCL) notNullAssertions++
                super.visitPostfixExpression(expression)
            }

            override fun visitTypeReference(typeReference: KtTypeReference) {
                val user = typeReference.typeElement as? KtUserType
                val qualified = user?.text.orEmpty()
                if (qualified.startsWith("java.util.")) {
                    javaUtilRefs++
                } else if (user?.referencedName in javaUtilCollections && user?.qualifier == null) {
                    javaUtilRefs++
                }
                super.visitTypeReference(typeReference)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                val cls = function.containingClassOrObjectKt()
                val name = function.name.orEmpty()
                val accessorName = accessorBaseName(name)
                if (accessorName != null) {
                    val bucket = functionsByClass.getOrPut(cls) { mutableMapOf() }
                    val alreadySeen = bucket[accessorName]
                    val isGetter = name.startsWith("get") || name.startsWith("is")
                    if (alreadySeen != null && alreadySeen != isGetter) {
                        getterSetterPairs++
                    }
                    bucket[accessorName] = isGetter
                }

                if (function.annotationEntries.any { it.shortName?.asString() == "Synchronized" }) {
                    synchronizedAnnotations++
                }

                super.visitNamedFunction(function)
            }

            override fun visitIfExpression(expression: KtIfExpression) {
                if (expression.isNotNullGuard()) ifNotNullGuards++
                super.visitIfExpression(expression)
            }

            override fun visitForExpression(expression: KtForExpression) {
                val rangeText = expression.loopRange?.text.orEmpty()
                if (rangeText.endsWith(".indices") || rangeText.endsWith("indices")) cStyleForLoops++
                super.visitForExpression(expression)
            }
        })

        return JavaIsmMetrics(
            notNullAssertions = notNullAssertions,
            notNullAssertionsPerKloc = perKloc(notNullAssertions, file.text),
            explicitGetterSetterPairs = getterSetterPairs,
            javaUtilCollectionRefs = javaUtilRefs,
            ifNotNullGuards = ifNotNullGuards,
            cStyleForLoops = cStyleForLoops,
            synchronizedAnnotations = synchronizedAnnotations,
            platformTypeReferences = platformTypes,
        )
    }

    private fun KtNamedFunction.containingClassOrObjectKt(): KtClass? {
        var p = parent
        while (p != null) {
            if (p is KtClass) return p
            p = p.parent
        }
        return null
    }

    private fun accessorBaseName(name: String): String? = when {
        name.length > 3 && name.startsWith("get") && name[3].isUpperCase() -> name.substring(3)
        name.length > 3 && name.startsWith("set") && name[3].isUpperCase() -> name.substring(3)
        name.length > 2 && name.startsWith("is") && name[2].isUpperCase() -> name.substring(2)
        else -> null
    }

    private fun KtIfExpression.isNotNullGuard(): Boolean {
        val cond = condition as? KtBinaryExpression ?: return false
        val op = cond.operationToken
        if (op != KtTokens.EXCLEQ) return false
        val left = cond.left
        val right = cond.right
        val leftIsNull = left is KtConstantExpression && left.text == "null"
        val rightIsNull = right is KtConstantExpression && right.text == "null"
        val otherIsRef = (left is KtReferenceExpression && !leftIsNull) ||
            (right is KtReferenceExpression && !rightIsNull)
        return (leftIsNull xor rightIsNull) && otherIsRef
    }

    private fun perKloc(count: Int, text: String): Double {
        val lines = text.count { it == '\n' } + 1
        if (lines == 0) return 0.0
        return count.toDouble() * 1000.0 / lines
    }
}
