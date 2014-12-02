#!/usr/bin/env bash

CWD=$(pwd)
export INTELLIJ_PLUGINS_HOME="$CWD/.pants.d/intellij/plugins"
export OSS_PANTS_HOME="$CWD/.pants.d/intellij/pants"
export INTELLIJ_HOME="$CWD/.pants.d/intellij/ideaIC"

./scripts/run-tests.sh $@