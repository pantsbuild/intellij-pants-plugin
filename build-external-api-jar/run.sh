#!/bin/bash
# IDEA_VERSION=202.6109.22
# IDEA_SHA=b8d2a1567fc29fa7514666aead10e70b23f2f94b301e489817a01ed34f5e46d3

#IDEA_VERSION=203.5981.114
#IDEA_SHA=94b17f42465b8f2af7bc8b9bf0ced1b9989877596d1c2758e4104b089b931b55

IDEA_VERSION=211.7142.45
IDEA_SHA=57f4547abb3814348eddf2f4c4a2df852b297f0ad8c60ec06194f132cd0f901c
SOURCES_DIR="/tmp/idea-sources"
DIR=$(realpath $(dirname "${BASH_SOURCE[0]}"))
GIT_ROOT=$DIR/..

set -e
(
    cd /tmp
    curl -L "https://github.com/JetBrains/intellij-community/archive/idea/${IDEA_VERSION}.zip" -o idea.zip
    echo "$IDEA_SHA idea.zip" | sha256sum -c - && unzip -o -q idea.zip
    mv "/tmp/intellij-community-idea-${IDEA_VERSION}" $SOURCES_DIR
)
(
    cd $DIR
    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 
    export IDEPROBE_DISPLAY=xvfb
    # Disable IntelliJ Data Sharing
    mkdir -p ~/.local/share/JetBrains/consentOptions/
    echo -n rsch.send.usage.stat:1.1:0:1574939222872 > ~/.local/share/JetBrains/consentOptions/accepted
    sbt test
)
(
    cd /tmp
    cp $SOURCES_DIR/LICENSE.txt intellij_license.txt
    cp $SOURCES_DIR/NOTICE.txt intellij_notice.txt
    cp "$GIT_ROOT/LICENSE" license_from_build_project.txt
    zip -xi external-system-test-api.zip -xi external-system-test-api.jar intellij_notice.txt intellij_license.txt license_from_build_project.txt
)
