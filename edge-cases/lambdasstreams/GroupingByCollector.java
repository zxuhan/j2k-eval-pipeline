package lambdasstreams;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// HYPOTHESIS:
// `Collectors.groupingBy(classifier, mapping(f, toList()))` has no 1:1 Kotlin
// stdlib analogue. Kotlin's `groupBy(f).mapValues { (_, v) -> v.map(g) }` is
// idiomatic, but j2k translates the stream pipeline verbatim, keeping
// `java.util.stream.Collectors` and `.stream()` calls. The output compiles on
// JVM (streams are available) but reads as Java-in-Kotlin. Higher `JavaIsm`
// score; lower `Idiom` score. Predicted: compiles, stream API preserved.
public final class GroupingByCollector {
    public record Item(String bucket, int value) {}

    public static Map<String, List<Integer>> groupValues(List<Item> items) {
        return items.stream()
            .collect(Collectors.groupingBy(
                Item::bucket,
                Collectors.mapping(Item::value, Collectors.toList())
            ));
    }
}
