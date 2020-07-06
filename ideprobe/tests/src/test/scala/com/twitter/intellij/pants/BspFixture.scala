package com.twitter.intellij.pants

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.Shell.CommandResult

trait BspFixture {
  lazy val coursierPath: Path = {
    val destination = Paths.get(System.getProperty("java.io.tmpdir"), "ideprobe-coursier")
    if (!Files.exists(destination)) {
      val url = new URL("https://git.io/coursier-cli")
      Files.copy(url.openConnection.getInputStream, destination)
      destination.toFile.setExecutable(true)
    }
    destination
  }

  def runFastpassCreate(config: Config, workspace: Path, targets: Seq[String]): Path = {
    val version = runFastpass(config, workspace, Seq("--version")).out
    println(s"Using fastpass version $version")
    val command = Seq(
      "create",
      "--no-bloop-exit",
      "--intellij",
      "--intellijLauncher", "echo"
    ) ++ targets
    Paths.get(runFastpass(config, workspace, command).out)
  }

  def runFastpass(config: Config, workspace: Path, args: Seq[String]): CommandResult = {
    val fastpassVersion = config.get[String]("fastpass.version").getOrElse("latest.stable")
    val command = Seq(
      coursierPath.toString,
      "launch",
      s"org.scalameta:fastpass_2.12:$fastpassVersion",
      "-r", "sonatype:snapshots",
      "--main", "scala.meta.fastpass.Fastpass",
      "--"
    ) ++ args
    Shell.run(workspace, command: _*)
  }

}
