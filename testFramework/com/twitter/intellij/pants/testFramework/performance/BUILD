jvm_binary(name = 'performance',
    dependencies = [
        '3rdparty/intellij:sdk',
        '3rdparty/intellij:python-plugin',
        '3rdparty/intellij:scala-plugin',
        'src:pants-intellij-plugin-scala',
        'testFramework:test-lib'
    ],
    source = 'PantsPerformanceBenchmark.scala',
    main = 'com.twitter.intellij.pants.testFramework.performance.PantsPerformanceBenchmark',
)

