package com.twitter.handlers

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.protocol.Freeze
import scala.concurrent.duration._
import scala.util.Try

object Freezes extends IntelliJApi {

  private val fileNameRegex = """threadDump-(\d{8}-\d{6})\.txt""".r
  private val directoryNameRegex = """threadDumps-.*?-(\d+)sec""".r
  private val timestampPattern = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

  def list: Seq[Freeze] = findFreezeThreadDumpFiles().map(toFreeze)

  private def findFreezeThreadDumpFiles(): Seq[Path] = {
    val fileStream = Files
      .find(logsPath, /*maxDepth = */ 2, (path, _) => {
        path.name.startsWith("threadDump") && path.name.endsWith("txt")
      })
    val paths =
      try fileStream.iterator().asScala.toList
      finally fileStream.close()
    paths
  }

  private def toFreeze(path: Path): Freeze = {
    val timestampFromFile = path.name match {
      case fileNameRegex(dateStr) =>
        Try(LocalDateTime.parse(dateStr, timestampPattern)).toOption
      case _ => None
    }

    val durationFromDir = path.getParent.name match {
      case directoryNameRegex(duration) => Some(duration.toInt.seconds)
      case _                            => None
    }

    val rawDump = path.content()
    val edtStackTrace = extractEdtStackTrace(rawDump)
    Freeze(durationFromDir, timestampFromFile, edtStackTrace, rawDump)
  }

  private def extractEdtStackTrace(rawDump: String): Seq[String] = {
    rawDump.linesIterator
      .dropWhile(!_.contains("AWT-EventQueue"))
      .drop(3)
      .takeWhile(_.trim.nonEmpty)
      .map(_.trim.stripPrefix("at "))
      .toList
  }
}
