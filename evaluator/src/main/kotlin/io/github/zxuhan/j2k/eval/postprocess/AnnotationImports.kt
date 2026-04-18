package io.github.zxuhan.j2k.eval.postprocess

/**
 * Map from annotation simple names known to be dropped by j2k's import
 * resolution to the fully-qualified import that should be restored.
 *
 * OkHttp 3.14.9 is annotated with JSR-305 (`javax.annotation.*`). j2k preserves
 * the annotation usages on members but does not synthesize the corresponding
 * `import javax.annotation.<Name>` directive, producing `Unresolved reference`
 * errors on 55/87 compile-failed files in the real run.
 *
 * The mapping is intentionally conservative — only annotations that are
 * unambiguously JSR-305 and that have no common collision with other
 * nullability families (JetBrains `org.jetbrains.annotations.*`, Android
 * `androidx.annotation.*`). If the file already imports the same short name
 * from a different package, we leave it alone.
 */
object AnnotationImports {

    val jsr305Defaults: Map<String, String> = mapOf(
        "Nullable" to "javax.annotation.Nullable",
        "Nonnull" to "javax.annotation.Nonnull",
        "CheckReturnValue" to "javax.annotation.CheckReturnValue",
        "ParametersAreNonnullByDefault" to "javax.annotation.ParametersAreNonnullByDefault",
        "ParametersAreNullableByDefault" to "javax.annotation.ParametersAreNullableByDefault",
        "Immutable" to "javax.annotation.concurrent.Immutable",
        "ThreadSafe" to "javax.annotation.concurrent.ThreadSafe",
        "NotThreadSafe" to "javax.annotation.concurrent.NotThreadSafe",
    )
}
