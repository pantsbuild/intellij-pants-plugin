#!/usr/bin/env bash

# script to setup new image (best to do on each intellij version bump)

USERNAME=lwawrzyk
IJ_VERSION=202.6397.94

docker login --username=$USERNAME
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build --tag $USERNAME/ideprobe-pants:$IJ_VERSION -f ideprobe/Dockerfile.deps ideprobe
docker push $USERNAME/ideprobe-pants
