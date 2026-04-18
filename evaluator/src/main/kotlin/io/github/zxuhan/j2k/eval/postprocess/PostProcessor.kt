package io.github.zxuhan.j2k.eval.postprocess

import io.github.zxuhan.j2k.eval.psi.KotlinPsiFactory
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createParentDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText

data class PostProcessFileResult(
    val path: String,
    val changed: Boolean,
    val addedImports: List<String>,
)

data class PostProcessReport(
    val files: List<PostProcessFileResult>,
    val filesChanged: Int,
    val importsAddedTotal: Int,
)

/**
 * Walks an input tree of converted Kotlin files, applies each registered fixer,
 * and writes the (possibly rewritten) files into an output tree preserving the
 * relative path. Files with no applicable fixes are copied through unchanged.
 */
class PostProcessor(
    private val psi: KotlinPsiFactory,
    private val annotationImports: AnnotationImportFixer = AnnotationImportFixer(),
) {

    @OptIn(ExperimentalPathApi::class)
    fun run(input: Path, output: Path): PostProcessReport {
        require(input.isDirectory()) { "input must be a directory: $input" }

        val kotlinFiles = input.walk().filter { it.extension == "kt" }.sorted().toList()
        val results = kotlinFiles.map { src ->
            val rel = src.relativeTo(input)
            val dst = output.resolve(rel.toString())
            dst.createParentDirectories()

            val original = src.readText()
            val ktFile = psi.parse(src.fileName.toString(), original)
            val importResult = annotationImports.fix(ktFile, original)

            dst.writeText(importResult.newSource)

            PostProcessFileResult(
                path = rel.toString(),
                changed = importResult.changed,
                addedImports = importResult.addedImports,
            )
        }

        return PostProcessReport(
            files = results,
            filesChanged = results.count { it.changed },
            importsAddedTotal = results.sumOf { it.addedImports.size },
        )
    }
}
