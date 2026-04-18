package lambdasstreams;

import java.io.IOException;
import java.util.List;

// HYPOTHESIS:
// Kotlin has no checked exceptions. A Java lambda that declares `throws IOException`
// cannot satisfy `Function<T, R>` in Java without wrapping; the idiomatic Java
// workaround is a helper interface with a `throws` clause. j2k converts the
// helper interface as `fun interface ThrowingFn<T, R> { fun apply(t: T): R }`
// and drops the `throws` annotation. Callers still need try/catch because the
// underlying IO op throws, but Kotlin will neither enforce nor declare it.
// Predicted: compiles, `@Throws` annotation likely *not* generated, surprising
// Java callers that relied on the checked signature.
public final class CheckedLambda {
    @FunctionalInterface
    public interface ThrowingFn<T, R> {
        R apply(T t) throws IOException;
    }

    public static <T, R> List<R> mapAll(List<T> in, ThrowingFn<T, R> fn) throws IOException {
        for (T t : in) { fn.apply(t); }
        return List.of();
    }
}
