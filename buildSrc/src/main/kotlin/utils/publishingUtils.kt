// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package utils

import org.gradle.api.*

enum class PublicationChannel(val channelName: String) {
    Stable("Stable"), BleedingEdge("BleedingEdge");

    companion object {
        fun from(name: String) = when (name.toLowerCase()) {
            "stable" -> Stable
            "bleedingedge" -> BleedingEdge
            else -> throw IllegalArgumentException("Unrecognized publication channel: $name")
        }
    }
}


val Project.publicationChannels: List<String>
    get() = (properties["publicationChannels"] as String?)
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { name -> PublicationChannel.from(name).channelName }
            ?: listOf(PublicationChannel.BleedingEdge.channelName)

val Project.projectVersion: String
    get() = properties["pluginVersion"] as String +
            (properties["versionSuffix"] as String?)?.takeIf { it.isNotBlank() }.orEmpty()

