#!/bin/bash
  source scripts/prepare-ci-environment.sh
  python3 ./scripts/deploy/deploy.py --tag="$TRAVIS_TAG" "$@"