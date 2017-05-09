#!/bin/bash
if [ "$TRAVIS_BRANCH" == "master" ] || [ ! -z "$TRAVIS_TAG" ]; then
  source scripts/prepare-ci-environment.sh
  ./scripts/deploy/deploy.py --tag="$TRAVIS_TAG"
else
  echo "Not on master. Skip deployment."
fi
