#!/usr/bin/env bash
source scripts/prepare-ci-environment.sh
mkdir -p .cache/intellij/$FULL_IJ_BUILD_NUMBER

verify_md5(){
  FILE=$1
  EXPECTED_MD5=$2
  ACTUAL_MD5=$(get_md5 $1)
  if [ $ACTUAL_MD5 != $EXPECTED_MD5 ];
  then
    echo "$1 md5 incorrect. Expected: $EXPECTED_MD5. Actual: $ACTUAL_MD5" >&2
#    exit 1
  fi
}

if [ -z $JAVA_HOME ]; then
  echo "Please set JAVA_HOME"
  exit 1
fi

if [ ! -f .cache/jdk-libs/sa-jdi.jar ] || [ ! -f .cache/jdk-libs/tools.jar ] ; then
  echo "Copying JDK libs..."
  mkdir -p .cache/jdk-libs
  cp "$JAVA_HOME/lib/sa-jdi.jar" "$JAVA_HOME/lib/tools.jar" .cache/jdk-libs
fi

if [ ! -d .cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist ]; then
  IJ_TAR_NAME=idea${IJ_BUILD}.tar.gz
  echo "Loading $IJ_BUILD..."
  wget -O $IJ_TAR_NAME "https://download.jetbrains.com/idea/$IJ_TAR_NAME"
  verify_md5 $IJ_TAR_NAME $EXPECTED_IJ_MD5
  {
    tar zxf $IJ_TAR_NAME &&
    UNPACKED_IDEA=$(find . -name 'idea-I*' | head -n 1) &&
    mv "$UNPACKED_IDEA" ".cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist" &&
    rm -f $IJ_TAR_NAME
  } || {
    echo "Failed to untar IntelliJ" >&2
    rm -rf .cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist
    exit 1
  }
fi

if [ ! -d .cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins ]; then
  echo "Loading $SCALA_PLUGIN_ID and $PYTHON_PLUGIN_ID for $FULL_IJ_BUILD_NUMBER..."
  mkdir -p plugins
  pushd plugins

  wget --no-check-certificate -O Scala.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$SCALA_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  verify_md5 Scala.zip $SCALA_PLUGIN_MD5
  unzip -q Scala.zip
  rm -f Scala.zip

  wget --no-check-certificate  -O python.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$PYTHON_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  verify_md5 python.zip $PYTHON_PLUGIN_MD5
  unzip -q python.zip
  rm -f python.zip

  popd
  mv plugins ".cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
fi

# Checking .git because cache entry '.cache/pants' in .travis.yml
# will alawys create directory .cache/pants
if [ ! -d .cache/pants/.git ]; then
  echo "Getting latest Pants..."
  pushd .cache
  git clone https://github.com/pantsbuild/pants
  echo "Bootstrapping Pants and Ivy..."
  pushd pants
  ./pants goals
  popd
  popd
fi
