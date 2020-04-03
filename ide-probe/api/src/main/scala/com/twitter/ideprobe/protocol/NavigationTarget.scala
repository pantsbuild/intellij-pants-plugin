package com.twitter.ideprobe.protocol

final case class NavigationQuery(project: ProjectRef = ProjectRef.Default, value: String)
final case class NavigationTarget(name: String, location: String)
