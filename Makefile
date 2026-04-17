SHELL := /bin/bash
.DEFAULT_GOAL := help

.PHONY: help fetch resolve-classpath convert evaluate all eval-edge-cases ci-local clean

help:
	@echo "j2k-eval-pipeline targets:"
	@echo "  fetch             fetch OkHttp 3.14.9 (target) + 4.12.0 (reference)"
	@echo "  resolve-classpath resolve OkHttp 3.14.9 runtime classpath -> build/classpath.txt"
	@echo "  convert           run j2k-runner on build/input -> build/converted"
	@echo "  evaluate          run evaluator on build/converted -> build/report.{json,md}"
	@echo "  all               fetch + resolve-classpath + convert + evaluate"
	@echo "  eval-edge-cases   run pipeline on the custom edge-case dataset (commit 6)"
	@echo "  ci-local          reproduce the full CI pipeline locally (commit 7+)"
	@echo "  clean             remove build outputs and fetched sources"

fetch:
	scripts/fetch-okhttp.sh

resolve-classpath:
	scripts/resolve-classpath.sh

convert:
	scripts/run-j2k.sh

evaluate:
	./gradlew :evaluator:installDist -q --console=plain
	evaluator/build/install/evaluator/bin/evaluator analyze \
	    --input build/converted \
	    --reference build/reference \
	    --classpath build/classpath.txt \
	    --out-dir build

all: fetch resolve-classpath convert evaluate

eval-edge-cases:
	@echo "not yet — wired in commit 6"; exit 1

ci-local:
	@echo "not yet — wired in commit 7"; exit 1

clean:
	rm -rf build **/build .gradle/.tmp converted .tmp
