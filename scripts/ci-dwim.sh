#!/usr/bin/env bash

set -euo pipefail

source ./scripts/prepare-ci-environment.sh

./scripts/setup-ci-environment.sh

./scripts/run-tests-ci.sh $@
