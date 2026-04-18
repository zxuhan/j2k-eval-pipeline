package designpatterns;

// HYPOTHESIS:
// A self-typed builder with `<B extends Builder<B>>` is Java's way of keeping
// fluent builders inheritable. Kotlin's idiomatic path is an `apply { }` DSL
// or a class with a cloning `copy()`. j2k translates the shape literally:
// `open class Builder<B : Builder<B>>`, `fun self(): B = this as B` with an
// unchecked cast warning. Compiles, but the API is an eyesore and the cast
// triggers UNCHECKED_CAST in Kotlin's stricter-than-Java generics checker.
// Predicted: compiles with warnings.
public class SelfTypedBuilder<B extends SelfTypedBuilder<B>> {
    String name;
    int weight;

    @SuppressWarnings("unchecked")
    public B name(String n) { this.name = n; return (B) this; }

    @SuppressWarnings("unchecked")
    public B weight(int w) { this.weight = w; return (B) this; }

    public static final class Concrete extends SelfTypedBuilder<Concrete> {
        public String build() { return name + ":" + weight; }
    }
}
