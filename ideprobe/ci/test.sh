#!/usr/bin/env bash

export IDEPROBE_DISPLAY=xvfb
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
export PANTS_PLUGIN_PATH="/tmp/pants.zip"

sbt test