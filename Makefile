SHELL := /bin/bash
.DEFAULT_GOAL := help

.PHONY: help fetch resolve-classpath convert postprocess evaluate evaluate-raw all eval-edge-cases ci-local clean

help:
	@echo "j2k-eval-pipeline targets:"
	@echo "  fetch             fetch OkHttp 3.14.9 (target) + 4.12.0 (reference)"
	@echo "  resolve-classpath resolve OkHttp 3.14.9 runtime classpath -> build/classpath.txt"
	@echo "  convert           run j2k-runner on build/input -> build/converted"
	@echo "  postprocess       inject missing imports -> build/postprocessed"
	@echo "  evaluate          run evaluator on build/postprocessed -> build/report.{json,md}"
	@echo "  all               fetch + resolve-classpath + convert + postprocess + evaluate"
	@echo "  eval-edge-cases   run pipeline on the custom edge-case dataset"
	@echo "  ci-local          reproduce the full CI pipeline locally (commit 7+)"
	@echo "  clean             remove build outputs and fetched sources"

fetch:
	scripts/fetch-okhttp.sh

resolve-classpath:
	scripts/resolve-classpath.sh

convert:
	scripts/run-j2k.sh

postprocess:
	./gradlew :evaluator:installDist -q --console=plain
	evaluator/build/install/evaluator/bin/evaluator postprocess \
	    --input build/converted \
	    --output build/postprocessed

evaluate:
	./gradlew :evaluator:installDist -q --console=plain
	evaluator/build/install/evaluator/bin/evaluator analyze \
	    --input build/postprocessed \
	    --reference build/reference \
	    --classpath build/classpath.txt \
	    --out-dir build

evaluate-raw:
	./gradlew :evaluator:installDist -q --console=plain
	evaluator/build/install/evaluator/bin/evaluator analyze \
	    --input build/converted \
	    --reference build/reference \
	    --classpath build/classpath.txt \
	    --out-dir build/raw-reports

all: fetch resolve-classpath convert postprocess evaluate

eval-edge-cases:
	scripts/run-j2k.sh edge-cases build/edge-converted build/edge-diagnostics.json
	./gradlew :evaluator:installDist -q --console=plain
	evaluator/build/install/evaluator/bin/evaluator analyze \
	    --input build/edge-converted \
	    --out-dir build/edge-reports

ci-local:
	@echo "not yet — wired in commit 7"; exit 1

clean:
	rm -rf build **/build .gradle/.tmp converted .tmp
