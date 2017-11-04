# Alias for sources and libs needed for tests
target(name='sources-and-libs',
  dependencies=[
    '3rdparty/intellij:gradle-plugin',
    '3rdparty/intellij:python-plugin',
    '3rdparty/intellij:scala-plugin',
    '3rdparty/intellij:sdk',
    'jps-plugin',
    'src:pants-intellij-plugin',
    'testFramework:test-lib',
  ],
)

junit_tests(
  name='misc',
  dependencies=[
    ':sources-and-libs',
  ],
  sources=globs(
    # 'com/twitter/intellij/pants/completion/*.java',
    # 'com/twitter/intellij/pants/components/impl/*.java',
    # 'com/twitter/intellij/pants/execution/*.java',
    # 'com/twitter/intellij/pants/extension/*.java',
    # 'com/twitter/intellij/pants/highlighting/*.java',
    # 'com/twitter/intellij/pants/macro/*.java',
    # 'com/twitter/intellij/pants/model/*.java',
    # 'com/twitter/intellij/pants/psi/reference/*.java',
    # 'com/twitter/intellij/pants/quickfix/*.java',
    'com/twitter/intellij/pants/resolve/*.java',
    # 'com/twitter/intellij/pants/service/project/*.java',
    # 'com/twitter/intellij/pants/service/project/detector/*.java',
    # 'com/twitter/intellij/pants/service/project/model/*.java',
    # 'com/twitter/intellij/pants/service/util/*.java',
    # 'com/twitter/intellij/pants/settings/*.java',
    # 'com/twitter/intellij/pants/util/*.java',
  )
)

junit_tests(
  name='jvm-integration',
  dependencies=[
    ':sources-and-libs',
  ],
  sources=globs(
    'com/twitter/intellij/pants/integration/*.java',
  )
)

junit_tests(
  name='py-integration',
  dependencies=[
    ':sources-and-libs',
  ],
  sources=globs(
    'com/twitter/intellij/pants/integration/python/*.java',
  )
)

junit_tests(
  name='metrics',
  dependencies=[
    ':sources-and-libs',
  ],
  sources=globs('com/twitter/intellij/pants/metrics/*.java')
)
