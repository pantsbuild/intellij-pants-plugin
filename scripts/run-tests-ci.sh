#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

prepare_ci_env

./scripts/run-tests.sh $@