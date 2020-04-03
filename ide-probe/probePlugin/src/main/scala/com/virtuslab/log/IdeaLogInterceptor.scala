package com.twitter.log

import com.intellij.diagnostic.LogEventException
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments
import com.intellij.util.ExceptionUtil
import com.twitter.handlers.IntelliJApi
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.log.IdeaLogParser
import com.twitter.ideprobe.protocol.IdeMessage
import org.apache.log4j._
import org.apache.log4j.spi.LoggingEvent

object IdeaLogInterceptor extends IntelliJApi {

  def inject(): Unit = {
    LogManager.getRootLogger.addAppender(new IdeaLogInterceptor)

    val errors = readInitialErrorsFromLogFile()
    errors.foreach(MessageLog.add)
  }

  private def readInitialErrorsFromLogFile(): Seq[Message] = {
    flushFileLogger()
    val initialLog = ideaLogPath.content()
    val errors = IdeaLogParser.extractErrors(initialLog, fromLastStart = true)
    errors.map(err => Message(content = Some(err), throwable = None, IdeMessage.Level.Error))
  }

  private def ideaLogPath = {
    logsPath.resolve("idea.log")
  }

  private def flushFileLogger(): Unit = {
    val rootLogger = LogManager.getRootLogger
    val appenders = rootLogger.getAllAppenders.asInstanceOf[java.util.Enumeration[Appender]]
    appenders.asScala.collect {
      case fileAppender: FileAppender =>
        val originalFlush = fileAppender.getImmediateFlush
        fileAppender.setImmediateFlush(true)
        rootLogger.info("Flush logs")
        fileAppender.setImmediateFlush(originalFlush)
    }
  }
}

class IdeaLogInterceptor extends AppenderSkeleton {

  override def append(loggingEvent: LoggingEvent): Unit = {
    if (loggingEvent.getLevel.isGreaterOrEqual(Level.ERROR)) {
      messageFromIdeaLoggingEvent(loggingEvent, IdeMessage.Level.Error).foreach(MessageLog.add)
    } else if (loggingEvent.getLevel.equals(Level.WARN)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Warn)
      MessageLog.add(msg)
    } else if (loggingEvent.getLevel.equals(Level.INFO)) {
      val msg = extractAnyMessage(loggingEvent, IdeMessage.Level.Info)
      MessageLog.add(msg)
    }
  }

  private def extractAnyMessage(loggingEvent: LoggingEvent, level: IdeMessage.Level) = {
    messageFromIdeaLoggingEvent(loggingEvent, level)
      .getOrElse(simpleMessage(loggingEvent, level))
  }

  private def messageFromIdeaLoggingEvent(loggingEvent: LoggingEvent, level: IdeMessage.Level): Option[Message] = {
    extractIdeaLoggingEvent(loggingEvent).map { ideaLoggingEvent =>
      val message = anyToString(ideaLoggingEvent)
      Message(message, Option(ideaLoggingEvent.getThrowable), level)
    }
  }

  // logic here roughly matches com.intellij.diagnostic.DialogAppender that is disabled in headless mode
  private def extractIdeaLoggingEvent(loggingEvent: LoggingEvent): Option[IdeaLoggingEvent] = {
    loggingEvent.getMessage match {
      case e: IdeaLoggingEvent => Some(e)
      case otherMessage =>
        for {
          info <- Option(loggingEvent.getThrowableInformation)
          throwable <- Option(info.getThrowable)
        } yield {
          ExceptionUtil.getRootCause(throwable) match {
            case logEventEx: LogEventException => logEventEx.getLogMessage
            case _ =>
              val msg =
                ExceptionUtil.findCause(throwable, classOf[ExceptionWithAttachments]) match {
                  case re: RuntimeExceptionWithAttachments =>
                    re.getUserMessage
                  case _ => Option(otherMessage).fold("")(_.toString)
                }
              new IdeaLoggingEvent(msg, throwable)
          }
        }
    }
  }

  private def simpleMessage(loggingEvent: LoggingEvent, level: IdeMessage.Level): Message = {
    Message(anyToString(loggingEvent.getMessage), throwable = None, level)
  }

  private def anyToString(any: Any): Option[String] = {
    Option(any).map(_.toString).map(_.trim).filter(_.nonEmpty)
  }

  override def close(): Unit = ()

  override def requiresLayout(): Boolean = false
}
