package com.twitter.handlers

import java.lang.management.ManagementFactory

object App extends IntelliJApi {
  // TODO not the best solution, on java 9+ should be replaced with ProcessHandler
  val pid: Long = {
    val pattern = "(\\d+)@.*".r
    ManagementFactory.getRuntimeMXBean.getName match {
      case pattern(pid) => pid.toLong
      case _            => throw new Error("Could not obtain pid of the current process")
    }
  }

  def shutdown(): Unit = {
    application.exit(true, true, false)
  }
}
