package designpatterns;

// HYPOTHESIS:
// The classic visitor pattern relies on overloaded dispatch by declared type:
// `void visit(Add)`, `void visit(Mul)`. Kotlin supports overloads, but j2k may
// (depending on the file) collapse them into a single method dispatching on
// `when (node) { is Add -> ... }`. The transformation is sometimes correct,
// sometimes lossy when the visitor is part of a published API where the
// overload-per-type signature is the contract. Predicted: compiles; API shape
// preserved in most cases, but watch for lowering to a `when`.
public final class Visitor {
    public interface Node {
        <R> R accept(V<R> v);
    }

    public interface V<R> {
        R visit(Add a);
        R visit(Mul m);
    }

    public static final class Add implements Node {
        public final int a, b;
        public Add(int a, int b) { this.a = a; this.b = b; }
        @Override public <R> R accept(V<R> v) { return v.visit(this); }
    }

    public static final class Mul implements Node {
        public final int a, b;
        public Mul(int a, int b) { this.a = a; this.b = b; }
        @Override public <R> R accept(V<R> v) { return v.visit(this); }
    }
}
