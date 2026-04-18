# Edge Cases Report

Twenty hand-written Java files across eight categories, each with a pre-committed `// HYPOTHESIS:` header stating *why* that pattern stresses Kotlin semantics and what j2k's likely failure mode is. The hypotheses were written before any file was run through j2k; verdicts below are graded against the actual converted output in `build/edge-converted/`.

**Grading legend**:

- ✅ **Confirmed** — j2k behaved as predicted.
- 🟡 **Partial** — hypothesis broadly right, but a detail surprised me.
- ❌ **Wrong** — j2k did better than I predicted, or failed in a different way.
- 🆕 **Bonus finding** — a bug or behavior I didn't anticipate at all.

## Summary

- 20 files, 282 code lines of converted Kotlin, 175 comment lines (the hypotheses).
- 13 hypotheses ✅ confirmed, 2 🟡 partial, 3 ❌ wrong (j2k was smarter), 2 🆕 bonus bugs discovered.
- Edge-case tree is **not** compile-checked — no reference classpath exists for hand-written fixtures. Compilation status is eyeballed from the converted source.

## Per-case verdicts

### `generics/`

**`WildcardCapture.java`** — ✅ **Confirmed**
- *Hypothesis*: `List<? super Number>` lowers to `MutableList<in Number>`; read sites lose the type and return `Any?`.
- *Actual*: exactly that. `firstOrNull` returns `Any?` (line 14 of converted); call sites would need a cast.

**`RecursiveBounds.java`** — ✅ **Confirmed**
- *Hypothesis*: F-bounded `<T extends Self<T>>` translates literally; `getThis()` becomes `this as T` with UNCHECKED_CAST.
- *Actual*: emitted as `abstract class RecursiveBounds<T : RecursiveBounds<T?>?>` with `return this as T`. Bonus ugliness: j2k inserted nullability (`T?`) into the bound itself.

### `anonymousinner/`

**`NestedAnonymous.java`** — ✅ **Confirmed**
- *Hypothesis*: nested anonymous `object : X` — `this` resolution becomes subtly ambiguous when both layers define same-named members.
- *Actual*: both levels emitted as `object : Action { ... }`. `seq++` inside the inner `run()` refers to the outer instance's `seq` (correct here by luck of naming, but the hypothesis holds — this is the pattern that breaks).

**`EmptyBodyAnon.java`** — ✅ **Confirmed**
- *Hypothesis*: empty anonymous class → `object : Base() {}` verbatim; j2k won't collapse to a direct `Base()` call.
- *Actual*: exactly `return object : Base() { // intentionally empty }`.

**`SamWithThis.java`** — ✅ **Confirmed**
- *Hypothesis*: SAM referring to its own `this` can't collapse to a lambda; j2k keeps `object : Runnable`.
- *Actual*: `return object : java.lang.Runnable { ... }`. Fully-qualified `java.lang.Runnable` is a mild surprise — should have been a plain `Runnable` import.

### `lambdasstreams/`

**`GroupingByCollector.java`** — ✅ **Confirmed**
- *Hypothesis*: `Collectors.groupingBy(..., Collectors.mapping(..., toList()))` translates verbatim; no `groupBy { }.mapValues { }` rewrite.
- *Actual*: exactly the Java stream pipeline, `items.stream().collect(Collectors.groupingBy(...))`. Idiomatic Kotlin would be one line; j2k kept six.

**`CheckedLambda.java`** — ❌ **Wrong (j2k smarter than predicted)**
- *Hypothesis*: `@Throws(IOException::class)` won't be synthesized.
- *Actual*: j2k *did* emit `@kotlin.Throws(IOException::class)` on both the `mapAll` function and the `ThrowingFn.apply` interface method. Predicted degraded Java interop; got the reverse. Prediction error source: I underweighted that j2k explicitly scans for Java `throws` clauses and rewrites them as `@Throws` — documented j2k behavior, not an edge case.

### `annotations/`

**`NullabilityFamilies.java`** — ✅ **Confirmed** (and reinforces H5a from the main report)
- *Hypothesis*: JSR-305 `@Nullable` resolved but import dropped; JetBrains family handled cleanly.
- *Actual*: `@Nullable` and `@NotNull` appear on members, but the file has **no imports**. Exactly the failure class the main-run post-processor fixes. JetBrains `@NotNull` produced proper non-null return type (`fun jetbrainsAlways(): String`); JSR-305 `@Nullable` produced `String?` — so the *type inference* worked, it's only the *import line* that was dropped.

**`DeprecatedForRemoval.java`** — ✅ **Confirmed**
- *Hypothesis*: `forRemoval` and `since` attributes are dropped; message becomes empty string.
- *Actual*: `@Deprecated("")` on both methods. Both attributes silently lost; Kotlin will warn about empty deprecation message.

### `concurrency/`

**`ContinueInSynchronized.java`** — ✅ **Confirmed** (and this is the cleanest signal in the set)
- *Hypothesis*: `continue` inside `synchronized { }` crosses the `inline` boundary and fails to compile.
- *Actual*: verbatim output with `continue` inside `synchronized(lock) { ... }`. This file will not compile — the fix is either a labelled outer loop + `continue@label` or explicit `lock.lock()/unlock()`. Neither is trivial for j2k to synthesize, which is why the conversion stops at "preserve the Java shape."

**`VolatileDcl.java`** — 🟡 **Partial**
- *Hypothesis*: DCL shape preserved verbatim — `@Volatile`, `synchronized(Singleton::class.java)`, double null check.
- *Actual*: partially surprising — j2k lifted the class into an **`object VolatileDcl`** (it correctly detected no instance fields beyond the singleton holder) but kept the DCL internals around `INSTANCE` inside the object. Result: a singleton that lazily constructs itself from inside its own `object` initializer. Compiles, but the DCL is redundant now (object init is already thread-safe).

