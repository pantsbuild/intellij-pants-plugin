#!/bin/bash
if [ "$TRAVIS_BRANCH" == "master" ]; then
  source scripts/prepare-ci-environment.sh
  ./scripts/deploy/deploy.py
fi
