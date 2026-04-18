# j2k Evaluation Report — OkHttp 3.14.9

## TL;DR

Ran IntelliJ Platform 2025.2.6's K2 J2K converter headlessly over OkHttp 3.14.9's `okhttp/src/main/java` (104 Java files, ~30k code lines), then scored the output with a PSI-based evaluator. Comparative similarity against OkHttp's own hand-migrated 4.12.0 Kotlin: mean 0.61, p90 0.80. Out-of-the-box compile rate: **17/104 (16.3%)**. One targeted post-processor (restores dropped `javax.annotation.*` imports) lifts that to **25/104 (24.0%)** and eliminates 77 JSR-305 errors entirely.

All numbers in this report come from committed JSON artifacts under `build/report.json`, `build/raw-reports/report.json`, and `build/edge-reports/report.json`, which CI also uploads. No hand-typed metrics.

## Methodology

- **Converter**: a custom IntelliJ plugin registers an `ApplicationStarter` and drives conversion through the `org.jetbrains.kotlin.j2kConverterExtension` extension point — the same path the IDE takes for "Convert Java File to Kotlin." K2 J2K, post-processor on by default.
- **Target scope**: `okhttp/src/main/java` only. Tests/samples/Android/mockwebserver excluded: they'd blow the CI budget and aren't representative of the library core.
- **Semantic oracle**: OkHttp 4.12.0's `okhttp/src/jvmMain/kotlin` — the same project, hand-migrated by the maintainers. Gives a real "how close did j2k get?" baseline rather than a hand-wavy idiom score.
- **Evaluator**: Kotlin PSI via `kotlin-compiler-embeddable`. Four analyzers — Structural, Idiom, Java-ism, Comparative (signature-token Levenshtein) — plus an in-process `K2JVMCompiler` for the compile check, fed OkHttp 3.14.9's runtime classpath (17 jars, transcribed from the tagged `build.gradle` — the 2019-era Gradle 5.x build doesn't run on a modern JDK).
- **Converter mode**: K2 only. NJ2K (`Kind.K1_NEW`) is not registered at IC-2025.2.6; the extension point exposes only `Kind.K2`. A K1 vs K2 matrix would need a separate K1-mode IDE distribution.

## Hypotheses

Pre-committed at the edge-cases commit (before the real run), graded here.

| # | Hypothesis | Outcome |
|---|---|---|
| **H1** | j2k converts ~100% of files syntactically — the converter rarely crashes mid-file, it produces *something*. | ✅ **Confirmed**. 104/104 files emitted; no conversion failures. |
| **H2** | Comparative similarity against the maintainers' 4.x Kotlin sits in 0.55–0.75 mean: structure preserved, but divergence from idiom differences (builder style, nullability annotations, stream-vs-sequence). | ✅ **Confirmed**. Mean 0.6064, median 0.6154, p90 0.8000. |
| **H3** | Idiom gap dominated by one thing: j2k preserves Java builder/DCL/collection-streaming verbatim rather than rewriting to Kotlin's idioms (`apply` / `by lazy` / `groupBy`). Expect low counts of `data class`, scope functions, and property accessors relative to function count. | ✅ **Confirmed**. 0 data classes across 158 classes, 13 scope-function calls across 1,373 functions (~0.9%), 31 property accessors vs 2,224 properties (~1.4%), 66 companion objects vs 8 singletons. |
| **H4** | Java-isms survive in measurable density: `!!` assertions, `@Synchronized`, if-not-null guards. Expect `!!` rate > 2 per kloc. | ✅ **Confirmed**. 159 `!!` across 30,208 code lines (**4.8 per kloc**), 72 `@Synchronized`, 170 if-not-null guards, 26 `java.util.*` refs. |
| **H5a** | **Originally** "5–15% of files fail to compile." Pilot run killed that. Revised before edge-cases commit: *"dominant compile-failure class is j2k dropping `javax.annotation.*` imports while keeping the annotation usages."* | ✅ **Confirmed**. 77 of 216 unresolved-reference errors in the raw run were JSR-305 (`Nullable`, `Nonnull`, `CheckReturnValue`, `ParametersAreNonnullByDefault`). Single-file post-processor drops that to 0. |
| **H5b** | *"Secondary failure class is j2k emitting `class Foo : Interface()` with parens on a Java interface supertype — 2.2 rejects it."* | 🟡 **Partial**. Real and reproducible on `Authenticator.kt`, `Dns.kt`, `EventListener.kt` (3 files) — but 3 files, not the dominant failure mode the pilot suggested. Scope: not fixed in this submission; proposed upstream patch sketched below. |

