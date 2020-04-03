package com.twitter.ideprobe.ide.intellij

object DebugMode {
  private val suspend: Boolean = false
  private val port: Int = 5005

  def vmOption: String = {
    val suspendOpt = if (suspend) "suspend=y" else "suspend=n"
    val addressOpt = s"address=$port"
    s"-agentlib:jdwp=transport=dt_socket,server=y,$suspendOpt,$addressOpt"
  }
}
