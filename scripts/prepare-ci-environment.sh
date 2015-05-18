#!/usr/bin/env bash

export CWD=$(pwd)
export IJ_VERSION="14.1"
export IJ_BUILD_NUMBER="141.177"

export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/$IJ_BUILD_NUMBER/plugins"
export INTELLIJ_HOME="$CWD/.cache/intellij/$IJ_BUILD_NUMBER/idea-dist"
export OSS_PANTS_HOME="$CWD/.cache/pants"
export JDK_LIBS_HOME="$CWD/.cache/jdk-libs"