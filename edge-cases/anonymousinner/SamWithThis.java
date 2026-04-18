package anonymousinner;

// HYPOTHESIS:
// A Java SAM anonymous class that references its own `this` cannot be collapsed
// to a Kotlin lambda — the lambda's receiver is the enclosing scope, not the
// implementing instance. j2k has to preserve `object : Runnable { ... }` form.
// The interesting failure mode is when the SAM refers to its own `hashCode()`
// or a helper method defined on the anonymous class: in Java that resolves to
// the anonymous subclass's override; a lambda would resolve to the lambda
// itself (a different instance). Predicted: compiles; j2k keeps object form.
public final class SamWithThis {
    public Runnable make() {
        return new Runnable() {
            @Override public void run() {
                System.out.println("hash=" + this.hashCode());
            }
        };
    }
}
