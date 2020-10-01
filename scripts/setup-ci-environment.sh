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

JDK_LIB_DIR="$JAVA_HOME/lib"

CACHE_JDK_LIB_DIR='.cache/jdk-libs'

mkdir -p "$CACHE_JDK_LIB_DIR"

for jar_file in $JDK_JARS; do
  src_jar="$JDK_LIB_DIR/$jar_file"
  dest_jar="$CACHE_JDK_LIB_DIR/$jar_file"
  if [ -f "$src_jar" ] && [ ! -f "$dest_jar" ]; then
    cp "$src_jar" "$dest_jar"
  fi
done

if [ ! -d .cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist ]; then
  IJ_TAR_NAME=idea${IJ_BUILD}.tar.gz
  echo "Loading $IJ_BUILD..."
  wget -q -O $IJ_TAR_NAME "https://download.jetbrains.com/idea/$IJ_TAR_NAME"
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

  # wget -q --no-check-certificate -O Scala.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$SCALA_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  wget -q --no-check-certificate -O Scala.zip "https://plugins.jetbrains.com/plugin/download?pluginId=$SCALA_PLUGIN_ID&version=$SCALA_PLUGIN_VERSION&channel=$SCALA_PLUGIN_CHANNEL"
  unzip -q Scala.zip
  rm -f Scala.zip

  wget -q --no-check-certificate  -O python.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$PYTHON_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
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
  git clone https://github.com/tpasternak/pants
  echo "Bootstrapping Pants..."
  pushd pants
  # Bootstrap Pants in the testing SHA so it won't cause
  # tests to time out during Pants run.
  if [ -z ${PANTS_SHA+x} ]; then
    echo "Using the latest master..."
  else
    echo "Using $PANTS_SHA..."
    git checkout -f $PANTS_SHA
  fi
  ./pants goals
  popd
  popd
fi
