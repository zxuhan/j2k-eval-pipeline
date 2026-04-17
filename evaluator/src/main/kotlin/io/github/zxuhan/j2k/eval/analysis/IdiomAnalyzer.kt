package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.model.IdiomMetrics
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression

class IdiomAnalyzer {

    private val scopeFnNames = setOf("let", "run", "apply", "also", "with")

    fun analyze(file: KtFile): IdiomMetrics {
        var dataClasses = 0
        var objectSingletons = 0
        var companions = 0
        var lambdaArgs = 0
        var samObjectExpr = 0
        var trailingLambdas = 0
        var whenExprs = 0
        var ifElseChains = 0
        var stringTemplates = 0
        var scopeCalls = 0
        var propertyAccessors = 0

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                if (klass.isData()) dataClasses++
                super.visitClass(klass)
            }

            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                when {
                    declaration.isObjectLiteral() -> { /* handled by visitObjectLiteralExpression */ }
                    declaration.isCompanion() -> companions++
                    else -> objectSingletons++
                }
                super.visitObjectDeclaration(declaration)
            }

            override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
                samObjectExpr++
                super.visitObjectLiteralExpression(expression)
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                val callee = expression.calleeExpression?.text
                val hasTrailing = expression.lambdaArguments.isNotEmpty()
                val hasLambda = hasTrailing ||
                    expression.valueArguments.any { it.getArgumentExpression()?.text?.startsWith("{") == true }
                if (hasTrailing) trailingLambdas++
                if (hasLambda) lambdaArgs++
                if (callee in scopeFnNames && hasLambda) scopeCalls++
                super.visitCallExpression(expression)
            }

            override fun visitWhenExpression(expression: KtWhenExpression) {
                whenExprs++
                super.visitWhenExpression(expression)
            }

            override fun visitIfExpression(expression: KtIfExpression) {
                if (expression.`else` is KtIfExpression && !expression.isElseBranch()) {
                    ifElseChains++
                }
                super.visitIfExpression(expression)
            }

            override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
                if (expression.entries.any { it.expression != null }) stringTemplates++
                super.visitStringTemplateExpression(expression)
            }

            override fun visitProperty(property: KtProperty) {
                if (property.getter != null || property.setter != null) propertyAccessors++
                super.visitProperty(property)
            }
        })

        return IdiomMetrics(
            dataClasses = dataClasses,
            objectSingletons = objectSingletons,
            companionObjects = companions,
            lambdaArguments = lambdaArgs,
            samObjectExpressions = samObjectExpr,
            trailingLambdas = trailingLambdas,
            whenExpressions = whenExprs,
            ifElseChains = ifElseChains,
            stringTemplates = stringTemplates,
            scopeFunctionCalls = scopeCalls,
            propertyAccessors = propertyAccessors,
        )
    }

    private fun KtIfExpression.isElseBranch(): Boolean {
        val p = parent ?: return false
        return p is KtIfExpression && p.`else` === this
    }
}
