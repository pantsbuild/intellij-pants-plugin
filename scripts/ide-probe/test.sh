#!/usr/bin/env bash

export IDEPROBE_DISPLAY=xvfb
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk

mkdir -p ~/.ivy2/local/
wget -nv https://contextbuddy.s3.eu-central-1.amazonaws.com/ide-probe/ideprobe.zip -O /tmp/ideprobe.zip
unzip /tmp/ideprobe.zip -d ~/.ivy2/local/

function setup_version_override {
    INTELLIJ_PANTS_LOCATION="/tmp/pants.zip"
    cat >"pants.conf" <<EOL
probe {
  intellij {
    version = "202.5103.13-EAP-SNAPSHOT"
    plugins = [
      { uri = "${INTELLIJ_PANTS_LOCATION}" },
      { id = "PythonCore",          version = "202.5103.13" },
      { id = "org.intellij.scala",  version = "2020.2.7" }
    ]
  }
}
EOL
    jar -cf overrides.jar pants.conf
    rm pants.conf
    echo overrides.jar
}

function run_junit {
    coursier launch 'org.junit.platform:junit-platform-console-standalone:1.7.0-M1' -- $*
}

function run_tests {
    local test_artifact=$1
    local classpath_prepend=$2

    local test_lookup_classpath=$(coursier fetch $test_artifact | head -1)

    local resolved_classpath=$(coursier fetch $test_artifact -p)
    local final_classpath="$classpath_prepend:$resolved_classpath"

    run_junit -cp $final_classpath \
      --scan-classpath=$test_lookup_classpath \
      --include-classname='.*' \
      --exclude-classname='.*PantsIssue42' \
      --details=verbose
}

run_tests 'com.virtuslab.ideprobe:pants-tests_2.13:0.1:test' $(setup_version_override)