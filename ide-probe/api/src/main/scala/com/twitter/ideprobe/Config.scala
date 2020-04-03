package com.twitter.ideprobe

import java.nio.file.Path
import com.typesafe.config.ConfigFactory
import pureconfig._
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

final case class Config(source: ConfigSource) {

  def apply[A: ClassTag](path: String)(implicit reader: Derivation[ConfigReader[A]]): A = {
    source.at(path).loadOrThrow[A]
  }

  def get[A: ClassTag](path: String)(implicit reader: Derivation[ConfigReader[A]]): Option[A] = {
    source.at(path).load[A].toOption
  }

}

object Config {
  val Empty = new Config(ConfigSource.empty)

  def fromString(str: String) = new Config(ConfigSource.string(str))

  def fromFile(path: Path): Config = {
    new Config(ConfigSource.file(path))
  }

  def fromClasspath(resourcePath: String): Config = {
    new Config(ConfigSource.resources(resourcePath))
  }

  def fromMap(properties: Map[String, String]): Config = {
    new Config(ConfigSource.fromConfig(ConfigFactory.parseMap(properties.asJava)))
  }
}
