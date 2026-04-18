package annotations;

// HYPOTHESIS:
// `@Deprecated(forRemoval = true, since = "1.0")` has no lossless Kotlin form.
// The closest equivalent is `@Deprecated("...", level = DeprecationLevel.ERROR)`
// plus `@ReplaceWith` if a replacement is given. j2k today drops both `forRemoval`
// and `since`, emitting a bare `@Deprecated("")` with an empty message — which
// the Kotlin compiler warns about (`@Deprecated message must not be empty` in
// recent versions). Predicted: compiles with warnings; semantic loss.
public final class DeprecatedForRemoval {
    @Deprecated(forRemoval = true, since = "1.0")
    public static void oldApi() {}

    @Deprecated(since = "0.9")
    public static void olderApi() {}
}
