#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

cp $INTELLIJ_HOME/lib/rt/jps-plugin-system.jar dist/intellij-jps-plugin-system.jar

./pants binary scripts/sdk: