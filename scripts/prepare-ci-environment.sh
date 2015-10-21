#!/usr/bin/env bash

set -e

export CWD=$(pwd)
export IJ_VERSION="14.1"
export IJ_BUILD_NUMBER="141.177"

export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"
export IJ_BUILD="IC-${IJ_VERSION}"
if [[ $IJ_ULTIMATE == "true" ]]; then
  export IJ_BUILD="IU-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IU-${IJ_BUILD_NUMBER}"
fi

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"

export IDEA_TEST_HOME="$CWD/.pants.d/intellij/plugins-sandbox/test"

append_intellij_jvm_options() {
  cmd=$1
  INTELLIJ_JVM_OPTIONS=(
    "-Didea.load.plugins.id=com.intellij.plugins.pants"
    "-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME"
    "-Didea.home.path=$IDEA_TEST_HOME"
    "-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java"
    "-Dpants.jps.plugin.classpath=$CWD/.pants.d/resources/prepare/jps-plugin.services"
    "-Dpants.compiler.enabled=${USE_PANTS_TO_COMPILE:-true}"
    "-Dpants.compiler.debuginfo=${USE_PANTS_COMPILE_DEBUG_INFO:-true}"
    "-Dpants.compiler.isolated.strategy=${USE_ISOLATED_STRATEGY:-true}"
  )
  for jvm_option in ${INTELLIJ_JVM_OPTIONS[@]}
  do
      cmd="$cmd --jvm-options=$jvm_option"
  done
  while read jvm_option; do
    cmd="$cmd --jvm-options=$jvm_option"
  done < "${2:-"$INTELLIJ_HOME/bin/idea64.vmoptions"}"

  echo $cmd
}