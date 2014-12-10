#!/usr/bin/env bash

set -e

CWD=$(pwd)
IJ_VERSION="14"
IJ_BUILD="IC-139.223"

SCALA_PLUGIN_ID="org.intellij.scala"
PYTHON_CE_PLUGIN_ID="PythonCore"

mkdir -p .pants.d/intellij

if [ ! -d .pants.d/intellij/ideaIC ]; then
  echo "Loading IntelliJ Community Edition $IJ_VERSION..."
  wget http://download-cf.jetbrains.com/idea/ideaIC-${IJ_VERSION}.tar.gz
  tar zxf ideaIC-${IJ_VERSION}.tar.gz
  rm -rf ideaIC-${IJ_VERSION}.tar.gz
  UNPACKED_IDEA=$(find . -name 'idea-IC*' | head -n 1)
  mv "$UNPACKED_IDEA" ".pants.d/intellij/ideaIC"
fi

if [ ! -d .pants.d/intellij/plugins ]; then
  echo "Loading $SCALA_PLUGIN_ID and $PYTHON_CE_PLUGIN_ID for $IJ_BUILD..."
  mkdir plugins
  pushd plugins

  wget -O availables.xml "https://plugins.jetbrains.com/plugins/list/?build=$IJ_BUILD"
  wget -O Scala.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$SCALA_PLUGIN_ID&build=$IJ_BUILD"
  unzip Scala.zip
  rm -rf Scala.zip
  wget -O python.zip "https://plugins.jetbrains.com/pluginManager/?action=download&id=$PYTHON_CE_PLUGIN_ID&build=$IJ_BUILD"
  unzip python.zip
  rm -rf python.zip

  popd
  mv plugins ".pants.d/intellij/plugins"
fi

if [ ! -d .pants.d/intellij/pants ]; then
  echo "Getting latest Pants..."
  pushd .pants.d/intellij/
  git clone https://github.com/pantsbuild/pants
  popd
fi

