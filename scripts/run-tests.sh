#!/usr/bin/env bash

CWD=$(pwd)

./pants test.junit \
  --no-suppress-output \
  --jvm-options="-Didea.load.plugins.id=com.intellij.plugins.pants" \
  --jvm-options="-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME" \
  --jvm-options="-Didea.home.path=$CWD/.pants.d/intellij/plugins-sandbox/test" \
  --jvm-options="-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java" \
  --jvm-options="-Dpants.jps.plugin.classpath=$CWD/.pants.d/resources/prepare/jps-plugin.services" \
  --jvm-options="-Dpants.compiler.enabled=${USE_PANTS_TO_COMPILE:-true}" \
  --jvm-options="-Dpants.compiler.isolated.strategy=${USE_ISOLATED_STRATEGY:-false}" \
  tests:${TEST_SET:-all} \
  $@
