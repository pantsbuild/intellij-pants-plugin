// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

plugins {
    scala
    `java-library`
    id("org.jetbrains.intellij")
}


dependencies {
    implementation("org.scala-lang:scala-library:2.13.3")
    implementation(project(":common"))
    implementation(project(":"))
    api(files("external-system-test-api.jar"))
}
