#!/usr/bin/env bash

prepare_ci_env() {
  CWD=$(pwd)
  export INTELLIJ_PLUGINS_HOME="$CWD/.cache/intellij/plugins"
  export OSS_PANTS_HOME="$CWD/.cache/intellij/pants"
  export INTELLIJ_HOME="$CWD/.cache/intellij/idea-dist"
}