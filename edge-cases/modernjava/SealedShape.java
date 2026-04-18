package modernjava;

// HYPOTHESIS:
// Java sealed interfaces/classes map to Kotlin `sealed` hierarchies, which is
// the modeled use case. One quirk: Java `permits` is explicit, Kotlin infers
// permitted subclasses from same-package/same-file scoping. j2k preserves
// the `sealed` modifier and moves permitted types into the same file if they
// aren't already there. If the original Java had permits spanning multiple
// files, the conversion may emit a `sealed` class with no permitted subclasses
// visible to the compiler, failing. Predicted: compiles when permits are
// in-file; likely fails when they're split.
public sealed interface SealedShape permits Circle, Square {
    double area();
}

record Circle(double r) implements SealedShape {
    @Override public double area() { return Math.PI * r * r; }
}

record Square(double side) implements SealedShape {
    @Override public double area() { return side * side; }
}
