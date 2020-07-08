package com.twitter.intellij.pants

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.dependencies.Hash
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

import scala.annotation.tailrec
import scala.collection.mutable

object PantsSetup extends ConfigFormat {

  sealed trait Git {
    def path: String
    def ref: Option[String]
  }

  case class GitDefault(path: String) extends Git {
    override def ref: Option[String] = None
  }

  case class GitBranch(path: String, branch: String) extends Git {
    def ref: Option[String] = Some(branch)
  }

  case class GitTag(path: String, tag: String) extends Git {
    def ref: Option[String] = Some(tag)
  }

  case class GitCommit(path: String, commit: String) extends Git {
    def ref: Option[String] = Some(commit)
  }

  implicit val gitConfigReader: ConfigReader[Git] = {
    possiblyAmbiguousAdtReader[Git](
      deriveReader[GitDefault],
      deriveReader[GitBranch],
      deriveReader[GitTag],
      deriveReader[GitCommit]
    )
  }

  def overridePantsVersion(fixture: IntelliJFixture, workspace: Path): Unit = {
    fixture.config.get[String]("pants.version").foreach { version =>
      setupFromVersionString(workspace, version)
    }
    fixture.config.get[Git]("pants.version").foreach { git =>
      setupFromSources(workspace, git)
    }
  }

  private val pantsSourcesCache: mutable.Map[String, Path] = mutable.Map.empty

  private def setupFromSources(workspace: Path, git: Git): Unit = {
    val hash = Hash.md5(git.path + ":" + git.ref.getOrElse(""))
    val targetPath = synchronized { pantsSourcesCache.getOrElseUpdate(hash, setup(git, hash)) }
    val pantsScriptPath = targetPath.resolve("pants")
    val originalPants = findPants(workspace, workspace)
    originalPants.write(s"""#!/usr/bin/env bash
                          |${pantsScriptPath.toAbsolutePath} $$*
                          |""".stripMargin)
  }

  private def setup(git: Git, hash: String): Path = {
    val targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "ideprobe-pants-from-src", hash)
    if (Files.notExists(targetPath)) {
      val cloned = Shell.run("git", "clone", git.path, targetPath.toString)
      if (cloned.exitCode != 0) throw new IllegalStateException(s"Could not clone git ${git.path}")
      git.ref.foreach { ref =>
        val checkout = Shell.run(in = targetPath, "git", "checkout", ref)
        if (checkout.exitCode != 0) throw new IllegalStateException(s"Could not checkout $ref in ${git.path}")
      }
    }
    targetPath
  }

  private def setupFromVersionString(workspace: Path, version: String) = {
    val pantsPath = findPants(workspace, workspace)
    val pantsIni = pantsPath.resolveSibling("pants.ini")
    val versionLine = s"""pants_version: $version"""
    val versionBlock = s"""[GLOBAL]\n$versionLine\n"""
    if (Files.exists(pantsIni)) {
      val content = pantsIni.content()
      val versionRegex = """pants_version: [\w.]+""".r
      val newContent = if (versionRegex.unanchored.pattern.matcher(content).matches()) {
        versionRegex.replaceAllIn(content, versionLine)
      } else {
        versionBlock + content
      }
      pantsIni.write(newContent)
    } else {

      pantsIni.write(versionBlock)
    }
  }

  @tailrec private def findPants(directory: Path, origin: Path): Path = {
    if (directory == null) {
      throw new RuntimeException(s"Failed to find 'pants' executable starting from $origin")
    }

    val pantsPath = directory.resolve("pants")
    if (Files.exists(pantsPath)) {
      pantsPath
    } else {
      findPants(directory.getParent, origin)
    }
  }

}
