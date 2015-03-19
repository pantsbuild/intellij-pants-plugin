#!/usr/bin/env bash

set -e

CWD=$(pwd)

if [[ -z "$IJ_BUILD_NUMBER" || -z "$IJ_VERSION" ]] ; then
  echo "Must set IJ_BUILD_NUMBER and IJ_VERSION in the environment."
  exit 1
fi

# we will use Community ids to download plugins.
SCALA_PLUGIN_ID="org.intellij.scala"
PYTHON_PLUGIN_ID="PythonCore"
FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"

IJ_BUILD="IC-${IJ_VERSION}"
if [[ $IJ_ULTIMATE == "true" ]]; then
  IJ_BUILD="IU-${IJ_VERSION}"
fi

mkdir -p .cache/intellij

if [ ! -d .cache/intellij/idea-dist ]; then
  echo "Loading $IJ_BUILD..."
  curl -L -o idea${IJ_BUILD}.tar.gz http://download-cf.jetbrains.com/idea/idea${IJ_BUILD}.tar.gz
  tar zxf idea${IJ_BUILD}.tar.gz
  rm -rf idea${IJ_BUILD}.tar.gz
  UNPACKED_IDEA=$(find . -name 'idea-I*' | head -n 1)
  mv "$UNPACKED_IDEA" ".cache/intellij/idea-dist"
fi

if [ ! -d .cache/intellij/plugins ]; then
  echo "Loading $SCALA_PLUGIN_ID and $PYTHON_PLUGIN_ID for $FULL_IJ_BUILD_NUMBER..."
  mkdir -p plugins
  pushd plugins

  URL="https://plugins.jetbrains.com/pluginManager/?action=download&id=${SCALA_PLUGIN_ID}&build=${FULL_IJ_BUILD_NUMBER}"
  echo "Downloading ${URL} to" `pwd`
  curl -L -o Scala.zip ${URL}
  unzip Scala.zip
  rm -rf Scala.zip

  URL="https://plugins.jetbrains.com/pluginManager/?action=download&id=${PYTHON_PLUGIN_ID}&build=${FULL_IJ_BUILD_NUMBER}"
  echo "Downloading ${URL} to" `pwd`
  curl -L -o python.zip ${URL}
  unzip python.zip
  rm -rf python.zip

  popd
  mv plugins ".cache/intellij/plugins"
fi

if [ ! -d .cache/intellij/pants ]; then
  echo "Getting latest Pants..."
  pushd .cache/intellij/
  git clone https://github.com/pantsbuild/pants
  echo "Bootstrapping Pants and Ivy..."
  pushd pants
  ./pants goal resolve examples/src/scala/:: examples/src/java/::
  popd
  popd
fi

echo "Bootstrapping Pants..."
./pants goal goals
