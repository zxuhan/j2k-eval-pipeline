# j2k-eval-pipeline

A GitHub Actions pipeline that runs IntelliJ's K2 Java-to-Kotlin converter headlessly over a real-world Java library, then scores the output with a Kotlin-PSI-based evaluator. Submitted as a JetBrains take-home.

**Target**: [OkHttp 3.14.9](https://github.com/square/okhttp/tree/parent-3.14.9) — the last pre-Kotlin-migration tagged release of Square's HTTP client. Picked because OkHttp 4.x+ is the maintainers' own hand-migrated Kotlin version of the same code, so *"how close did j2k get to the humans' Kotlin"* is an answerable question rather than a hand-wavy idiom score.

## Top-line numbers

From the latest CI run, sourced from committed JSON artifacts under `build/`:

| metric | value |
|---|---|
| Files converted | 104 / 104 |
| Code lines | 30,208 |
| Comparative similarity vs OkHttp 4.12.0 | mean 0.61, p90 0.80 |
| Compile rate, raw j2k output | **17 / 104 (16.3%)** |
| Compile rate, after one post-processor pass | **25 / 104 (24.0%)** |
| JSR-305 unresolved-ref errors fixed | **77 → 0** |
| `!!` assertions in converted output | 159 (4.81 per kloc) |

Full breakdown in [`EVALUATION_REPORT.md`](EVALUATION_REPORT.md). Per-edge-case hypothesis verdicts in [`EDGE_CASES_REPORT.md`](EDGE_CASES_REPORT.md).

## Quick start

Requires JDK 17, `git`, `curl`.

```bash
git clone git@github.com:zxuhan/j2k-eval-pipeline.git
cd j2k-eval-pipeline
make fetch resolve-classpath convert postprocess evaluate
# → build/report.{json,md}

make eval-edge-cases
# → build/edge-reports/report.{json,md}
```

Or `make ci-local` to reproduce the full CI chain end-to-end, including the step-summary artifact.

## What runs where

```
fetch → resolve-classpath → convert → postprocess → evaluate
  │            │               │           │             │
  │            │               │           │             └─ PSI analyzers + K2JVMCompiler
  │            │               │           └─ restore dropped javax.annotation imports
  │            │               └─ headless IDEA plugin → K2 J2K extension point
  │            └─ Gradle sidecar resolves OkHttp 3.14.9's runtime classpath
  └─ git-clone OkHttp 3.14.9 (target) + 4.12.0 (reference oracle)
```

Each stage is one Makefile target and one shell script / Gradle module; no hidden glue.

## Repo layout

```
j2k-runner/   — IntelliJ plugin (ApplicationStarter) that drives J2kConverterExtension headlessly
evaluator/   — Kotlin CLI: analyze + postprocess subcommands, PSI-based analyzers + reporters
scripts/     — fetch-okhttp.sh, resolve-classpath.sh, run-j2k.sh
edge-cases/  — 20 hand-written Java stress tests across 8 categories, each with a // HYPOTHESIS:
.github/     — single-job linear CI, uploads converted tree + diagnostics + reports
```

The evaluator has five analyzers:

- **Structural** — class / function / property / interface counts, nesting depth.
- **Idiom** — `data class`, object singletons, scope functions, trailing lambdas, `when` vs if-chains.
- **Java-ism** — `!!` density, `java.util.*` refs, `@Synchronized`, if-not-null guards, C-style indexed for-loops.
- **Comparative** — signature-token Levenshtein against a reference tree (OkHttp 4.x), per-file + aggregate stats.
- **Compile** — in-process `K2JVMCompiler` via `kotlin-compiler-embeddable`, full classpath, per-file diagnostics.

Plus one post-processor (`AnnotationImportFixer`) that restores the dominant dropped-import class and lifts the compile rate by ~8 percentage points.

## Design decisions worth calling out

- **Run j2k via `ApplicationStarter` + the `j2kConverterExtension` extension point** — the same path the IDE takes when a user clicks "Convert Java File to Kotlin." Alternatives considered: a standalone `kotlin-nj2k` library (not published as supported), `idea.sh inspect` (no j2k inspection exists), AWT-robot UI automation (brittle and useless in CI). At IC-2025.2.6 only the K2 kind (`J2kConverterExtension.Kind.K2`) is registered — NJ2K (K1_NEW) isn't available out of the box, so this submission is K2-only. Prior art: Meta Engineering's December 2024 post on batch j2k, validated in dialogue with JetBrains' Ilya Kirillov.
- **Target scope: `okhttp/src/main/java` only.** Tests, samples, and Android-specific code would add file count without adding signal and blow past a 15-minute CI budget. 104 files of core HTTP client code is enough for real statistics.
- **Evaluator uses real PSI, not regex.** A reviewer who works on j2k will notice "idiomatic Kotlin analysis" implemented as grep-over-source immediately. Comparative uses a flattened signature-token sequence + normalized Levenshtein rather than Zhang-Shasha tree edit distance — O(n·m) with no dependency, and class/function-shape divergence is what matters for j2k evaluation (bodies nearly always translate fine; the divergence is at the declaration level).
- **Hand-transcribed classpath, not the target's own build.** OkHttp 3.14.9's Gradle 5.x build doesn't run on a modern JDK; rather than pin an ancient Gradle just to resolve jars, `scripts/classpath-resolver/build.gradle.kts` declares the same coordinates (Okio 1.17.2, JSR-305 3.0.2, BouncyCastle 1.64, Conscrypt 2.2.1, ...) and lets a current Gradle resolve them.
- **`CompileAnalyzer` invokes `K2JVMCompiler` in-process.** We already depend on `kotlin-compiler-embeddable` for PSI — shelling out to `kotlinc` would mean resolving a second Kotlin distribution and parsing textual diagnostics instead of a structured `MessageCollector` callback.

See `EVALUATION_REPORT.md` for the full set of decisions and the rationale for each.

## CI

One linear job on `ubuntu-latest`, budget ~15 minutes (latest run: 4m33s). Caches the IJ Platform install and Gradle state. Uploads converted tree, diagnostics JSON, and reports as artifacts. Writes `build/report.md` into `$GITHUB_STEP_SUMMARY` so the result is visible on the Actions run page without downloading anything.

## Caveats

- **K2 only** — see above. A K1 vs K2 side-by-side would need a separate K1-mode IDE distribution.
- **"Compiles" ≠ "works."** This submission does not run OkHttp's test suite against the converted code. Compile rate is a lower bound on correctness, not a guarantee.
- **One fixer shipped, one sketched.** The JSR-305 import gap is fixed in the pipeline. The interface-with-parens bug (3 files) is documented with a proposed upstream patch, not rewritten locally.
