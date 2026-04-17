package io.github.zxuhan.j2k.eval.model

data class CompareFileResult(
    val path: String,
    val referencePath: String,
    val similarity: Double,
    val convertedTokens: Int,
    val referenceTokens: Int,
)

data class ComparativeReport(
    val referenceRoot: String,
    val pairCount: Int,
    val unmatchedConvertedCount: Int,
    val unmatchedReferenceCount: Int,
    val meanSimilarity: Double,
    val medianSimilarity: Double,
    val p90Similarity: Double,
    val files: List<CompareFileResult>,
    val topDivergent: List<CompareFileResult>,
)

data class CompileFileResult(
    val path: String,
    val compiled: Boolean,
    val errors: List<String>,
)

data class CompileReport(
    val compiledCount: Int,
    val failedCount: Int,
    val passRate: Double,
    val files: List<CompileFileResult>,
)

data class Report(
    val aggregate: AggregateAnalysis,
    val comparative: ComparativeReport? = null,
    val compile: CompileReport? = null,
)
