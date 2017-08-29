#!/usr/bin/env bash

source scripts/prepare-ci-environment.sh

function usage() {
  echo "Runs commons tests for local or hosted CI."
  echo
  echo "Usage: $0 (-h|-rtf)"
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
  case $opt in
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

cmd=$(append_intellij_jvm_options "test.junit")
cmd="$cmd --jvm-options=-Dproject.workspace=$repo"
if [ ! -z "$targets_list" ]; then
  cmd="$cmd --jvm-options=-Dproject.targets=$targets_list"
fi

if [ ! -z "$target_list_file" ]; then
  cmd="$cmd --jvm-options=-Dproject.target.list.file=$target_list_file"
fi

./pants $cmd testFramework/com/twitter/intellij/pants/testFramework/runner
