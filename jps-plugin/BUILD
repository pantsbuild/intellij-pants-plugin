import os

java_library(
    name = 'jps-plugin',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        ':gson',
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

jar_library(name='gson',
            jars=[
                jar(org='com.google.code.gson',
                    name='gson',
                    rev='2.3.1',
                    # To get jps-plugin/lib/gson-2.3.1.jar
                    url="file://" + os.path.join(os.getcwd(), 'jps-plugin', 'lib'))
            ])

