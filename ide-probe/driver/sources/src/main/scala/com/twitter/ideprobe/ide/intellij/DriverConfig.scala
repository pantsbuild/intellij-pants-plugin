package com.twitter.ideprobe.ide.intellij

import com.twitter.ideprobe.ide.intellij.DriverConfig.LaunchParameters
import scala.concurrent.duration._

case class DriverConfig(
    launch: LaunchParameters = LaunchParameters(),
    headless: Boolean = true,
    vmOptions: Seq[String] = Nil
)

object DriverConfig {
  case class LaunchParameters(command: Seq[String] = Nil, timeout: FiniteDuration = 30.seconds)
}
