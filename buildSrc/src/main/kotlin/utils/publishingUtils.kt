// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package utils

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


val publicationChannels: List<String> = System.getenv("publicationChannels")
        ?.takeIf { it.isNotBlank() }
        ?.split(",")
        ?.map { name -> PublicationChannel.from(name).channelName }
        ?: listOf(PublicationChannel.BleedingEdge.channelName)
