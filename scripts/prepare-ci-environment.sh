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
export IJ_VERSION="2018.3.2"
export IJ_BUILD_NUMBER="183.4886.37"


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
  export EXPECTED_IJ_MD5="06dac318478f95335c142bebab586de5"
  export PYTHON_PLUGIN_ID="Pythonid"
  export PYTHON_PLUGIN_MD5="6fbabbbc179b022c1b5fd6e37b645b45"
else
  export IJ_BUILD="IC-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"
  export EXPECTED_IJ_MD5="7bf14969ca97b574ecb8eb3844d9816e"
  export PYTHON_PLUGIN_ID="PythonCore"
  export PYTHON_PLUGIN_MD5="eb942096057c2c5dece681254d8abaf1"
fi

# we will use Community ids to download plugins.
export SCALA_PLUGIN_ID="org.intellij.scala"
export SCALA_PLUGIN_MD5="11d0a202595bbbb49deff40712c13daf"

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export DUMMY_REPO_HOME="$CWD/.cache/dummy_repo"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"

append_intellij_jvm_options() {
  scope=$1
  cmd=""

  if [[ ${ENABLE_SCALA_PLUGIN:=true} == true ]]; then
    load_plugins="-Didea.load.plugins.id=com.intellij.properties,org.intellij.groovy,org.jetbrains.plugins.gradle,org.intellij.scala,PythonCore,JUnit,com.intellij.plugins.pants"
  else
    load_plugins="-Didea.load.plugins.id=com.intellij.properties,org.intellij.groovy,org.jetbrains.plugins.gradle,PythonCore,JUnit,com.intellij.plugins.pants"
  fi

  INTELLIJ_JVM_OPTIONS=(
    "-Didea.load.plugins.id=${load_plugins}"
    "-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME"
    "-Didea.home.path=$INTELLIJ_HOME"
    # EAP build does not know its own build number, thus failing to tell plugin compatibility.
    "-Didea.plugins.compatible.build=$IJ_BUILD_NUMBER"
    # "-Dcompiler.process.debug.port=5006"
  )
  for jvm_option in ${INTELLIJ_JVM_OPTIONS[@]}
  do
      cmd="$cmd --jvm-$scope-options=$jvm_option"
  done
  while read jvm_option; do
    cmd="$cmd --jvm-$scope-options=$jvm_option"
  done < "${2:-"$CWD/resources/idea64.vmoptions"}"

  echo $cmd
}

export JDK_JARS="$(printf "%s\n" 'sa-jdi.jar' 'tools.jar')"
