package modernjava;

// HYPOTHESIS:
// Pattern-matching `switch` (JEP 441, Java 21) has no isomorphic Kotlin form.
// Kotlin `when` supports type checks (`is T`) and guard predicates, but does
// not support record deconstruction patterns — those need manual destructuring
// (`val (x, y) = p`). j2k at IC-2025.2.6 likely does not understand JEP 441
// syntax at all and fails to parse the file, or lowers the switch to a chain
// of `if (x is Foo) ... else if (x is Bar) ...`. Predicted: either (a) j2k
// rejects the file with a parse error, or (b) a functional-but-verbose
// `when` with manual destructuring.
public final class SwitchPattern {
    sealed interface Shape permits C, R {}
    record C(double r) implements Shape {}
    record R(double w, double h) implements Shape {}

    public static double area(Shape s) {
        return switch (s) {
            case C c -> Math.PI * c.r() * c.r();
            case R r -> r.w() * r.h();
        };
    }
}
