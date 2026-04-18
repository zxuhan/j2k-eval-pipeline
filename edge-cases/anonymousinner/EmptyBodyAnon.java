package anonymousinner;

// HYPOTHESIS:
// An empty-bodied anonymous class with no overrides is legal in Java (creates
// a subclass of concrete type `Base`). Kotlin's `object : Base() {}` is the
// natural mapping. j2k handles this but does not collapse to a direct `Base()`
// constructor call even though the empty override set makes the anonymous
// class equivalent. Not a correctness issue; an idiom gap.
// Predicted: compiles, `object : Base() {}` shape preserved verbatim.
public final class EmptyBodyAnon {
    static class Base {
        public String tag() { return "base"; }
    }

    public Base make() {
        return new Base() {
            // intentionally empty
        };
    }
}
