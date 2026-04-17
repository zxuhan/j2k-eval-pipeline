package io.github.zxuhan.j2k.eval.model

data class StructuralMetrics(
    val classes: Int,
    val objects: Int,
    val interfaces: Int,
    val functions: Int,
    val properties: Int,
    val maxNestingDepth: Int,
    val meanNestingDepth: Double,
    val codeLines: Int,
    val commentLines: Int,
) {
    val commentRatio: Double
        get() = if (codeLines + commentLines == 0) 0.0
        else commentLines.toDouble() / (codeLines + commentLines)
}

data class IdiomMetrics(
    val dataClasses: Int,
    val objectSingletons: Int,
    val companionObjects: Int,
    val lambdaArguments: Int,
    val samObjectExpressions: Int,
    val trailingLambdas: Int,
    val whenExpressions: Int,
    val ifElseChains: Int,
    val stringTemplates: Int,
    val scopeFunctionCalls: Int,
    val propertyAccessors: Int,
)

data class JavaIsmMetrics(
    val notNullAssertions: Int,
    val notNullAssertionsPerKloc: Double,
    val explicitGetterSetterPairs: Int,
    val javaUtilCollectionRefs: Int,
    val ifNotNullGuards: Int,
    val cStyleForLoops: Int,
    val synchronizedAnnotations: Int,
    val platformTypeReferences: Int,
)

data class FileAnalysis(
    val path: String,
    val structural: StructuralMetrics,
    val idiom: IdiomMetrics,
    val javaIsm: JavaIsmMetrics,
)

data class AggregateAnalysis(
    val files: List<FileAnalysis>,
) {
    val fileCount: Int get() = files.size
    val totalCodeLines: Int get() = files.sumOf { it.structural.codeLines }
    val totalNotNullAssertions: Int get() = files.sumOf { it.javaIsm.notNullAssertions }
}
