#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

if [[ $IJ_ULTIMATE == "true" ]]; then
  export TEST_SET='jvm-integration'
fi

# For tests/com/twitter/intellij/pants/integration/WholeRepoIntegrationTest.java
rm -rf .cache/dummy_repo
cp -r testData/dummy_repo .cache/

pushd .cache
pushd pants
if [ -z ${PANTS_SHA+x} ]; then
  echo "Pulling the latest master..."
  git pull
else
  echo "Using $PANTS_SHA..."
  git reset --hard $PANTS_SHA
fi
popd
popd

args="${TEST_TARGET:-test} tests:${TEST_SET:-:} $(append_intellij_jvm_options test-junit) ${ADDITIONAL_ARGS:-$@}"

echo "Running ./pants $args"
./pants $args
