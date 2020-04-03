package com.twitter.ideprobe.ide.intellij

import java.io.File
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.file.Path
import com.twitter.ideprobe.jsonrpc.JsonRpcConnection
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.ProbeDriver
import com.twitter.ideprobe.Shell
import com.twitter.ideprobe.jsonrpc.JsonRpcConnection
import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcessBuilder
import scala.concurrent.ExecutionContext
import scala.concurrent.blocking

final class InstalledIntelliJ(val root: Path, config: DriverConfig) {
  val paths: IntelliJPaths = new IntelliJPaths(root, config.headless)

  private val vmoptions: Path = {
    val baseVMOptions = Seq(
      s"-Djava.awt.headless=${config.headless}",
      DebugMode.vmOption
    )

    val vmOptions = baseVMOptions ++ config.vmOptions

    val content = vmOptions.mkString("\n")

    root.resolve("bin/ideprobe.vmoptions").write(content)
  }

  private val ideaProperties: Path = {
    val content = s"""|idea.config.path=${paths.config}
                      |idea.system.path=${paths.system}
                      |idea.plugins.path=${paths.plugins}
                      |idea.log.path=${paths.logs}
                      |java.util.prefs.userRoot=${paths.userPrefs}
                      |""".stripMargin

    root.resolve("bin/idea.properties").write(content)
  }

  def startIn(workingDir: Path)(implicit ec: ExecutionContext): RunningIde = {
    val server = new ServerSocket(0)

    val launcher = startProcess(workingDir, server)
    try {
      server.setSoTimeout(config.launch.timeout.toMillis.toInt)
      val socket = blocking(server.accept()) // will be closed along with the connection by ProbeDriver
      val connection = JsonRpcConnection.from(socket)
      val driver = ProbeDriver.start(connection)

      new RunningIde(launcher, driver.pid(), driver)
    } catch {
      case cause: Exception =>
        launcher.destroy(true)
        throw cause
    } finally {
      // we only need the server to establish the initial connection
      server.close()
    }
  }

  private def startProcess(workingDir: Path, server: ServerSocket) = {
    val command = config.launch.command.toList match {
      case Nil =>
        List(paths.executable.toString)
      case "idea" :: tail =>
        paths.executable.toString :: tail
      case nonEmpty =>
        nonEmpty
    }

    val environment = {
      val PATH = List(paths.bin, System.getenv("PATH"))
        .mkString(File.pathSeparator)
      Map(
        "IDEA_VM_OPTIONS" -> vmoptions.toString,
        "IDEA_PROPERTIES" -> ideaProperties.toString,
        "IDEPROBE_DRIVER_PORT" -> server.getLocalPort.toString,
        "PATH" -> PATH
      )
    }

    val builder = new NuProcessBuilder(command.asJava)

    builder.setCwd(workingDir)
    builder.setProcessListener(new Shell.ProcessOutputLogger)
    builder.environment().putAll(environment.asJava)

    println(s"Starting process ${command.mkString(" ")} in $workingDir")

    val processHandler = new NuAbstractProcessHandler {
      override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
        if (!closed || buffer.remaining > 0)
          printBuffer(buffer, "intellij-stderr")
      }

      override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
        if (!closed || buffer.remaining > 0)
          printBuffer(buffer, "intellij-stdout")
      }

      private def printBuffer(buffer: ByteBuffer, tag: String) = {
        val bytes = new Array[Byte](buffer.remaining)
        buffer.get(bytes);
        val output = new String(bytes)
        val lines = output.split("\n")
        lines.foreach(line => println(s"[${tag}] ${line}"))
      }
    }
    builder.setProcessListener(processHandler)

    builder.start()
  }
}
