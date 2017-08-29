java_library(
    name = 'jps-plugin',
    dependencies = [
        '3rdparty/intellij:sdk',
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

