// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

plugins {
    scala
    `java-library`
    id("org.jetbrains.intellij")
}


dependencies {
    val scalaVersion: String by project
    implementation(project(":"))
    implementation(project(":common"))
    compileOnly("org.scala-lang:scala-library:$scalaVersion")
    api(files("external-system-test-api.jar"))
}
