package com.twitter.ideprobe

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Path
import ammonite.ops.CommandResult
import com.zaxxer.nuprocess.NuAbstractProcessHandler
import scala.concurrent.Future
import scala.concurrent.Promise

object Shell {
  class ProcessOutputLogger extends NuAbstractProcessHandler {
    private val outputChannel = Channels.newChannel(System.out)
    private val errorChannel = Channels.newChannel(System.err)

    override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
      outputChannel.write(buffer)
    }

    override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
      errorChannel.write(buffer)
    }
  }

  def apply(command: String*): Future[Int] = {
    import com.zaxxer.nuprocess._
    val builder = new NuProcessBuilder(command: _*)
    val finished = Promise[Int]()
    builder.setProcessListener(new NuAbstractProcessHandler {
      val next = new ProcessOutputLogger
      override def onExit(statusCode: Int): Unit = {
        finished.success(statusCode)
      }

      override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
        next.onStderr(buffer, closed)
      }

      override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
        next.onStdout(buffer, closed)
      }
    })

    builder.start()
    finished.future
  }

  def run(command: String*): CommandResult = {
    import ammonite.ops._
    %%(command)(pwd)
  }

  def run(in: Path, command: String*): CommandResult = {
    import ammonite.ops._
    %%(command)(os.Path(in))
  }
}