## Results

### Structural

From `build/report.json` on the post-processed tree:

| metric | total |
|---|---:|
| files | 104 |
| code lines | 30,208 |
| classes | 158 |
| objects | 107 |
| interfaces | 20 |
| functions | 1,373 |
| properties | 2,224 |
| comment lines | 2,823 |
| max nesting depth | 8 |

### Idiom signals

| metric | total | rate |
|---|---:|---:|
| data classes | 0 | — |
| object singletons | 8 | — |
| companion objects | 66 | — |
| SAM object expressions | 33 | — |
| trailing lambdas | 279 | — |
| `when` expressions | 18 | — |
| if/else chains | 106 | — |
| scope-fn calls (`let/run/apply/also/with`) | 13 | 0.9% of functions |
| property accessors | 31 | 1.4% of properties |

The structural shape is faithful. The idiom shape is not: **zero** `data class` results from 158 class conversions, even though OkHttp 4.x uses `data class` heavily for immutable configuration types (e.g. `CipherSuite`, `ConnectionSpec`, `Protocol`). Scope-function adoption is effectively nil. 66 companion objects vs 8 object singletons says j2k reliably folds `static` members into `companion`, but does not recognize the pattern "class with only static members and a private constructor → `object`."

### Java-isms

| metric | total | rate |
|---|---:|---:|
| `!!` assertions | 159 | 4.81 per kloc |
| `java.util.*` refs | 26 | — |
| `@Synchronized` annotations | 72 | — |
| if-not-null guards | 170 | — |
| C-style `for (.indices)` loops | 11 | — |
| explicit getter/setter pairs | 3 | — |

The `!!` density is load-bearing: each is a potential NPE that didn't exist in the Java source (all fields were reference types, j2k inferred them as nullable and inserted `!!` at dereferences it couldn't prove non-null). 170 if-not-null guards are the mirror image — both are j2k's way of shipping past nullability inference it can't close.

### Comparative against OkHttp 4.12.0

- Matched pairs: 95
- Unmatched (converted only): 9
- Unmatched (reference only): 27
- Mean similarity: **0.6064**
- Median: 0.6154
- P90: 0.8000

27 reference-only files reflect maintainer refactors between 3.x and 4.x — new files that didn't exist in 3.x. 9 converted-only files are mostly nested classes the maintainers inlined. Neither counts as "j2k got it wrong"; both are noise floor.

**Top 5 most-divergent files**:

| file | similarity | cause |
|---|---:|---|
| `okhttp3/internal/Util.kt` | 0.176 | 4.x rewrote Util into file-level extension functions on stdlib types |
| `okhttp3/internal/platform/AndroidPlatform.kt` | 0.216 | 4.x split Android code into 3 files by API level |
| `okhttp3/OkHttpClient.kt` | 0.221 | builder DSL entirely rewritten as `Builder` data class with `copy()` semantics |
| `okhttp3/internal/platform/Android10Platform.kt` | 0.244 | new file in 4.x, only fuzzy-matched |
| `okhttp3/internal/connection/ExchangeFinder.kt` | 0.290 | maintainers split `StreamAllocation` into `ExchangeFinder` + `Exchange` |

None of the top-5 divergences are j2k bugs — they're maintainer refactors that happened between 3.x and 4.x. The signature-token similarity metric is doing the right thing: flagging shape differences without pretending j2k caused them.

### Compile check: pre- and post-post-processor

| | Pass | Fail | Rate |
|---|---:|---:|---:|
| Raw j2k output | 17 | 87 | **16.3%** |
| After `evaluator postprocess` | 25 | 79 | **24.0%** |
| **Δ** | **+8** | **−8** | **+7.7 pp** |

Files fixed by the post-processor: `Connection.kt`, `Interceptor.kt`, `Internal.kt`, `InternalCache.kt`, `MediaType.kt`, `Route.kt`, `WebSocket.kt`, `WebSocketListener.kt` — all files where the *only* compile error was a missing JSR-305 import.

JSR-305-specific errors before vs after: **77 → 0**. Unresolved-reference errors total: **216 → 157**.

### Remaining compile failures (79 files, 323 error messages)

Categorized from the post-fix `build/report.json`:

