#!/usr/bin/env bash
set -exo pipefail
source scripts/prepare-ci-environment.sh

if [ -z $JAVA_HOME ]; then
  echo "Please set JAVA_HOME"
  exit 1
fi

JDK_LIB_DIR="$JAVA_HOME/lib"

CACHE_JDK_LIB_DIR='.cache/jdk-libs'

mkdir -p "$CACHE_JDK_LIB_DIR"

for jar_file in $JDK_JARS; do
  src_jar="$JDK_LIB_DIR/$jar_file"
  dest_jar="$CACHE_JDK_LIB_DIR/$jar_file"
  if [ -f "$src_jar" ] && [ ! -f "$dest_jar" ]; then
    cp "$src_jar" "$dest_jar"
  fi
done

# Checking .git because cache entry '.cache/pants' in .travis.yml
# will alawys create directory .cache/pants
if [ ! -d .cache/pants/.git ]; then
  echo "Getting latest Pants..."
  pushd .cache
  git clone https://github.com/pantsbuild/pants
  echo "Bootstrapping Pants..."
  pushd pants

  git checkout 33735fe23228472367dc73f26bb96a755452192f
  ./pants help goals
  popd
  popd
fi
