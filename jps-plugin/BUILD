import os

java_library(
    name = 'jps-plugin',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        'common'
    ],
    resources = [
        ':services'
    ],
    sources = rglobs('*.java')
)

resources(
    name = 'services',
    sources = globs('META-INF/services/*')
)

