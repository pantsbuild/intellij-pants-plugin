package com.twitter.ideprobe.dependencies

import java.nio.file.Path

object DependenciesConfig {
  case class Resolvers(
      intellij: IntelliJ = IntelliJ(None),
      plugins: Plugins = Plugins(None),
      resourceProvider: ResourceProvider = ResourceProvider(None)
  )

  case class ResourceProvider(cacheDir: Option[Path])

  case class IntelliJ(
      repository: Option[IntellijMavenRepository]
  )

  case class IntellijMavenRepository(
      uri: String,
      group: String,
      artifact: String
  )

  case class Plugins(
      repository: Option[PluginRepository]
  )

  case class PluginRepository(uri: String)

}
