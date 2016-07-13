#!/usr/bin/env bash

set -e

# Important: to update the build number of intellij, you need to update the following hashes:
# Intellij tarball for Community and Ultimate Edition
# Scala plugin
# Python plugin for Community and Ultimate Edition

export CWD=$(pwd)
export IJ_VERSION="162.1121.10"
export IJ_BUILD_NUMBER="162.1121.10"

get_md5(){
  if [[ $OSTYPE == *"darwin"* ]]; then
    echo  $(md5 $1| awk '{print $NF}')
  else
    echo  $(md5sum $1| awk '{print $1}')
  fi
}

if [[ $IJ_ULTIMATE == "true" ]]; then
  export IJ_BUILD="IU-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IU-${IJ_BUILD_NUMBER}"
  export EXPECTED_IJ_MD5="e03c6d151c498891a47cd38b54b100b8"
  export PYTHON_PLUGIN_ID="Pythonid"
  export PYTHON_PLUGIN_MD5="553a68036a761a95d2b57637613e39df"
else
  export IJ_BUILD="IC-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"
  export EXPECTED_IJ_MD5="38f0387395647a874e0e103b48dd969d"
  export PYTHON_PLUGIN_ID="PythonCore"
  export PYTHON_PLUGIN_MD5="c4e885fea86f5ad5da17a40804a3cafe"
fi

# we will use Community ids to download plugins.
export SCALA_PLUGIN_ID="org.intellij.scala"
export SCALA_PLUGIN_MD5="8aad157a73234877dcbd1941eee705ea" #2016.2.0

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"

export IDEA_TEST_HOME="$CWD/.pants.d/intellij/plugins-sandbox/test"

append_intellij_jvm_options() {
  scope=$1
  cmd=""

  INTELLIJ_JVM_OPTIONS=(
    "-Didea.load.plugins.id=org.intellij.scala,PythonCore,JUnit,com.intellij.plugins.pants"
    "-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME"
    "-Didea.home.path=$IDEA_TEST_HOME"
    "-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java"
    "-Dpants.jps.plugin.classpath=$CWD/jps-plugin:$INTELLIJ_HOME/lib/rt/jps-plugin-system.jar:$CWD/jps-plugin/lib/gson-2.3.1.jar"
    #EAP build does not know its own build number, thus failing to tell plugin compatibility.
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

