scala_library(
    name = 'pants-intellij-plugin-scala',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
    ],
    resources = [
        'resources',
        ':properties'
    ],
    java_sources = [
        ':pants-intellij-plugin-java'
    ],
    sources = rglobs('*.scala')
)

java_library(
    name = 'pants-intellij-plugin-java',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        ':pants-intellij-plugin-scala'
    ],
    resources = [
        'resources'
    ],
    sources = rglobs('*.java')
)

resources(
    name = 'properties',
    sources = globs('com/twitter/intellij/pants/PantsBundle.properties')
)