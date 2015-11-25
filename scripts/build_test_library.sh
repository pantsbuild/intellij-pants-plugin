#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

# pack intellij-jps-plugin-system.jar within the sdk
cp $INTELLIJ_HOME/lib/rt/jps-plugin-system.jar scripts/sdk/intellij-jps-plugin-system.jar
./pants binary scripts/sdk:
rm scripts/sdk/intellij-jps-plugin-system.jar