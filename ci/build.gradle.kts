group = "com.intellij.plugins"
version = "1.18.0"

repositories {
    mavenCentral()
}
val cwd: String by lazy { System.getProperty("user.dir") }
val env: Map<String, Any> by lazy {
    val intellijVersion: String by project
    val intellijBuildNumber: String by project
    val intellijSHA: String by project
    val isUltimate = System.getenv().getOrDefault("IJ_ULTIMATE", false) == true
    val intellijPrefix = if (isUltimate) "IU" else "IC"
    val intellijBuild = "$intellijPrefix-$intellijVersion"
    val fullIntellijBuild = "$intellijPrefix-$intellijBuildNumber"
    val pythonId = if (isUltimate) "Pythonid" else "PythonCore"
    val jdkJars = System.getenv()["JAVA_HOME"]?.takeIf {
        file("$it/lib/tools.jar").exists()
    }?.let {
        mapOf("JDK_JARS" to arrayOf("sa-jdi.jar", "tools.jar"))
    } ?: emptyMap()
    mapOf(
            "CWD" to cwd,
            "IJ_VERSION" to intellijVersion,
            "IJ_BUILD_NUMBER" to intellijBuildNumber,
            "IJ_SHA" to intellijSHA,
            "PANTS_TEST_JUNIT_STRICT_JVM_VERSION" to true,
            "MODE" to "debug",
            "IJ_BUILD" to intellijBuild,
            "FULL_IJ_BUILD_NUMBER" to fullIntellijBuild,
            "PYTHON_PLUGIN_ID" to pythonId,
            "INTELLIJ_PLUGINS_HOME" to "$cwd/.cache/intellij/$fullIntellijBuild/plugins",
            "INTELLIJ_HOME" to "$cwd/.cache/intellij/$fullIntellijBuild/idea-dist",
            "OSS_PANTS_HOME" to "$cwd/.cache/pants",
            "DUMMY_REPO_HOME" to "$cwd/.cache/dummy_repo",
            "JDK_LIBS_HOME" to "$cwd/.cache/jdk-libs"
    ) + jdkJars
}

fun appendIntellijJvmOptions(scope: String): String {

    val plugins = mutableListOf(
            "com.intellij.properties",
            "JUnit",
            "org.intellij.groovy",
            "com.intellij.java",
            "org.intellij.intelliLang",
            "PythonCore",
            "com.intellij.modules.python-core-capable",
            "com.intellij.plugins.pants"
    )

    val scalaEnabled = System.getenv().getOrDefault("ENABLE_SCALA_PLUGIN", true) == true
    if (scalaEnabled) plugins.add("org.intellij.scala")

    val intellijJvmOptions = listOf(
            "-Didea.load.plugins.id=${plugins.joinToString(",")}",
            "-Didea.plugins.path=${env["INTELLIJ_PLUGINS_HOME"]}",
            "-Didea.home.path=${env["INTELLIJ_HOME"]}",
            "-Didea.plugins.compatible.build=$${env["IJ_BUILD_NUMBER"]}"
    )

    val vmoptions = file("${env["CWD"]}/resources/idea64.vmoptions").takeIf { it.exists() }?.readLines().orEmpty()

    return (intellijJvmOptions + vmoptions).reduce { acc, s -> "$acc --jvm-$scope-options=$s" }
}
tasks {

}
task<Exec>("setup-pants") {
    workingDir("$cwd/.cache")
    commandLine("git", "clone", "https://github.com/pantsbuild/pants")
    doLast {
        commandLine("git", "checkout", "33735fe23228472367dc73f26bb96a755452192f")
    }
}
task<Exec>("setup-scalameta-pants") {
    workingDir("$cwd/.cache")
    commandLine("git", "clone", "https://github.com/scalameta/pants", "-b", "1.26.x-intellij-plugin", "pants-host")
    doLast {
        commandLine("git", "checkout", "70bcd0aacb3dd3aacf61231c9db54592597776a8")
    }
}

task<Exec>("setup-ci-environment") {
    environment(env)
    val jdkLibDir = "${env["JAVA_HOME"]}/lib"
    environment("JDK_LIB_DIR", jdkLibDir)
    val cacheJdkLibDir = "$cwd/.cache/jdk-libs"
    environment("CACHE_JDK_LIB_DIR", cacheJdkLibDir)
    file(cacheJdkLibDir).mkdirs()
    doFirst {
        (env["JDK_JARS"] as? Array<String>)?.forEach {
            val old = file("$jdkLibDir/$it")
            val new = file("$cacheJdkLibDir/$it")
            if (old.exists() && !new.exists()) {
                old.copyTo(new)
            }
        }
    }
    if (!file("$cwd/.cache/pants/.git").exists()) {
        dependsOn("setup-pants")
    }
    if (!file("$cwd/.cache/pants-host/.git").exists()) {
        dependsOn("setup-scalameta-pants")
    }
    workingDir("$cwd/.cache/pants")
    commandLine("./pants", "help", "goals")
    doLast {
        workingDir("$cwd/.cache/pants-host")
        commandLine("./pants", "help", "goals")
    }
}

task<Exec>("run-tests-ci") {
    environment(env)
    workingDir(cwd)
    val tasksScope: String? by project
    val testSet: String? by project
    val additionalArgs: String? by project
    val arguments = "${tasksScope ?: "-test"} tests:${testSet ?: "-"} " +
            "${appendIntellijJvmOptions("test-junit")} ${additionalArgs.orEmpty()}"
    doFirst {
        file("$cwd/.cache/dummy_repo").deleteRecursively()
        file("$cwd/src/test/resources/testData/dummy_repo").copyRecursively(file("$cwd/.cache/dummy_repo"))
        file("$cwd/..cache/intellij/*/idea-dist/system/caches/").delete()
    }
    commandLine(".cache/pants-host/pants", arguments)
}