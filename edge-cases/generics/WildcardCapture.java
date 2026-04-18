package generics;

import java.util.Collections;
import java.util.List;

// HYPOTHESIS:
// Java `? super T` is contravariant capture; Kotlin's analogue is `in T` on the
// use site. j2k typically lowers `List<? super Number>` to `MutableList<in Number>`,
// which is semantically equivalent for writes but changes the read-site type to
// `Any?` — call sites that did `list.get(i)` in Java now force an explicit cast
// or else fail to compile. PSI signature-token comparison will also register
// this as a shape change vs the OkHttp 4.x hand-migration, which uses
// `MutableList<Any>` here (the maintainers relaxed the bound).
// Predicted: converts, but read-site usages may require `!!` or a cast.
public final class WildcardCapture {
    public static void addNumbers(List<? super Number> sink, List<? extends Number> src) {
        for (Number n : src) {
            sink.add(n);
        }
    }

    public static Object firstOrNull(List<? super Number> sink) {
        return sink.isEmpty() ? null : sink.get(0);
    }

    public static void main(String[] args) {
        addNumbers(Collections.emptyList(), Collections.emptyList());
    }
}
