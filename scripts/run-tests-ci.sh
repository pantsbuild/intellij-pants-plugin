#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

# For tests/com/twitter/intellij/pants/integration/WholeRepoIntegrationTest.java
rm -rf .cache/dummy_repo
cp -r testData/dummy_repo .cache/

# Remove IntelliJ index cache.
rm -rf .cache/intellij/*/idea-dist/system/caches/

args="${TASKS:-test} tests:${TEST_SET:-:} $(append_intellij_jvm_options test-junit) ${ADDITIONAL_ARGS:-$@}"

echo "Running ./pants $args"
.cache/pants-host/pants $args
