#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

function usage() {
  echo "Runs commons tests for local or hosted CI."
  echo
  echo "Usage: $0 (-h|-bsrdpceat)"
  echo " -h        print out this help message"
  echo " -r        path to your test repo"
  echo " -f        file containing list of targets"
  echo " -t        comma separated list of targets"
  if (( $# > 0 )); then
    echo "$@"
    exit -1
  else
    exit 0
  fi
}

while getopts "hf:t:r:" opt; do
  case ${opt} in
    h) usage ;;
    f) target_list_file=$OPTARG ;;
    t) targets_list=$OPTARG ;;
    r) repo=$OPTARG ;;
    *) usage "Invalid option: -$OPTARG" ;;
  esac
done

if [ -z "$repo" ]; then
  echo "-r [option] is required"
  exit -1
fi

target_list_option=''
targets_list_args=''
if [ ! -z "$targets_list" ]; then
  target_list_option="project.targets"
  targets_list_args="$targets_list"
fi

if [ ! -z "$target_list_file" ]; then
  target_list_option="project.target.list.file"
  targets_list_args="$target_list_file"
fi

CWD=$(pwd)
./pants test.junit \
  --jvm-options="-Didea.load.plugins.id=com.intellij.plugins.pants" \
  --jvm-options="-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME" \
  --jvm-options="-Didea.home.path=$CWD/.pants.d/intellij/plugins-sandbox/test" \
  --jvm-options="-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java" \
  --jvm-options="-Dpants.jps.plugin.classpath=$CWD/.pants.d/resources/prepare/jps-plugin.services" \
  --jvm-options="-Dproject.workspace=$repo"  \
  --jvm-options="-D$target_list_option=$targets_list_args" \
  testFramework/com/twitter/intellij/pants/testFramework/runner
