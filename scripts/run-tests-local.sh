#!/usr/bin/env bash

source scripts/prepare-local-environment.sh

prepare_local_env

./scripts/run-tests.sh $@