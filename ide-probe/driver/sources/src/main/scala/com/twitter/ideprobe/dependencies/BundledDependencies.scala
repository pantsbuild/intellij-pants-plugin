package com.twitter.ideprobe.dependencies

import com.twitter.ideprobe.BuildInfo

object BundledDependencies {
  val probePlugin: Plugin.Direct = fromResources("ideprobe")

  def fromResources(name: String): Plugin.Direct = {
    val path = s"/$name-${BuildInfo.version}.zip"
    val resource = getClass.getResource(path)
    if (resource == null)
      throw new Error(s"Plugin $name is not available at path $path")

    Plugin.Direct(resource.toURI)
  }
}
