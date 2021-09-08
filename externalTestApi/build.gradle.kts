// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).
import java.net.URI

plugins {
    `java-library`
}

repositories {
    val github = ivy {
        url = URI("https://github.com/")
        patternLayout {
            artifact("/[organisation]/[module]/releases/download/[revision]/[classifier].[ext]")
        }
        metadataSources { artifact() }
    }

    exclusiveContent {
        forRepositories(github)
        filter { includeGroup("pantsbuild") }
    }
}

val externalSystemTestApi: Configuration by configurations.creating

fun externalSystemTestApiJar() = zipTree(externalSystemTestApi.singleFile).matching {
    include("external-system-test-api.jar")
}

dependencies {
    externalSystemTestApi("pantsbuild:intellij-pants-plugin:external-system-test-api-v211.7142.45:external-system-test-api@zip")
    api(externalSystemTestApiJar())
}


