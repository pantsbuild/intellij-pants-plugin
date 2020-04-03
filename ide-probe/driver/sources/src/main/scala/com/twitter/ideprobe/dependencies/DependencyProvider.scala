package com.twitter.ideprobe.dependencies

final class DependencyProvider(
    intelliJResolver: DependencyResolver[IntelliJVersion],
    pluginResolver: DependencyResolver[Plugin],
    resources: ResourceProvider
) {
  def fetch(intelliJ: IntelliJVersion): Resource = {
    val uri = intelliJResolver.resolve(intelliJ)
    resources.get(uri)
  }

  def fetch(plugin: Plugin): Resource = {
    val uri = pluginResolver.resolve(plugin)
    resources.get(uri)
  }
}
