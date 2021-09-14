import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

plugins {
    scala
    id("org.jetbrains.intellij") version "1.1.4"
}

group = "com.intellij.plugins"

allprojects {
    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/intellij-jbr")
    }

    pluginManager.withPlugin("org.jetbrains.intellij") {
        val idea_version: String by project
        val scala_plugin_version: String by project
        intellij {
            type.set("IC")
            version.set(idea_version)
            plugins.set(listOf(
                    "com.intellij.properties",
                    "org.intellij.groovy",
                    "com.intellij.gradle",
                    "com.intellij.java",
                    "PythonCore:$idea_version",
                    "org.intellij.scala:$scala_plugin_version",
                    "JUnit")
            )
        }
    }
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.3")
    implementation(project(":common"))
    testImplementation(project(":testFramework"))
    testCompileOnly(files("testFramework/external-system-test-api.jar"))
}

tasks {
    test {
        useJUnit()
        testLogging {
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = FULL
        }
        maxParallelForks = 1

        doFirst {
            // For tests/com/twitter/intellij/pants/integration/WholeRepoIntegrationTest.java
            file(".cache/dummy_repo").takeIf { it.exists() }?.deleteRecursively()
            file("src/test/resources/testData/dummy_repo").copyRecursively(file(".cache/dummy_repo"), overwrite = true)

            // Remove IntelliJ index cache.
            file(".cache/intellij/*/idea-dist/system/caches/").takeIf { it.exists() }?.deleteRecursively()
        }
    }
    patchPluginXml {
        pluginXmlFiles.addAll(
                file("src/main/resources/META-INF/pants-python.xml"),
                file("src/main/resources/META-INF/pants-scala.xml"),
        )
    }

    val miscTests by creating(org.gradle.api.tasks.testing.Test::class) {
        useJUnit()
        filter {
            includeTestsMatching("*.completion.*")
            includeTestsMatching("*.components.*")
            includeTestsMatching("*.execution.*")
            includeTestsMatching("*.extension.*")
            includeTestsMatching("*.highlighting.*")
            includeTestsMatching("*.macro.*")
            includeTestsMatching("*.model.*")
            includeTestsMatching("*.psi.*")
            includeTestsMatching("*.quickfix.*")
            includeTestsMatching("*.resolve.*")
            includeTestsMatching("*.service.*")
            includeTestsMatching("*.settings.*")
            includeTestsMatching("*.util.*")
            includeTestsMatching("*.rc.*")
        }
        testLogging {
            showCauses = true
            showStackTraces = true
            exceptionFormat = FULL
        }
    }
}
