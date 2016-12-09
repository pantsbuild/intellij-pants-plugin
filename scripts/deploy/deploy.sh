#!/bin/bash
if [ "$TRAVIS_BRANCH" == "master" ]; then
  source scripts/prepare-ci-environment.sh
  ./scripts/deploy/deploy.py
else
  echo "Not on master. Skip deployment."
fi
