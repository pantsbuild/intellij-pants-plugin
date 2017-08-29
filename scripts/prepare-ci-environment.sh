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
export IJ_VERSION="2017.2"
export IJ_BUILD_NUMBER="172.3317.76"

get_md5(){
  if [[ $OSTYPE == *"darwin"* ]]; then
    echo  $(md5 $1| awk '{print $NF}')
  else
    echo  $(md5sum $1| awk '{print $1}')
  fi
}

if [[ $IJ_ULTIMATE == "true" ]]; then
  export IJ_BUILD="IU-$IJ_VERSION"
  export FULL_IJ_BUILD_NUMBER="IU-$IJ_BUILD_NUMBER"
  export EXPECTED_IJ_MD5="fcba3c2dce4168f0f319a8cbf0ca49cc"
  export PYTHON_PLUGIN_ID="Pythonid"
  export PYTHON_PLUGIN_MD5="7f4471d738f06462590d4a20c147b39d"
else
  export IJ_BUILD="IC-$IJ_VERSION"
  export FULL_IJ_BUILD_NUMBER="IC-$IJ_BUILD_NUMBER"
  export EXPECTED_IJ_MD5="d87e4e0c536ba6046c7a10961d54416a"
  export PYTHON_PLUGIN_ID="PythonCore"
  export PYTHON_PLUGIN_MD5="6e78f632263957728bd07b7b5ad9e19f"
fi

# we will use Community ids to download plugins.
export SCALA_PLUGIN_ID="org.intellij.scala"
export SCALA_PLUGIN_MD5="c6b52267f30abd738c779f2a21d0fe3c" # 2017.2.6

export CACHE_LOCATION="$CWD/.cache"
export CACHED_IJ_DIR="$CACHE_LOCATION/intellij/$FULL_IJ_BUILD_NUMBER"

export INTELLIJ_HOME="$CACHED_IJ_DIR/idea-dist"
export INTELLIJ_PLUGINS_HOME="$CACHED_IJ_DIR/plugins"
export OSS_PANTS_HOME="$CACHE_LOCATION/pants"
export DUMMY_REPO_HOME="$CACHE_LOCATION/dummy_repo"
export JDK_LIBS_HOME="$CACHE_LOCATION/jdk-libs"

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
  done < "${2:-$CWD/resources/idea64.vmoptions}"

  echo $cmd
}
