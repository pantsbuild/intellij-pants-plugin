java_library(
    name = 'common',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
    ],
    resources = [
        ':properties'
    ],
    sources = rglobs('*.java')
)

resources(
    name = 'properties',
    sources = globs('com/twitter/intellij/pants/PantsBundle.properties')
)

jvm_binary(
  name = 'pants-common',
  basename = 'pants-common',
  dependencies = [':common']
)
