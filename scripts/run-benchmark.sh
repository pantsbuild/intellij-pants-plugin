#!/usr/bin/env bash

# Usage:
# ./scripts/run-benchmark.sh --output timigs.txt -- <targets>

source scripts/prepare-ci-environment.sh

jvm_options_file=""
disabled_plugins_file=""
while [ $# -ge 1 ]; do
        case "$1" in
                --)
                    # No more options left.
                    shift
                    break
                   ;;
                --output)
                        output=$2
                        shift
                        ;;
                --jvm-options-file)
                        jvm_options_file=$2
                        shift
                        ;;
                --disabled-plugins-file)
                        disabled_plugins_file=$2
                        shift
                        ;;
        esac
        shift
done

echo "output = $output"
echo "jvm_options_file = $jvm_options_file"
echo "disabled_plugins_file = $disabled_plugins_file"

cmd=$(append_intellij_jvm_options run.jvm "$jvm_options_file")
cmd="$cmd --jvm-options=-Didea.is.unit.test=true"

cmd="$cmd testFramework/com/twitter/intellij/pants/testFramework/performance"

> $output

for target in $@
do
    rm -rf $IDEA_TEST_HOME
    mkdir -p $IDEA_TEST_HOME
    additional_args=""
    if [ -n "$disabled_plugins_file" ]; then
      additional_args="--run-jvm-args=-disabled-plugins-file --run-jvm-args=$disabled_plugins_file";
    fi
    target_cmd="$cmd --run-jvm-args=-target --run-jvm-args=$target --run-jvm-args=-output --run-jvm-args=$output $additional_args"
    echo "Executing:"
    echo "pants $target_cmd"
    ./pants $target_cmd
done
