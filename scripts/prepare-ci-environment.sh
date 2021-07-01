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
export IJ_VERSION="2021.2"
export IJ_BUILD_NUMBER="212.4416.7"
export IJ_SHA="4fc30beaf5bb622a6142fc56316e8bcc82ef7e772ca02bf42571bc8bbd5d8569"

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
  export IJ_BUILD="IU-${IJ_BUILD_NUMBER}"
  export FULL_IJ_BUILD_NUMBER="IU-${IJ_BUILD_NUMBER}"
  export PYTHON_PLUGIN_ID="Pythonid"
else
  export IJ_BUILD="IC-${IJ_BUILD_NUMBER}"
  export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"
  export PYTHON_PLUGIN_ID="PythonCore"
fi

# we will use Community ids to download plugins.
export SCALA_PLUGIN_ID="org.intellij.scala"
export SCALA_PLUGIN_VERSION="2021.2.10"
export SCALA_PLUGIN_CHANNEL="stable"

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export DUMMY_REPO_HOME="$CWD/.cache/dummy_repo"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"

export EXTERNAL_SYSTEM_TEST_IMPL_JAR_URL=https://github.com/lukaszwawrzyk/intellij-pants-plugin/releases/download/external-system-test-api-v2021.2/external-system-test-api.zip
export EXTERNAL_SYSTEM_TEST_IMPL_JAR_SHA=700069145cf471b0e05f82c432c71693aab1504b13ff995a3049b379566a4983

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
