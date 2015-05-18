#!/usr/bin/env bash

export CWD=$(pwd)
export IJ_VERSION="14.1"
export IJ_BUILD_NUMBER="141.177"

export FULL_IJ_BUILD_NUMBER="IC-${IJ_BUILD_NUMBER}"

export IJ_BUILD="IC-${IJ_VERSION}"
if [[ $IJ_ULTIMATE == "true" ]]; then
  export IJ_BUILD="IU-${IJ_VERSION}"
fi

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$FULL_IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"