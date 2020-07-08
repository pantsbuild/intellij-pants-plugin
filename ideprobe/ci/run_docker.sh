#!/bin/sh
find . -maxdepth 1 -regex '.*/pants_.*\.zip' -delete
TRAVIS_BRANCH=master ./scripts/deploy/deploy.sh --skip-publish
PANTS_ZIP=$(ls | grep 'pants_.*\.zip' | head -1 | head -c -1)

DOCKER_IMAGE=ide-probe
DOCKER_DIRECTORY=/tmp/ideprobe/output
HOST_DIRECTORY=/tmp/ideprobe/output

mkdir -p "${HOST_DIRECTORY}"
docker run  \
  --mount type=bind,source="${HOST_DIRECTORY}",target="${DOCKER_DIRECTORY}" \
  -v ${PWD}/$PANTS_ZIP:/tmp/pants.zip \
  "${DOCKER_IMAGE}" \
  "$@"
