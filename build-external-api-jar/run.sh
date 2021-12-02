#!/bin/bash

set -e

DIR=$(realpath $(dirname "${BASH_SOURCE[0]}"))
GIT_ROOT=$DIR/..

(
    cd $DIR
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 
    export IDEPROBE_DISPLAY=xvfb
    # Disable IntelliJ Data Sharing
    mkdir -p ~/.local/share/JetBrains/consentOptions/
    echo -n rsch.send.usage.stat:1.1:0:1574939222872 > ~/.local/share/JetBrains/consentOptions/accepted
    sbt run
)
(
    cd /tmp
    cp "$GIT_ROOT/LICENSE" license_from_build_project.txt
    zip -xi external-system-test-api.zip -xi external-system-test-api.jar license_from_build_project.txt
)
