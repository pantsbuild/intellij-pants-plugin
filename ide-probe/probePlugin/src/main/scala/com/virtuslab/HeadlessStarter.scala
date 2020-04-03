package com.twitter

import java.nio.file.Path
import java.nio.file.Paths
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.twitter.handlers.IntelliJApi

/**
 * Allows opening a project in headless mode
 */
final class HeadlessStarter extends ApplicationStarter with IntelliJApi {
  private val LOG = Logger.getInstance(s"$getClass")
  private val CommandName = "headless"

  override def getCommandName: String = CommandName
  override def isHeadless: Boolean = true

  override def main(args: Array[String]): Unit = {
    args match {
      case Array(CommandName, arg) =>
        val file = projectPath(arg)
        open(file)
      case Array(CommandName) =>
        LOG.info("No projects to open")
      case args =>
        LOG.error(s"Invalid arguments: [${args.mkString(" ")}]. Expected [$CommandName project-path]")
    }
  }

  private def open(file: Path): Unit = {
    val openProjectOptions = new OpenProjectTask()
    openProjectOptions.checkDirectoryForFileBasedProjects = false

    val project = ProjectUtil.openOrImport(file, openProjectOptions)
    if (project == null) {
      error(s"Could not open project: $file")
    }
  }

  private def projectPath(arg: String) = {
    val currentDirectory = System.getenv("IDEA_INITIAL_DIRECTORY")
    val path = Paths.get(arg)
    if (path.isAbsolute) {
      path
    } else if (currentDirectory == null) {
      path.toAbsolutePath
    } else {
      Paths.get(currentDirectory).resolve(path)
    }
  }
}
