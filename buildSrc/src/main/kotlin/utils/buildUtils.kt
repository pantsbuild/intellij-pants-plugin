// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package utils

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val isCI = System.getenv("CI")?.let { !it.equals("false", ignoreCase = true) } ?: false

fun Test.configureTests() {
    useJUnit()
    testLogging.apply {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        if (isCI) showStandardStreams = true
    }
    maxParallelForks = 1
}
