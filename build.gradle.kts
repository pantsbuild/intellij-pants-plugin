import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import utils.publicationChannels

// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

plugins {
    scala
    id("org.jetbrains.intellij") version "1.1.6"
}

group = "com.intellij.plugins"

allprojects {
    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("org.jetbrains.intellij") {
        val ideaVersion: String by project
        val scalaPluginVersion: String by project
        intellij {
            type.set("IC")
            version.set(ideaVersion)
            plugins.set(listOf(
                    "com.intellij.properties",
                    "org.intellij.groovy",
                    "com.intellij.gradle",
                    "com.intellij.java",
                    "PythonCore:$ideaVersion",
                    "org.intellij.scala:$scalaPluginVersion",
                    "JUnit")
            )
        }
    }
    tasks {
        withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
        }
    }
}

dependencies {
    val scalaVersion: String by project
    implementation(project(":common"))
    compileOnly("org.scala-lang:scala-library:$scalaVersion")

    testImplementation(project(":testFramework"))
    testCompileOnly("org.scala-lang:scala-library:$scalaVersion")
}

tasks {
    patchPluginXml {
        val pluginVersion: String by project
        val pluginSinceBuild: String by project
        val pluginUntilBuild: String by project
        version.set(pluginVersion)
        sinceBuild.set(pluginSinceBuild)
        untilBuild.set(pluginUntilBuild)
    }

    val separateTests by registering(Test::class) {
        // Those tests have to run separately due to the reuse of the project between tests
        group = "verification"
        useJUnit()
        filter {
            includeTestsMatching("*.PantsProjectCacheTest")
        }
        testLogging {
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = FULL
            if (utils.isCI) showStandardStreams = true
        }
        maxParallelForks = 1
        setForkEvery(1)
    }

    test {
        useJUnit()
        testLogging {
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = FULL
            if (utils.isCI) showStandardStreams = true
        }
        maxParallelForks = 1

        doFirst {
            // For tests/com/twitter/intellij/pants/integration/WholeRepoIntegrationTest.java
            file(".cache/dummy_repo").takeIf { it.exists() }?.deleteRecursively()
            file("src/test/resources/testData/dummy_repo").copyRecursively(file(".cache/dummy_repo"), overwrite = true)
            file(".cache/dummy_repo/pants").setExecutable(true)

            // Remove IntelliJ index cache.
            file(".cache/intellij/*/idea-dist/system/caches/").takeIf { it.exists() }?.deleteRecursively()
        }
        finalizedBy(separateTests)
    }

    publishPlugin {
        channels.set(publicationChannels)
        System.getenv("TOKEN")?.takeIf { it.isNotBlank() }?.let {
            token.set(it)
        }
    }
}
