#!/bin/bash
# Trigger deploy process if travis ci build is on master or on a tag.
if [ "$TRAVIS_BRANCH" == "master" ] || [ ! -z "$TRAVIS_TAG" ]; then
  source scripts/prepare-ci-environment.sh
  ./scripts/deploy/deploy.py --tag="$TRAVIS_TAG"
else
  echo "Not on master. Skip deployment."
fi
