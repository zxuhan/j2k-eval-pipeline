package concurrency;

// HYPOTHESIS:
// Double-checked-locking singletons are a canonical Java idiom for lazy init.
// The idiomatic Kotlin equivalent is `object Singleton` (eager) or
// `private val instance by lazy { ... }` (lazy, thread-safe by default). j2k
// preserves the DCL shape verbatim: a `@Volatile` companion field, a
// `synchronized(Singleton::class.java)` block, double null check. Correct and
// compiles, but reads as Java with Kotlin keywords. The IdiomAnalyzer should
// flag "manual DCL" as a negative idiom signal; the ComparativeAnalyzer will
// see divergence against OkHttp 4.x, which uses `by lazy` in several places.
// Predicted: compiles, idiom score tanked.
public final class VolatileDcl {
    private static volatile VolatileDcl INSTANCE;

    private VolatileDcl() {}

    public static VolatileDcl instance() {
        VolatileDcl local = INSTANCE;
        if (local == null) {
            synchronized (VolatileDcl.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new VolatileDcl();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }
}
