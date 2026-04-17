package io.github.zxuhan.j2k.eval.analysis

import io.github.zxuhan.j2k.eval.model.CompareFileResult
import io.github.zxuhan.j2k.eval.model.ComparativeReport
import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.walk

/**
 * Declaration-level similarity between converted Kotlin and a hand-migrated reference
 * (e.g. OkHttp 3.14 converted-by-j2k vs OkHttp 4.x in the maintainers' repo).
 *
 * Strategy — per D4 in CLAUDE.md: for each matched file pair, flatten the declarations
 * (classes/objects/interfaces/functions/properties) to a token sequence of shape-bearing
 * pieces (modifiers, kind, name, param types, return type), then compute normalized
 * Levenshtein. Bodies are intentionally ignored; j2k almost always gets bodies "right
 * enough" and the divergence from idiomatic Kotlin is at the declaration shape.
 */
class ComparativeAnalyzer(private val psi: KotlinPsiFactory) {

    fun analyze(convertedRoot: Path, referenceRoot: Path): ComparativeReport {
        val convertedFiles = collectKt(convertedRoot)
        val referenceFiles = collectKt(referenceRoot)

        val convertedByKey = convertedFiles.associateBy { keyOf(convertedRoot, it) }
        val referenceByKey = referenceFiles.associateBy { keyOf(referenceRoot, it) }

        val matchedKeys = convertedByKey.keys intersect referenceByKey.keys
        val unmatchedConverted = convertedByKey.keys - matchedKeys
        val unmatchedReference = referenceByKey.keys - matchedKeys

        val dict = mutableMapOf<String, Int>()
        val results = matchedKeys.sorted().map { k ->
            val converted = convertedByKey.getValue(k)
            val reference = referenceByKey.getValue(k)
            computePair(converted, reference, dict)
        }

        val sims = results.map { it.similarity }.sorted()
        val mean = if (sims.isEmpty()) 0.0 else sims.sum() / sims.size
        val median = if (sims.isEmpty()) 0.0 else sims[sims.size / 2]
        val p90 = if (sims.isEmpty()) 0.0 else sims[minOf(sims.lastIndex, (sims.size * 0.9).toInt())]
        val topDivergent = results.sortedBy { it.similarity }.take(5)

        return ComparativeReport(
            referenceRoot = referenceRoot.toString(),
            pairCount = results.size,
            unmatchedConvertedCount = unmatchedConverted.size,
            unmatchedReferenceCount = unmatchedReference.size,
            meanSimilarity = mean,
            medianSimilarity = median,
            p90Similarity = p90,
            files = results,
            topDivergent = topDivergent,
        )
    }

    private fun computePair(converted: Path, reference: Path, dict: MutableMap<String, Int>): CompareFileResult {
        val cTokens = tokensFor(converted, dict)
        val rTokens = tokensFor(reference, dict)
        val dist = levenshtein(cTokens, rTokens)
        val denom = maxOf(cTokens.size, rTokens.size).coerceAtLeast(1)
        return CompareFileResult(
            path = converted.toString(),
            referencePath = reference.toString(),
            similarity = 1.0 - dist.toDouble() / denom,
            convertedTokens = cTokens.size,
            referenceTokens = rTokens.size,
        )
    }

    private fun tokensFor(path: Path, dict: MutableMap<String, Int>): IntArray {
        val ktFile = psi.parse(path)
        val tokens = mutableListOf<String>()
        emit(ktFile, tokens)
        return IntArray(tokens.size) { i -> dict.getOrPut(tokens[i]) { dict.size } }
    }

    private fun emit(file: KtFile, tokens: MutableList<String>) {
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                emitClassHeader(classOrObject, tokens)
                super.visitClassOrObject(classOrObject)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                emitFunction(function, tokens)
                super.visitNamedFunction(function)
            }

