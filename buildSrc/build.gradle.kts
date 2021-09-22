import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

plugins {
    kotlin("jvm") version "1.5.30"
}

repositories {
    mavenCentral()
}

tasks {
    withType<JavaCompile> {
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
