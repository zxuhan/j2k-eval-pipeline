package io.github.zxuhan.j2k

import org.jetbrains.kotlin.j2k.J2kConverterExtension
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

internal data class Args(
    val inputDir: Path,
    val outputDir: Path,
    val diagnosticsFile: Path,
    val converterKind: J2kConverterExtension.Kind,
    val postProcess: Boolean,
) {
    companion object {
        private const val USAGE = """
            usage: j2k-batch --input <dir> --output <dir> --diagnostics <file.json>
                             [--converter k1|k2] [--no-postprocess]
        """

        fun parse(raw: List<String>): Args {
            var input: Path? = null
            var output: Path? = null
            var diag: Path? = null
            var kind = J2kConverterExtension.Kind.K1_NEW
            var postProcess = true

            val it = raw.iterator()
            while (it.hasNext()) {
                when (val arg = it.next()) {
                    "--input" -> input = Path(it.nextOrFail(arg))
                    "--output" -> output = Path(it.nextOrFail(arg))
                    "--diagnostics" -> diag = Path(it.nextOrFail(arg))
                    "--converter" -> kind = parseKind(it.nextOrFail(arg))
                    "--no-postprocess" -> postProcess = false
                    "--help", "-h" -> die(0, USAGE.trimIndent())
                    else -> die(2, "unknown argument: $arg\n${USAGE.trimIndent()}")
                }
            }

            requireNotNull(input) { die(2, "--input is required\n${USAGE.trimIndent()}") }
            requireNotNull(output) { die(2, "--output is required\n${USAGE.trimIndent()}") }
            requireNotNull(diag) { die(2, "--diagnostics is required\n${USAGE.trimIndent()}") }

            if (!input.exists() || !input.isDirectory()) {
                die(2, "--input directory does not exist or is not a directory: $input")
            }

            return Args(input, output, diag, kind, postProcess)
        }

        private fun Iterator<String>.nextOrFail(flag: String): String =
            if (hasNext()) next() else die(2, "$flag requires a value")

        private fun parseKind(value: String): J2kConverterExtension.Kind = when (value.lowercase()) {
            "k1", "k1_new", "nj2k" -> J2kConverterExtension.Kind.K1_NEW
            "k2", "k2_new" -> J2kConverterExtension.Kind.K2
            else -> die(2, "--converter must be 'k1' or 'k2', got '$value'")
        }

        private fun die(code: Int, msg: String): Nothing {
            (if (code == 0) System.out else System.err).println(msg)
            exitProcess(code)
        }
    }
}
