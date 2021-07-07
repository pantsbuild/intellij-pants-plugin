#!/bin/bash

SOURCES_DIR="/tmp/idea-sources"
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
    cp $SOURCES_DIR/LICENSE.txt intellij_license.txt
    cp $SOURCES_DIR/NOTICE.txt intellij_notice.txt
    cp "$GIT_ROOT/LICENSE" license_from_build_project.txt
    zip -xi external-system-test-api.zip -xi external-system-test-api.jar intellij_notice.txt intellij_license.txt license_from_build_project.txt
)
