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
export IJ_VERSION="2017.2.4"
export IJ_BUILD_NUMBER="172.4155.36"

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
  export EXPECTED_IJ_MD5="101443bad0a5e2ed63f9535a7d51fc21"
  export PYTHON_PLUGIN_ID="Pythonid"
  export PYTHON_PLUGIN_MD5="0e50933c204313aaa2a062e9b19b33ac"
else
  export IJ_BUILD="IC-${IJ_VERSION}"
  export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"
  export EXPECTED_IJ_MD5="d5c7a493b83f16ea817a8aa91d9420ae"
  export PYTHON_PLUGIN_ID="PythonCore"
  export PYTHON_PLUGIN_MD5="2eeffe7aa2f80f125e8804bbe947753d"
fi

# we will use Community ids to download plugins.
export SCALA_PLUGIN_ID="org.intellij.scala"
export SCALA_PLUGIN_MD5="00def7257eb1b8b5800676dbe96e5c27" # 2017.2.3

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export DUMMY_REPO_HOME="$CWD/.cache/dummy_repo"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"

append_intellij_jvm_options() {
  scope=$1
  cmd=""

  INTELLIJ_JVM_OPTIONS=(
    "-Didea.load.plugins.id=com.intellij.properties,org.intellij.groovy,org.jetbrains.plugins.gradle,org.intellij.scala,PythonCore,JUnit,com.intellij.plugins.pants"
    "-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME"
    "-Didea.home.path=$INTELLIJ_HOME"
    "-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java"
    "-Dpants.jps.plugin.classpath=$CWD/jps-plugin:$INTELLIJ_HOME/lib/rt/jps-plugin-system.jar"
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
