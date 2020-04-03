package com.twitter.ideprobe.protocol

import java.time.LocalDateTime
import scala.concurrent.duration.FiniteDuration

case class Freeze(
    duration: Option[FiniteDuration],
    timestamp: Option[LocalDateTime],
    edtStackTrace: Seq[String],
    fullDump: String
)
