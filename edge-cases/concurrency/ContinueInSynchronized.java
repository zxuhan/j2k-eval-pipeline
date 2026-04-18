package concurrency;

import java.util.Iterator;
import java.util.List;

// HYPOTHESIS:
// `synchronized(lock) { ... }` in Java becomes `synchronized(lock) { ... }` in
// Kotlin, but `kotlin.synchronized` is an INLINE FUNCTION taking a lambda.
// A `continue` inside that lambda crosses the inline boundary, which the
// Kotlin compiler rejects with `'continue' is not allowed here`. j2k emits
// the literal shape, so any `continue`/`break` inside a synchronized block
// in Java produces uncompilable Kotlin. The fix is either a labelled outer
// loop + `continue@label` (works because non-local returns/breaks through
// inline boundaries are allowed when the target is labelled and lexically
// enclosing) OR explicit `lock.lock()/unlock()` in a try/finally.
// Predicted: FAIL to compile.
public final class ContinueInSynchronized {
    private final Object lock = new Object();

    public int countReady(List<Iterator<Boolean>> iters) {
        int c = 0;
        for (Iterator<Boolean> it : iters) {
            synchronized (lock) {
                if (!it.hasNext()) continue;
                if (!it.next()) continue;
                c++;
            }
        }
        return c;
    }
}
