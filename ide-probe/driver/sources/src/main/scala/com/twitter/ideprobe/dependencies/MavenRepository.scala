package com.twitter.ideprobe.dependencies

import java.net.URI
import com.twitter.ideprobe.dependencies.MavenRepository.Key

final class MavenRepository(uri: String) extends DependencyResolver[Key] {
  override def resolve(key: Key): URI = {
    import key._
    val groupPath = group.replace('.', '/')
    URI.create(s"$uri/$groupPath/$artifact/$version/$artifact-$version.zip")
  }
}

object MavenRepository {
  final case class Key(group: String, artifact: String, version: String)
}
