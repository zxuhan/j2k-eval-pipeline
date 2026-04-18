package anonymousinner;

// HYPOTHESIS:
// Java anonymous inner classes capture the enclosing `this` implicitly; Kotlin
// `object : X { ... }` does too, but a *nested* anonymous class that refers to
// the outer anonymous class's own members via implicit `this` is ambiguous in
// Kotlin without a qualified `this@Outer`. j2k tends to emit plain `this`
// and rely on inference, which compiles for the inner instance but refers to
// the wrong receiver for the outer. Predicted: compiles, but semantics subtly
// shift when both layers define a member with the same name.
public final class NestedAnonymous {
    interface Action { void run(); }

    public Action outer() {
        return new Action() {
            int seq = 0;
            @Override public void run() {
                Action inner = new Action() {
                    @Override public void run() {
                        seq++;
                    }
                };
                inner.run();
            }
        };
    }
}