| category | messages | dominant cause |
|---|---:|---|
| unresolved reference | 157 | j2k keeps `IOException`, `Closeable`, `File`, `TreeSet`, `ByteString`, `Objects`, `SocketFactory`, `ProxySelector`, internal classes (`InternalCache`, `CertificateChainCleaner`) — all referenced without imports |
| nullability type mismatch | 69 | j2k infers `String?` where the callee accepts `String`; especially around `Headers`, `Cookie`, `Request` value types |
| annotation-argument-not-const | 23 | `@Throws(IOException::class)` where `IOException` is unresolved → const resolution fails downstream |
| missing abstract implementations | 12 | `OkHttpClient`, `RealCall`, `FormBody`, `MultipartBody` — Java-side default methods on interfaces (`Call.clone`) that j2k emits as abstract on the Kotlin side |
| nullable-receiver | 12 | `foo.bar()` where `foo: T?` — j2k didn't insert `?.` or `!!` |
| visibility leak | 7 | `internal` types exposed through `public` signatures |
| property-as-function | 6 | `isHttps` was a `getIsHttps()` → property, but callers invoke it as `isHttps()` |
| interface-with-parens | 3 | H5b — `Authenticator.kt`, `Dns.kt`, `EventListener.kt` |
| smart-cast impossible | 2 | property with custom getter |
| overload resolution | 2 | ambiguity between kotlin stdlib and Java stdlib overloads |
| other | 30 | misc |

## Proposed upstream fixes

Two concrete, mechanical j2k bugs whose fixes would lift the compile rate materially. Sketched in prose; both are patches I would be willing to prototype against `intellij-community` if the discussion proceeds.

### Fix 1 — Restore JSR-305 (and general annotation-class) imports

**Shipped in this submission** as `evaluator/src/main/kotlin/…/postprocess/AnnotationImportFixer.kt`. The post-processor scans `KtAnnotationEntry` short-names in the file, compares to `importDirectives`, and splices `import javax.annotation.<Name>` lines into the source text. 77 → 0 JSR-305 errors; +8 files compile.

**Upstream**: the right place is `NewJ2kPostProcessor`'s import-resolution phase. The post-processor already walks references and normalizes types; extending the walk to `KtAnnotationEntry.typeReference` and feeding short-names through the same resolver that handles type references would cover this class of bug for any annotation package, not just JSR-305. The current behavior suggests annotations are being filtered out of the ref walk — likely an artifact of the type-vs-annotation distinction in the Java PSI.

### Fix 2 — Don't emit `()` on Java-interface supertypes

**Not shipped** — 3 files (`Authenticator.kt`, `Dns.kt`, `EventListener.kt`). The converter emits `class Foo : Authenticator()` where `Authenticator` is a Java interface. Kotlin 2.2 rejects this with `Interface 'interface Authenticator' does not have constructors`.

**Upstream sketch**: in the supertype-list building phase, when the Java side is `class Foo implements Bar`, check whether `Bar` is a `PsiClass` with `isInterface()` true before generating a `KtSuperTypeCallEntry` (with parens). If it's an interface, generate `KtSuperTypeEntry` (no parens). The check already exists for `extends` clauses; it just isn't applied to `implements`.

Both bugs are the same family: the converter *has* the necessary information (the Java PSI tells it `Authenticator` is an interface, and tells it `@Nullable` is an annotation class from `javax.annotation`), but that information is dropped during emission. Hence "mechanical fix" rather than "architectural."

## Caveats and scope

- **K2 only.** IC-2025.2.6 does not register NJ2K (K1_NEW) out of the box. A K1 vs K2 side-by-side would require bundling a K1-mode IDE distribution; deferred as out-of-scope for a 9-commit take-home.
- **One fixer shipped, one sketched.** Commit 7 fixes the dominant JSR-305 import gap. The interface-parens gap is documented but not rewritten — 3 files of signal doesn't justify a second PSI rewriter in this codebase.
- **No runtime tests.** "Compiles" ≠ "behaves correctly." This submission doesn't execute OkHttp's test suite against the converted code. Compilation rate is a lower bound on correctness, not a guarantee of it.
- **Evaluator on 2.2 / K2-compile path.** The compile check uses `K2JVMCompiler` from `kotlin-compiler-embeddable`. Some strictness differences vs the IDE's K2 frontend are possible — particularly around nullability inference of Java signatures. Differences, if any, would apply uniformly across raw and post-fixed runs; the delta is still meaningful.

## Reproduction

```bash
make fetch resolve-classpath convert postprocess evaluate
# → build/report.json, build/report.md
make evaluate-raw
# → build/raw-reports/report.{json,md}  (pre-postprocess baseline)
make eval-edge-cases
# → build/edge-reports/report.{json,md}
```

CI runs the same chain on `ubuntu-latest` and uploads all three report trees plus `build/converted/` and `build/diagnostics.json` as artifacts; `build/report.md` is written to `$GITHUB_STEP_SUMMARY`.
