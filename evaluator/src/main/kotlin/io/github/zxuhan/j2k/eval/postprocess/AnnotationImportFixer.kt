package io.github.zxuhan.j2k.eval.postprocess

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Injects missing JSR-305 annotation imports into a converted Kotlin file.
 *
 * Detects annotation short names referenced but not imported, and rewrites the
 * source text by splicing `import javax.annotation.<Name>` after the last
 * existing import (or after `package` if no imports exist). Source-text splice
 * rather than PSI tree mutation — avoids round-trip hazards with the
 * compiler-embedded PSI and keeps whitespace stable.
 *
 * A file is only modified if at least one annotation needs adding. Files with
 * no referenced JSR-305 annotations, or where all referenced ones are already
 * imported (possibly from a different package — we won't override), pass
 * through unchanged.
 */
class AnnotationImportFixer(
    private val mapping: Map<String, String> = AnnotationImports.jsr305Defaults,
) {

    data class Result(
        val changed: Boolean,
        val addedImports: List<String>,
        val newSource: String,
    )

    fun fix(file: KtFile, source: String): Result {
        val referencedNames = file.collectDescendantsOfType<org.jetbrains.kotlin.psi.KtAnnotationEntry>()
            .mapNotNull { it.shortName?.asString() }
            .toSet()

        val importedNames = file.importDirectives
            .mapNotNull { it.importPath?.importedName?.asString() ?: it.shortShortName() }
            .toSet()

        val toAdd = mapping
            .filterKeys { name -> name in referencedNames && name !in importedNames }
            .values
            .sorted()
            .distinct()

        if (toAdd.isEmpty()) {
            return Result(changed = false, addedImports = emptyList(), newSource = source)
        }

        val newImportLines = toAdd.joinToString("\n") { "import $it" }
        val newSource = spliceImports(source, file, newImportLines)
        return Result(changed = true, addedImports = toAdd, newSource = newSource)
    }

    private fun KtImportDirective.shortShortName(): String? =
        importedReference?.text?.substringAfterLast('.')

    private fun spliceImports(source: String, file: KtFile, newImportBlock: String): String {
        val lastImport = file.importDirectives.maxByOrNull { it.textRange.endOffset }
        if (lastImport != null) {
            val end = lastImport.textRange.endOffset
            return source.substring(0, end) + "\n" + newImportBlock + source.substring(end)
        }

        val pkg = file.packageDirective?.takeIf { it.textLength > 0 }
        if (pkg != null) {
            val end = pkg.textRange.endOffset
            return source.substring(0, end) + "\n\n" + newImportBlock + source.substring(end)
        }

        return newImportBlock + "\n\n" + source
    }
}
