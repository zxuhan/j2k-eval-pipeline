package annotations;

import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

// HYPOTHESIS:
// Three nullability annotation families coexist in real-world Java: JSR-305
// (`javax.annotation.Nullable`), JetBrains (`org.jetbrains.annotations.NotNull`),
// and Android (`androidx.annotation.Nullable`). j2k's resolver reliably handles
// the JetBrains family (it's the IntelliJ standard) but treats JSR-305 and
// Android inconsistently across versions. Commit-4 evidence: converted files
// reference `javax.annotation.Nullable` symbols without emitting imports,
// producing unresolved references. Predicted: compile failure on files that
// used JSR-305; JetBrains-annotated members get proper `?` / non-null types.
public final class NullabilityFamilies {
    @Nullable
    public static String jsrMaybe() { return null; }

    @NotNull
    public static String jetbrainsAlways() { return "x"; }

    public static int length(@Nullable String s) {
        return s == null ? 0 : s.length();
    }
}
