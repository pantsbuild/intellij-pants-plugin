java_library(
    name='pants-intellij-plugin-java',
    dependencies=[
        '3rdparty/intellij:junit-plugin',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        '3rdparty/intellij:sdk',
        '3rdparty/jvm:metrics',
        'common',
        'jps-plugin',
    ],
    resources=[
        'resources'
    ],
    sources=rglobs('*.java')
)

scala_library(
    name='pants-intellij-plugin-scala',
    dependencies=[
        ':pants-intellij-plugin-java'
    ],
    resources=[
        'resources',
    ],
    sources=rglobs('*.scala')
)

jvm_binary(
  name = 'pants',
  basename = 'pants',
  dependencies = [':pants-intellij-plugin-scala']
)
