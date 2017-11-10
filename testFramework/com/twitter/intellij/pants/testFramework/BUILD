java_library(
    name = 'test-lib',
    dependencies = [
        '3rdparty/intellij:external-system-test-api',
        '3rdparty/intellij:junit-plugin',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:scala-plugin',
        'src:pants-intellij-plugin',
        'common',
    ],
    sources = globs('*.java')
)

jvm_binary(
  name = 'test-lib-bin',
  basename = 'intellij-pants-plugin-test-lib',
  dependencies = [':test-lib']
)
