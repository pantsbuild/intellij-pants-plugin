junit_tests(
    name = 'all',
    dependencies = [
        ':integration',
        ':py-integration',
    ],
    sources =
        globs('com/twitter/intellij/pants/completion/*.java') +
        globs('com/twitter/intellij/pants/components/impl/*.java') +
        globs('com/twitter/intellij/pants/execution/*.java') +
        globs('com/twitter/intellij/pants/highlighting/*.java') +
        globs('com/twitter/intellij/pants/quickfix/*.java') +
        globs('com/twitter/intellij/pants/resolve/*.java') +
        globs('com/twitter/intellij/pants/service/project/*.java') +
        globs('com/twitter/intellij/pants/service/util/*.java')
)

junit_tests(
    name = 'integration',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        'jps-plugin',
        'src:pants-intellij-plugin-scala',
        'testFramework:test-lib'
    ],
    sources = globs('com/twitter/intellij/pants/integration/*.java')
)

junit_tests(
    name = 'py-integration',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        'jps-plugin',
        'src:pants-intellij-plugin-scala',
        'testFramework:test-lib'
    ],
    sources = globs('com/twitter/intellij/pants/integration/python/*.java')
)