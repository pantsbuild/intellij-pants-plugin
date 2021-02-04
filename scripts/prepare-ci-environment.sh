#!/usr/bin/env bash
# Option not to exit terminal while iterating.
exit_on_error="${EXIT_ON_ERROR:-1}"
if [[ $exit_on_error -ne 0 ]]; then
  set -e
fi
# Important: to update the build number of intellij, you need to update the following hashes:
# Intellij tarball for Community and Ultimate Edition
# Scala plugin
# Python plugin for Community and Ultimate Edition

export CWD=$(pwd)
# Normally, IJ_VERSION is of the form YEAR.x[.y[.z]]
# But for EAPs, set IJ_VERSION to the same as IJ_BUILD_NUMBER
export IJ_VERSION="2020.3"
export IJ_BUILD_NUMBER="203.5981.165"
export IJ_SHA="c6f78b72cf7b82619685651ae8517c3faf983dc558c4d4f4c171801ab8d43674"

# tests run from within pants repository must use java 8
export PANTS_TEST_JUNIT_STRICT_JVM_VERSION=true

# This is for bootstrapping Pants, since this repo does not do Pants intensive operations,
# we can optimize for build time.
# https://github.com/pantsbuild/pants/blob/16bd8fffb6db89779f5862604a0fe8745c8e50c4/build-support/bin/native/bootstrap_code.sh#L27
export MODE="debug"

get_md5(){
  if [[ $OSTYPE == *"darwin"* ]]; then
    echo  $(md5 $1| awk '{print $NF}')
  else
    echo  $(md5sum $1| awk '{print $1}')
  fi
}

if [[ "${IJ_ULTIMATE:-false}" == "true" ]]; then
  export IJ_BUILD="IU-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IU-${IJ_BUILD_NUMBER}"
  export PYTHON_PLUGIN_ID="Pythonid"
else
  export IJ_BUILD="IC-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"
  export PYTHON_PLUGIN_ID="PythonCore"
  export PYTHON_PLUGIN_SHA="aaaee0052620cd063ad76fa9cf4672c7b38c1db2d0007372fecd05d3d9ccb2f2"
fi

# we will use Community ids to download plugins.
export SCALA_PLUGIN_ID="org.intellij.scala"
export SCALA_PLUGIN_VERSION="2020.3.16"
export SCALA_PLUGIN_CHANNEL="stable"
export SCALA_PLUGIN_SHA="ce7fe52a7c3012073b3cdf4f4dff1c0b0003f9f1dbde69b430a288a7ee0dcad8"

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export DUMMY_REPO_HOME="$CWD/.cache/dummy_repo"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"

export EXTERNAL_SYSTEM_TEST_IMPL_JAR_URL=https://github.com/pantsbuild/intellij-pants-plugin/releases/download/external-system-test-api-v203.5981.41/external-system-test-api.zip
export EXTERNAL_SYSTEM_TEST_IMPL_JAR_SHA=4fa2e4a8a13382d16be11a649ae33f6a37efe2c693f04e02b7b703120b0966dd

append_intellij_jvm_options() {
  scope=$1

  plugins=(
    'com.intellij.properties'
    'JUnit'
    'org.intellij.groovy'
    'com.intellij.java'
    'org.intellij.intelliLang'
    'PythonCore'
    'com.intellij.modules.python-core-capable'
    'com.intellij.plugins.pants'
  )

  if [[ ${ENABLE_SCALA_PLUGIN:=true} == true ]]; then
    plugins+=('org.intellij.scala')
  fi

  load_plugins=$(printf "%s," "${plugins[@]}")

  INTELLIJ_JVM_OPTIONS=(
    "-Didea.load.plugins.id=${load_plugins}"
    "-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME"
    "-Didea.home.path=$INTELLIJ_HOME"
    # EAP build does not know its own build number, thus failing to tell plugin compatibility.
    "-Didea.plugins.compatible.build=$IJ_BUILD_NUMBER"
    # "-Dcompiler.process.debug.port=5006"
  )

  cmd=""
  for jvm_option in ${INTELLIJ_JVM_OPTIONS[@]}
  do
      cmd="$cmd --jvm-$scope-options=$jvm_option"
  done
  while read jvm_option; do
    cmd="$cmd --jvm-$scope-options=$jvm_option"
  done < "${2:-"$CWD/resources/idea64.vmoptions"}"

  echo $cmd
}

# on java 9+ these jars are not available
if [ -f "$JAVA_HOME/lib/tools.jar" ]; then
  JDK_JARS="$(printf "%s\n" 'sa-jdi.jar' 'tools.jar')"
fi
export JDK_JARS
