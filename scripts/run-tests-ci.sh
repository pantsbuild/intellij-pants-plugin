#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

prepare_ci_env

if [[ $IJ_ULTIMATE == "true" ]]; then
  export TEST_SET="integration"
fi
./scripts/run-tests.sh $@