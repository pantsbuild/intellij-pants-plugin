java_library(
    name = 'pants-intellij-plugin-java',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        'common',
        'jps-plugin',
        ':pants-intellij-plugin-scala'
    ],
    resources = [
        'resources'
    ],
    sources = rglobs('*.java')
)

scala_library(
    name = 'pants-intellij-plugin-scala',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        'common',
        'jps-plugin',
    ],
    resources = [
        'resources',
    ],
    java_sources = [
        ':pants-intellij-plugin-java'
    ],
    sources = rglobs('*.scala')
)