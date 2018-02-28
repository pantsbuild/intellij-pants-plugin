# Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
# Licensed under the Apache License, Version 2.0 (see LICENSE).

junit_tests(
  sources = globs('*.java'),
  dependencies=[
    ":guava",
  ]
)

jar_library(
  name="guava",
  jars=[
    jar(org='com.google.guava', name='guava', rev='23.0'),
  ],
)
