package concurrency;

// HYPOTHESIS:
// `Thread.interrupted()` clears the interrupt flag as a side effect; Kotlin
// has no separate API and j2k calls through to `Thread.interrupted()` directly,
// which is correct. The subtle trap: j2k may lower `while (!Thread.interrupted())`
// into a Kotlin `while` loop verbatim, but if the loop body also throws
// InterruptedException, Kotlin won't force a `@Throws(InterruptedException::class)`
// annotation, silently dropping the checked-exception contract that Java
// callers depend on. Predicted: compiles, but the `@Throws` annotation is not
// synthesized; Java interop degraded.
public final class InterruptedLoop {
    public int spin() throws InterruptedException {
        int n = 0;
        while (!Thread.interrupted()) {
            n++;
            if (n > 100_000) throw new InterruptedException("cap");
        }
        return n;
    }
}
