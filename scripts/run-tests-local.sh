#!/usr/bin/env bash

export INTELLIJ_PLUGINS_HOME="$HOME/Library/Application Support/IdeaIC14/"
export INTELLIJ_HOME="/Applications/IntelliJ IDEA 14 CE EAP.app/Contents/"

./scripts/run-tests.sh $@