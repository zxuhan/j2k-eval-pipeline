SHELL := /bin/bash
.DEFAULT_GOAL := help

.PHONY: help fetch convert evaluate eval-edge-cases ci-local clean

help:
	@echo "j2k-eval-pipeline targets:"
	@echo "  fetch            fetch OkHttp 3.14.9 source tree (commit 3)"
	@echo "  convert          run j2k on the fetched sources (commit 3)"
	@echo "  evaluate         run evaluator on converted output (commit 4+)"
	@echo "  eval-edge-cases  run pipeline on the custom edge-case dataset (commit 6)"
	@echo "  ci-local         reproduce the full CI pipeline locally (commit 7+)"
	@echo "  clean            remove build outputs and fetched sources"

fetch:
	@echo "not yet — wired in commit 3"; exit 1

convert:
	@echo "not yet — wired in commit 3"; exit 1

evaluate:
	@echo "not yet — wired in commit 4"; exit 1

eval-edge-cases:
	@echo "not yet — wired in commit 6"; exit 1

ci-local:
	@echo "not yet — wired in commit 7"; exit 1

clean:
	rm -rf build **/build .gradle/.tmp converted .tmp
