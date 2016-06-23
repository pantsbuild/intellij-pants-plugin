scala_library(
    name='pants-intellij-plugin',
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
        'resources',
    ],
    sources=rglobs('*.scala', '*.java')
)
