#!/usr/bin/env bash
set -x

export IDEPROBE_DISPLAY=xvfb
export PANTS_PLUGIN_PATH="/tmp/pants.zip"

if [ -z "${TEST_PATTERN}" ]; then
  sbt "pantsTests/test"
else
  sbt "pantsTests/testOnly $TEST_PATTERN -- --ignore-runners=none"
fi