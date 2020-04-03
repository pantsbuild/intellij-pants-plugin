package com.twitter.ideprobe.jsonrpc

import com.typesafe.config.ConfigRenderOptions
import com.twitter.ideprobe.ConfigFormat
import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.ConfigWriter
import scala.util.control.NonFatal

object PayloadJsonFormat extends ConfigFormat {
  type SerializedJson = String

  val Null: SerializedJson = toJson(Option.empty[String])

  def fromJson[A: ConfigReader](json: SerializedJson): A =
    try {
      ConfigSource.string(json).loadOrThrow[Map[String, A]].getOrElse("data", null.asInstanceOf[A])
    } catch {
      case NonFatal(e) =>
        throw new Exception(s"Could not parse json: $json", e)
    }

  def fromJsonOpt[A: ConfigReader](json: SerializedJson): Option[A] = {
    ConfigSource.string(json).load[Map[String, A]].toOption.map(_.getOrElse("data", null.asInstanceOf[A]))
  }

  def toJson[A: ConfigWriter](a: A): SerializedJson =
    try {
      val wrapped = Map("data" -> a)
      ConfigWriter[Map[String, A]].to(wrapped).render(ConfigRenderOptions.concise.setJson(true))
    } catch {
      case NonFatal(e) =>
        throw new Exception(s"Could not serialize: $a", e)
    }
}
