java_library(
    name = 'test-lib',
    dependencies = [
        ':junit',
        '3rdparty/intellij:external-system-test-api',
        '3rdparty/intellij:junit-plugin',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:java-plugin',
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:scala-plugin',
        'src:pants-intellij-plugin',
        'common',
    ],
    sources = ['*.java'],
)

jar_library(
    name = "junit",
    jars = [
        jar(
            org = "junit",
            name = "junit",
            rev = "4.12",
        ),
    ],
)

jvm_binary(
  name = 'test-lib-bin',
  basename = 'intellij-pants-plugin-test-lib',
  dependencies = [':test-lib']
)
