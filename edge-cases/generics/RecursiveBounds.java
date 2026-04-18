package generics;

// HYPOTHESIS:
// Self-bounded generics (`<T extends Comparable<T>>`) translate cleanly to Kotlin
// (`<T : Comparable<T>>`), but F-bounded polymorphism of the form
// `<T extends Self<T>>` is idiomatic Java for typed builders and relies on
// `getThis()` to return `T`. Kotlin prefers `abstract class Self<T : Self<T>>`
// + `protected abstract fun self(): T`. j2k will almost certainly emit the
// literal shape unchanged — correct but not idiomatic — and `this as T` will
// appear where Java used an unchecked cast, raising an UNCHECKED_CAST warning.
// Predicted: compiles, idiom score low.
public abstract class RecursiveBounds<T extends RecursiveBounds<T>> {
    protected int count;

    @SuppressWarnings("unchecked")
    public T increment() {
        count++;
        return (T) this;
    }

    public abstract T self();

    public static final class Concrete extends RecursiveBounds<Concrete> {
        @Override
        public Concrete self() { return this; }
    }
}
