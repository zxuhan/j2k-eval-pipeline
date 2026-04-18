package trickysyntax;

// HYPOTHESIS:
// j2k aggressively converts `getFoo()` / `setFoo(x)` pairs into Kotlin
// properties, which is correct for pure accessors but wrong when the getter
// has SIDE EFFECTS (logging, lazy init, counter bump). The resulting Kotlin
// property hides the side effect from readers who expect `x.foo` to be cheap
// and pure. The Kotlin idiom for a side-effecting getter is an explicit
// `fun computeFoo(): T`; the property conversion is semantically equivalent
// but stylistically wrong. Predicted: compiles; subtle readability regression.
public final class GetterShaped {
    private int accessCount = 0;
    private String label;

    public GetterShaped(String label) { this.label = label; }

    // Looks like a getter but mutates state.
    public String getLabel() {
        accessCount++;
        return label + ":" + accessCount;
    }

    public void setLabel(String label) { this.label = label; }
}