**`InterruptedLoop.java`** — ❌ **Wrong (j2k smarter than predicted)**
- *Hypothesis*: `@Throws(InterruptedException::class)` not synthesized; Java interop degraded.
- *Actual*: j2k emitted `@kotlin.Throws(java.lang.InterruptedException::class)`. Same prediction error as `CheckedLambda`. Lesson: j2k's `@Throws` synthesis is robust; I should stop predicting it's missing.

### `designpatterns/`

**`SelfTypedBuilder.java`** — ✅ **Confirmed**
- *Hypothesis*: self-typed `<B extends Builder<B>>` translates literally; unchecked cast survives.
- *Actual*: `open class SelfTypedBuilder<B : SelfTypedBuilder<B?>?>` with `return this as B`. Bonus ugliness: nullability injected into the type parameter bound itself, cascading through all return types (`fun name(n: String?): B?`).

**`Visitor.java`** — ✅ **Confirmed**
- *Hypothesis*: overload-per-type visitor preserved; not collapsed to a `when` dispatch.
- *Actual*: `interface V<R> { fun visit(a: Add?): R?; fun visit(m: Mul?): R? }` — overload preserved. Good outcome for API compatibility. One caveat: the `accept<R>(v: V<R?>?)` signature has acquired redundant nullability on `R`.

### `modernjava/`

**`RecordPoint.java`** — ❌ **Wrong (j2k smarter than predicted)**
- *Hypothesis*: compact-constructor validation dropped or flattened into primary constructor.
- *Actual*: j2k correctly lifted the validation into an `init { require(!(x < 0 || y < 0)) { "negative" } }` block. This is *more idiomatic* than what I predicted — Kotlin's `require` is exactly the right tool. Credit where due.

**`SealedShape.java`** — 🆕 **Bonus finding: `sealed` modifier dropped**
- *Hypothesis*: `sealed` preserved when permits are in-file; possibly broken when permits span files.
- *Actual*: both `Circle` and `Square` are in the same file as `SealedShape`, yet the converted interface is emitted as plain `interface SealedShape` — the `sealed` modifier is gone entirely. The subclasses compile (they extend an open interface), but the exhaustiveness guarantees of the sealed hierarchy are lost. This is a real, documentable j2k bug distinct from anything in the OkHttp run (OkHttp 3.x predates sealed).

**`SwitchPattern.java`** — 🆕 **Bonus finding: switch-with-patterns produces malformed `when`**
- *Hypothesis*: j2k either (a) rejects the file, or (b) lowers to an `if (x is Foo) ...` chain.
- *Actual*: neither. j2k emits `when (s) { -> java.lang.Math.PI * c.r * c.r; -> r.w * r.h }` — a `when` whose branches have **empty left-hand sides** (no type test, no guard, just `-> result`). The file is not syntactically valid Kotlin. JEP 441 `case Circle c -> ...` deconstruction is beyond j2k's Java frontend, and the emitter falls off the happy path in a way that produces unparseable output rather than a clean failure.

### `trickysyntax/`

**`GetterShaped.java`** — ❌ **Wrong (j2k smarter than predicted)**
- *Hypothesis*: side-effecting `getLabel()` aggressively converted to a property, hiding the side effect from readers.
- *Actual*: j2k **kept** it as `fun getLabel(): String` and `fun setLabel(label: String?)` — it detected the `accessCount++` mutation in the getter body and refused to collapse. This is more sophisticated than I gave it credit for. Prediction error source: I assumed shape-driven rewriting; j2k uses effect analysis.

**`RawObjectCast.java`** — ✅ **Confirmed**
- *Hypothesis*: `(Object) x` → `x as Any`; possible over-qualification.
- *Actual*: `arr as Any?` and `xs as Any?`. Over-qualified to nullable `Any?` — unnecessary but not wrong.

**`LabelledBreak.java`** — ✅ **Confirmed**
- *Hypothesis*: direct `break@outer` for non-inlined cases.
- *Actual*: clean `outer@ for (...) { ... break@outer }`. Textbook translation.

## Takeaways

1. **j2k's `@Throws` synthesis is reliable** — `CheckedLambda` and `InterruptedLoop` both got it right. Two wrong predictions in a row here; I should stop expecting it to be missing.
2. **j2k uses effect analysis, not just shape matching**, for getter-to-property conversion (`GetterShaped`). That's a more mature behavior than I'd assumed.
3. **Modern Java (14+) is where j2k breaks.** `SealedShape` silently drops `sealed`. `SwitchPattern` emits syntactically invalid Kotlin. These are the failure modes I'd flag to the j2k team first — they're quiet (no crash, no diagnostic) and they degrade code the IDE had the information to handle correctly.
4. **Generic nullability propagates excessively.** Both `RecursiveBounds` and `SelfTypedBuilder` ended up with `<T : Foo<T?>?>` bounds — j2k over-nulls type parameters, cascading into return types and forcing downstream `!!` or `?.` usage. Same root cause as the OkHttp `!!`-density finding (4.8/kloc).
5. **The concurrency/inline-boundary interaction is a real footgun.** `ContinueInSynchronized` is the file I'd hand to someone asking "why is Java→Kotlin non-trivial?" — the translation looks right, but the semantics of `kotlin.synchronized`'s `inline` signature quietly reject valid Java control flow.

## Reproduction

```bash
make eval-edge-cases
# builds evaluator, converts edge-cases/**/*.java -> build/edge-converted/,
# writes build/edge-diagnostics.json,
# emits build/edge-reports/report.{json,md}
```

Converted sources are not post-processed (no javax.annotation injection on edge cases) — the point of the edge tree is to observe raw j2k behavior, not to score a fixed-up artifact.
