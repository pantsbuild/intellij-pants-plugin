#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

# we will use Community ids to download plugins.
SCALA_PLUGIN_ID="org.intellij.scala"
PYTHON_PLUGIN_ID="PythonCore"
if [[ $IJ_ULTIMATE == "true" ]]; then
  PYTHON_PLUGIN_ID="Pythonid"
fi

mkdir -p .cache/intellij/$FULL_IJ_BUILD_NUMBER

if [ ! -d .cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist ]; then
  echo "Loading $IJ_BUILD..."
  wget http://download.jetbrains.com/idea/idea${IJ_BUILD}.tar.gz
  tar zxf idea${IJ_BUILD}.tar.gz
  rm -rf idea${IJ_BUILD}.tar.gz
  UNPACKED_IDEA=$(find . -name 'idea-I*' | head -n 1)
  mv "$UNPACKED_IDEA" ".cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
fi

#if [ ! -d .cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins ]; then
  echo "Loading $SCALA_PLUGIN_ID and $PYTHON_PLUGIN_ID for $FULL_IJ_BUILD_NUMBER..."
  mkdir -p plugins
  pushd plugins

  wget -O Scala.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$SCALA_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  unzip Scala.zip
  rm -rf Scala.zip
  wget -O python.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$PYTHON_PLUGIN_ID&build=$FULL_IJ_BUILD_NUMBER"
  unzip python.zip
  rm -rf python.zip

  popd
  mv plugins ".cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
#fi

if [ ! -d .cache/pants ]; then
  echo "Getting latest Pants..."
  pushd .cache
  git clone https://github.com/pantsbuild/pants
  echo "Bootstrapping Pants and Ivy..."
  pushd pants
  ./pants resolve examples/src/scala/:: examples/src/java/:: BUILD:
  popd
  popd
fi

if [ ! -d .cache/jdk-libs ]; then
  echo "Copying JDK libs..."
  mkdir -p .cache/jdk-libs
  cp "$JAVA_HOME/lib/sa-jdi.jar" "$JAVA_HOME/lib/tools.jar" .cache/jdk-libs
fi

echo "Bootstrapping Pants..."
./pants goals
