#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

if [[ $IJ_ULTIMATE == "true" ]]; then
  export TEST_SET="integration"
fi

rm -rf $IDEA_TEST_HOME
mkdir -p $IDEA_TEST_HOME

./pants $(append_intellij_jvm_options "test.junit") tests:${TEST_SET:-all} $@