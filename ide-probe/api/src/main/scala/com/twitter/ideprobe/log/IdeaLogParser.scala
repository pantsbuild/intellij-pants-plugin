package com.twitter.ideprobe.log

import scala.annotation.tailrec

object IdeaLogParser {

  private val errorLineRegex = logLineRegex(level = "ERROR")
  private val anyLogLineRegex = logLineRegex(level = "[A-Z]+")

  private def logLineRegex(level: String) = raw""".*\s+\[\s*\d+\]\s+$level - .*? - (.*)""".r

  def extractErrors(content: String, fromLastStart: Boolean = false): Seq[String] = {
    @tailrec
    def extract(lines: Seq[String], errors: Seq[String]): Seq[String] = {
      val atFirstError = lines.dropWhile(isNotErrorLine)
      if (atFirstError.isEmpty) {
        errors
      } else {
        val index = atFirstError.indexWhere(isNonErrorLogLine)
        val (errorBlock, rest) = if (index == -1) (atFirstError, Nil) else atFirstError.splitAt(index)
        val error = errorBlock
          .collect {
            case errorLineRegex(message) => message
            case nonLogLine              => nonLogLine
          }
          .mkString("\n")
        extract(rest, errors :+ error)
      }
    }

    val allLines = content.linesIterator.toSeq
    val linesToParse = if (fromLastStart) dropOldLines(allLines) else allLines
    extract(linesToParse, Seq.empty)
  }

  private def dropOldLines(allLines: Seq[String]): Seq[String] = {
    allLines.reverseIterator.takeWhile(line => !line.contains("- IDE STARTED -")).toSeq.reverse
  }

  private def isNonErrorLogLine(line: String) = {
    !errorLineRegex.matches(line) && anyLogLineRegex.matches(line)
  }

  private def isNotErrorLine(string: String): Boolean = {
    !errorLineRegex.matches(string)
  }
}
