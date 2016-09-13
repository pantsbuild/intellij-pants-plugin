resources(
  name='plugin-resources',
  sources=globs('META-INF/plugin.xml', 'intellij-jps-plugin-system.jar')
)

jvm_binary(
  name='intellij-sdk-bin',
  basename='intellij-sdk',
  dependencies=[
    ':plugin-resources',
    '3rdparty/intellij:external-system-test-api',
    '3rdparty/intellij:sdk',
  ],
  deploy_excludes = [
    exclude('com.intellij.sdk.community', 'slf4j-api'),
    exclude('com.intellij.sdk.community', 'slf4j-log4j12'),
  ],
)

jvm_binary(
  name='intellij-junit-plugin-bin',
  basename='intellij-junit-plugin',
  dependencies=['3rdparty/intellij:junit-plugin'],
)

jvm_binary(
  name='intellij-python-plugin-bin',
  basename='intellij-python-plugin',
  dependencies=['3rdparty/intellij:python-plugin']
)

jvm_binary(
  name='intellij-scala-plugin-bin',
  basename='intellij-scala-plugin',
  dependencies=['3rdparty/intellij:scala-plugin'],
)

jvm_binary(
  name='intellij-pants-plugin-bin',
  basename='intellij-pants-plugin',
  dependencies=[
    'common',
    'tests:misc',
    'tests:jvm-integration',
    'tests:py-integration',
    'tests:metrics',
    'jps-plugin',
    'src:pants-intellij-plugin',
  ],
  deploy_excludes = [
    exclude('com.intellij.sdk.community', 'slf4j-api'),
    exclude('com.intellij.sdk.community', 'slf4j-log4j12'),
  ],
)
