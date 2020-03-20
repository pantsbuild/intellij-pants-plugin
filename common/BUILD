java_library(
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        '3rdparty/intellij:java-plugin',
        ':properties'
    ],
    sources = ['**/*.java'],
)

resources(
    name = 'properties',
    sources = ['com/twitter/intellij/pants/PantsBundle.properties'],
)
