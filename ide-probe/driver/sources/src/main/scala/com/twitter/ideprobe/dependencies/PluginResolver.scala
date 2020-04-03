package com.twitter.ideprobe.dependencies

import java.net.URI

object PluginResolver {
  val Official = PluginResolver("https://plugins.jetbrains.com/plugin/download")

  def apply(uri: String): DependencyResolver[Plugin] = {
    new Resolver(uri)
  }

  def from(configuration: DependenciesConfig.Plugins): DependencyResolver[Plugin] = {
    configuration.repository.map(repo => PluginResolver(repo.uri)).getOrElse(Official)
  }

  private final class Resolver(uri: String) extends DependencyResolver[Plugin] {
    override def resolve(plugin: Plugin): URI = {
      plugin match {
        case Plugin.Direct(uri) =>
          uri
        case Plugin.Versioned(id, version, Some(channel)) =>
          URI.create(s"$uri?pluginId=$id&version=$version&channel=$channel")
        case Plugin.Versioned(id, version, None) =>
          URI.create(s"$uri?pluginId=$id&version=$version")
      }
    }
  }
}
