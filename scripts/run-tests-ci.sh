#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

if [[ $IJ_ULTIMATE == "true" ]]; then
  export TEST_SET='jvm-integration'
fi

rm -rf $IDEA_TEST_HOME
mkdir -p $IDEA_TEST_HOME

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

args="test tests:${TEST_SET:-:} $(append_intellij_jvm_options test-junit) ${ADDITIONAL_ARGS:-$@} \
--test-junit-test=com.twitter.intellij.pants.integration.OSSPantsExamplesMultiTargetsIntegrationTest#testHelloJavaAndScala \
--test-junit-test=com.twitter.intellij.pants.integration.OSSPantsFromScriptIntegrationTest#testScript \
--test-junit-test=com.twitter.intellij.pants.integration.OSSPantsScalaExamplesIntegrationTest#testError1"

echo "Running ./pants $args"
./pants $args
