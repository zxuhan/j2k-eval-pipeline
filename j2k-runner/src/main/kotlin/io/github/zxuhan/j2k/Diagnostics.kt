package io.github.zxuhan.j2k

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

internal class Diagnostics(
    var converterKind: String,
    val postProcessed: Boolean,
) {
    private val files = mutableListOf<FileEntry>()
    private var errors = 0
    private var warnings = 0
    private var totalDurationMs = 0L

    fun record(
        sourcePath: String,
        durationMs: Long,
        warningsCount: Int,
        errorsCount: Int,
        messages: List<String> = emptyList(),
    ) {
        files += FileEntry(sourcePath, durationMs, warningsCount, errorsCount, messages)
        warnings += warningsCount
        errors += errorsCount
        totalDurationMs += durationMs
    }

    fun totalErrors(): Int = errors

    fun writeJson(target: Path) {
        target.createParentDirectories()
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"converterKind\": ").append(quote(converterKind)).append(",\n")
        sb.append("  \"postProcessed\": ").append(postProcessed).append(",\n")
        sb.append("  \"totals\": {\n")
        sb.append("    \"files\": ").append(files.size).append(",\n")
        sb.append("    \"warnings\": ").append(warnings).append(",\n")
        sb.append("    \"errors\": ").append(errors).append(",\n")
        sb.append("    \"durationMs\": ").append(totalDurationMs).append("\n")
        sb.append("  },\n")
        sb.append("  \"files\": [")
        files.forEachIndexed { idx, f ->
            sb.append(if (idx == 0) "\n" else ",\n")
            sb.append("    {\n")
            sb.append("      \"path\": ").append(quote(f.path)).append(",\n")
            sb.append("      \"durationMs\": ").append(f.durationMs).append(",\n")
            sb.append("      \"warningsCount\": ").append(f.warningsCount).append(",\n")
            sb.append("      \"errorsCount\": ").append(f.errorsCount).append(",\n")
            sb.append("      \"messages\": [")
            f.messages.forEachIndexed { i, m ->
                sb.append(if (i == 0) "" else ", ")
                sb.append(quote(m))
            }
            sb.append("]\n")
            sb.append("    }")
        }
        if (files.isNotEmpty()) sb.append("\n  ")
        sb.append("]\n}\n")
        Files.writeString(target, sb.toString())
    }

    private data class FileEntry(
        val path: String,
        val durationMs: Long,
        val warningsCount: Int,
        val errorsCount: Int,
        val messages: List<String>,
    )

    private fun quote(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