            override fun visitProperty(property: KtProperty) {
                emitProperty(property, tokens)
                super.visitProperty(property)
            }
        })
    }

    private fun emitClassHeader(c: KtClassOrObject, tokens: MutableList<String>) {
        val kind = when {
            c is KtClass && c.isInterface() -> "kind=interface"
            c is KtClass -> "kind=class"
            c is KtObjectDeclaration && c.isCompanion() -> "kind=companion"
            c is KtObjectDeclaration -> "kind=object"
            else -> "kind=unknown"
        }
        tokens += "decl:classOrObject"
        tokens += kind
        for (m in modifierTokens(c)) tokens += "mod=$m"
        tokens += "name=${c.name ?: "<anon>"}"
        val typeParams = (c as? KtClass)?.typeParameters.orEmpty()
        if (typeParams.isNotEmpty()) tokens += "typeParams=${typeParams.size}"
        c.primaryConstructorParameters.forEach { p ->
            tokens += "ctorParam=${typeText(p)}"
        }
        c.superTypeListEntries.forEach { entry ->
            tokens += "super=${entry.typeReference?.text ?: "?"}"
        }
    }

    private fun emitFunction(f: KtNamedFunction, tokens: MutableList<String>) {
        tokens += "decl:fun"
        for (m in modifierTokens(f)) tokens += "mod=$m"
        tokens += "name=${f.name ?: "<anon>"}"
        val typeParams = f.typeParameters
        if (typeParams.isNotEmpty()) tokens += "typeParams=${typeParams.size}"
        f.valueParameters.forEach { p -> tokens += "param=${typeText(p)}" }
        tokens += "returns=${f.typeReference?.text ?: "Unit"}"
    }

    private fun emitProperty(p: KtProperty, tokens: MutableList<String>) {
        tokens += if (p.isVar) "decl:var" else "decl:val"
        for (m in modifierTokens(p)) tokens += "mod=$m"
        tokens += "name=${p.name ?: "<anon>"}"
        tokens += "type=${p.typeReference?.text ?: "<inferred>"}"
    }

    private fun typeText(p: KtParameter): String = p.typeReference?.text ?: "<inferred>"

    private fun modifierTokens(decl: Any): List<String> {
        val modList = when (decl) {
            is KtClassOrObject -> decl.modifierList
            is KtNamedFunction -> decl.modifierList
            is KtProperty -> decl.modifierList
            else -> null
        } ?: return emptyList()
        return TRACKED_MODIFIERS.mapNotNull { tok ->
            if (modList.hasModifier(tok)) tok.value else null
        }
    }

    private fun levenshtein(a: IntArray, b: IntArray): Int {
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size
        var prev = IntArray(b.size + 1) { it }
        var curr = IntArray(b.size + 1)
        for (i in 1..a.size) {
            curr[0] = i
            val ai = a[i - 1]
            for (j in 1..b.size) {
                val cost = if (ai == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost,
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[b.size]
    }

    @OptIn(ExperimentalPathApi::class)
    private fun collectKt(root: Path): List<Path> =
        if (!root.isDirectory()) emptyList()
        else root.walk().filter { it.extension == "kt" }.sorted().toList()

    private fun keyOf(root: Path, file: Path): String =
        root.relativize(file).toString().replace('\\', '/')

    companion object {
        private val TRACKED_MODIFIERS: List<KtModifierKeywordToken> = listOf(
            KtTokens.DATA_KEYWORD,
            KtTokens.SEALED_KEYWORD,
            KtTokens.OPEN_KEYWORD,
            KtTokens.ABSTRACT_KEYWORD,
            KtTokens.FINAL_KEYWORD,
            KtTokens.INNER_KEYWORD,
            KtTokens.INLINE_KEYWORD,
            KtTokens.SUSPEND_KEYWORD,
            KtTokens.OVERRIDE_KEYWORD,
            KtTokens.LATEINIT_KEYWORD,
            KtTokens.PRIVATE_KEYWORD,
            KtTokens.INTERNAL_KEYWORD,
            KtTokens.PROTECTED_KEYWORD,
            KtTokens.PUBLIC_KEYWORD,
            KtTokens.ENUM_KEYWORD,
            KtTokens.ANNOTATION_KEYWORD,
            KtTokens.COMPANION_KEYWORD,
            KtTokens.CONST_KEYWORD,
            KtTokens.OPERATOR_KEYWORD,
        )
    }
}
