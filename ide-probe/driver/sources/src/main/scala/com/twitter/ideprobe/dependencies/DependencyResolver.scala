package com.twitter.ideprobe.dependencies

import java.net.URI

trait DependencyResolver[Key] {
  def resolve(key: Key): URI
}
