java_library(
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        ':properties'
    ],
    sources = rglobs('*.java')
)

resources(
    name = 'properties',
    sources = globs('com/twitter/intellij/pants/PantsBundle.properties')
)
