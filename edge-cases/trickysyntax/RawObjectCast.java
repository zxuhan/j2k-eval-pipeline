package trickysyntax;

import java.util.List;

// HYPOTHESIS:
// A raw `Object` cast (`(Object) x`) in Java is used occasionally to disambiguate
// varargs or to force boxing. In Kotlin, `Any` is the equivalent, and `x as Any`
// works, but j2k may over-qualify to `kotlin.Any` or drop the cast entirely
// when it believes type inference suffices. The varargs-disambiguation case is
// particularly fragile: `System.out.println((Object) arr)` prints the array's
// identity hashCode, while `println(arr)` in Kotlin prints the array contents
// for `IntArray` but not `Array<Int>`. Predicted: compiles; output differs
// from Java semantics in the varargs case.
public final class RawObjectCast {
    public static void show(int[] arr) {
        System.out.println((Object) arr);
    }

    public static Object asObject(List<?> xs) {
        return (Object) xs;
    }
}
