package com.twitter.ideprobe.dependencies

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import com.twitter.ideprobe.Extensions._

trait ResourceProvider {
  def get(uri: URI): Resource
}

object ResourceProvider {
  def from(config: DependenciesConfig.ResourceProvider): ResourceProvider = {
    config.cacheDir.map(new Cached(_)).getOrElse(Default)
  }

  val Default = new Cached(Paths.get(sys.props("java.io.tmpdir"), "ideprobe", "cache"))

  val Direct: ResourceProvider = Resource.from(_)

  final class Cached(directory: Path) extends ResourceProvider {
    override def get(uri: URI): Resource = {
      Resource.from(uri) match {
        case Resource.Http(uri) =>
          val cachedResource = cached(uri)
          if (!cachedResource.isFile) {
            println(s"Fetching $uri into $cachedResource")
            Files
              .createTempFile("cached-resource", "-tmp")
              .append(uri.toURL.inputStream)
              .moveTo(cachedResource)
          }

          Resource.File(cachedResource)
        case nonCacheable =>
          nonCacheable
      }
    }

    private def cached(uri: URI): Path = {
      directory.resolve(Hash.md5(uri.toString))
    }
  }
}
