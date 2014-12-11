#!/usr/bin/env bash

source scripts/prepare-local-environment.sh
source scripts/prepare-ci-environment.sh

function usage() {
  echo "Runs commons tests for local or hosted CI."
  echo
  echo "Usage: $0 (-h|-bsrdpceat)"
  echo " -h        print out this help message"
  echo " -l        run the tests locally"
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

while getopts "hlf:t:r:" opt; do
  case ${opt} in
    h) usage ;;
    l) local_run="true" ;;
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

if [ "$local_run" == "true" ]; then
  prepare_local_env
else
  prepare_ci_env
fi

CWD=$(pwd)
./pants goal test testFramework/com/twitter/intellij/pants/testFramework/runner \
  --test-junit-jvmargs="-Didea.load.plugins.id=com.intellij.plugins.pants" \
  --test-junit-jvmargs="-Didea.plugins.path=$INTELLIJ_PLUGINS_HOME" \
  --test-junit-jvmargs="-Didea.home.path=$CWD/.pants.d/intellij/plugins-sandbox/test" \
  --test-junit-jvmargs="-Dpants.plugin.base.path=$CWD/.pants.d/compile/jvm/java" \
  --test-junit-jvmargs="-Dpants.jps.plugin.classpath=$CWD/.pants.d/resources/prepare/jps-plugin.services" \
  --test-junit-jvmargs="-Dproject.workspace=$repo"  \
  --test-junit-jvmargs="-D$target_list_option=$targets_list_args"