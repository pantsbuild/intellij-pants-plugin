#!/usr/bin/env bash
set -exo pipefail
source scripts/prepare-ci-environment.sh
mkdir -p .cache/intellij/$FULL_IJ_BUILD_NUMBER

# Checking .git because cache entry '.cache/pants' in .travis.yml
# will alawys create directory .cache/pants
if [ ! -d .cache/pants/.git ]; then
  echo "Getting latest Pants..."
  pushd .cache
  git clone https://github.com/wisechengyi/pants
  echo "Bootstrapping Pants..."
  pushd pants

  git checkout yic/fix_setuptools
  ./pants help goals
  popd
  popd
fi

if [ ! -d .cache/pants-host/.git ]; then
    pushd .cache
    git clone https://github.com/scalameta/pants -b 1.26.x-intellij-plugin pants-host
    pushd pants-host
    git checkout 70bcd0aacb3dd3aacf61231c9db54592597776a8
    ./pants help goals
    popd
    popd
fi

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
  curl -LSso $IJ_TAR_NAME "https://download.jetbrains.com/idea/$IJ_TAR_NAME"
  {
    echo "$IJ_SHA $IJ_TAR_NAME" | sha256sum -c - &&
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

  if [ "$SCALA_PLUGIN_CHANNEL" == "stable" ]; then
      curl -LSso Scala.zip "https://plugins.jetbrains.com/plugin/download?pluginId=$SCALA_PLUGIN_ID&version=$SCALA_PLUGIN_VERSION"
  else
      curl -LSso Scala.zip "https://plugins.jetbrains.com/plugin/download?pluginId=$SCALA_PLUGIN_ID&version=$SCALA_PLUGIN_VERSION&channel=$SCALA_PLUGIN_CHANNEL"
  fi
  curl -LSso python.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$PYTHON_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"

  sha256sum --strict -c ../scripts/checksums.txt

  unzip -o -q Scala.zip
  rm -f Scala.zip

  unzip -o -q python.zip
  rm -f python.zip

  popd
  mv plugins ".cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
fi




(
    cd "$CWD/testData"
    curl -LSso "external-system-test-api.zip" "$EXTERNAL_SYSTEM_TEST_IMPL_JAR_URL"
    echo "$EXTERNAL_SYSTEM_TEST_IMPL_JAR_SHA external-system-test-api.zip" | sha256sum -c - && unzip -o "external-system-test-api.zip"
)
