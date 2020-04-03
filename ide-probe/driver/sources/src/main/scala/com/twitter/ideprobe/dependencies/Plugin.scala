package com.twitter.ideprobe.dependencies

import java.net.URI
import com.twitter.ideprobe.ConfigFormat
import com.twitter.ideprobe.ConfigFormat
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

sealed trait Plugin

object Plugin extends ConfigFormat {
  case class Versioned(id: String, version: String, channel: Option[String]) extends Plugin
  case class Direct(uri: URI) extends Plugin

  def apply(id: String, version: String, channel: Option[String] = None): Versioned = {
    Versioned(id, version, channel)
  }

  implicit val pluginReader: ConfigReader[Plugin] = {
    possiblyAmbiguousAdtReader[Plugin](deriveReader[Versioned], deriveReader[Direct])
  }

}
