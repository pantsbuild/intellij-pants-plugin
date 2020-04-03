package com.twitter.ideprobe

import com.twitter.ideprobe.dependencies.DependenciesConfig
import com.twitter.ideprobe.ide.intellij.DriverConfig
import com.twitter.ideprobe.ide.intellij.IntellijConfig
import pureconfig.ConfigReader
import pureconfig.generic.auto._

case class IdeProbeConfig(
    intellij: IntellijConfig,
    workspace: Option[WorkspaceConfig] = None,
    resolvers: DependenciesConfig.Resolvers = DependenciesConfig.Resolvers(),
    driver: DriverConfig = DriverConfig()
)

object IdeProbeConfig extends ConfigFormat {
  implicit val format: ConfigReader[IdeProbeConfig] = exportReader[IdeProbeConfig].instance
}
