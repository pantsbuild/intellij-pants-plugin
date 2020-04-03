package com.twitter.ideprobe.ide.intellij

import java.nio.file.Path
import com.twitter.ideprobe.Extensions._

final class IntelliJPaths(root: Path, headless: Boolean) {
  val config: Path = root.createDirectory("config")
  val system: Path = root.createDirectory("system")
  val plugins: Path = root.createDirectory("plugins")
  val logs: Path = system.createDirectory("logs")
  val bin: Path = root.resolve("bin")
  val userPrefs: Path = {
    val path = root.resolve("prefs")
    IntellijPrivacyPolicy.installAgreementIn(path)
    path
  }

  val executable: Path = {
    val content = {
      val launcher = bin.resolve("idea.sh").makeExecutable()

      val command =
        if (headless) s"$launcher headless"
        else {
          Display.Mode match {
            case Display.Native => s"$launcher"
            case Display.Xvfb   => s"xvfb-run --auto-servernum $launcher"
          }
        }

      s"""|#!/bin/sh
          |$command "$$@"
          |""".stripMargin
    }

    bin
      .resolve("idea")
      .write(content)
      .makeExecutable()
  }
}
