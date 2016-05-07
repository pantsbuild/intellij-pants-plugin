#!/usr/bin/env bash
source scripts/prepare-ci-environment.sh
mkdir -p .cache/intellij/$FULL_IJ_BUILD_NUMBER

if [ ! -d .cache/jdk-libs ]; then
  echo "Copying JDK libs..."
  mkdir -p .cache/jdk-libs
  cp "$JAVA_HOME/lib/sa-jdi.jar" "$JAVA_HOME/lib/tools.jar" .cache/jdk-libs
fi

if [ ! -d .cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist ]; then
  IJ_TAR_NAME=idea${IJ_BUILD}.tar.gz
  echo "Loading $IJ_BUILD..."
  wget -O $IJ_TAR_NAME "http://download.jetbrains.com/idea/$IJ_TAR_NAME"
  if [ $(get_md5 $IJ_TAR_NAME) != $EXPECTED_IJ_MD5 ];
  then
    echo "IJ tar md5 incorrect" >&2
    exit 1
  fi
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

  wget -O Scala.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$SCALA_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  if [ $(get_md5 Scala.zip) != $SCALA_PLUGIN_MD5 ];
  then
    echo "Scala plugin md5 incorrect" >&2
    exit 1
  fi
  unzip Scala.zip
  rm -f Scala.zip

  wget -O python.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$PYTHON_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  if [ $(get_md5 python.zip) != $PYTHON_PLUGIN_MD5 ];
  then
    echo "Python plugin md5 incorrect" >&2
    exit 1
  fi
  unzip python.zip
  rm -f python.zip

  popd
  mv plugins ".cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
fi

if [ ! -d .cache/pants ]; then
  echo "Getting latest Pants..."
  pushd .cache
  git clone https://github.com/peiyuwang/pants
  echo "Bootstrapping Pants and Ivy..."
  pushd pants
  git checkout -b peiyu/better-ivy-error-message origin/peiyu/better-ivy-error-message
  git branch
  ./pants goals
  popd
  popd
fi
