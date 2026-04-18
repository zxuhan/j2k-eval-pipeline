package modernjava;

// HYPOTHESIS:
// Java records map naturally to Kotlin `data class`, but with subtle gaps:
// (1) compact constructors with parameter validation have no 1:1 Kotlin form —
// validation must be moved to an `init { }` block; (2) `@Override`-style
// accessor methods on a record are implicit in Java but must be explicit
// `override` in Kotlin. j2k handles the basic shape but often does not
// synthesize `init` validation from compact constructors: the checks either
// disappear or appear in the primary constructor as manual `require()` calls
// (which is actually more idiomatic — the conversion may be *better* than
// the source). Predicted: compiles; validation preserved as `require()`.
public record RecordPoint(int x, int y) {
    public RecordPoint {
        if (x < 0 || y < 0) throw new IllegalArgumentException("negative");
    }

    public int manhattan() { return Math.abs(x) + Math.abs(y); }
}
