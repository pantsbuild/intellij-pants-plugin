package com.twitter.ideprobe.protocol

import java.nio.file.Path

case class BuildMessage(file: Option[String], content: String)

case class BuildResult(
    isAborted: Boolean,
    errors: Seq[BuildMessage],
    warnings: Seq[BuildMessage],
    infos: Seq[BuildMessage],
    stats: Seq[BuildMessage]
) {
  def hasErrors: Boolean = errors.nonEmpty
}

case class BuildParams(scope: BuildScope, rebuild: Boolean)

case class BuildScope(project: ProjectRef, modules: Seq[String], files: Seq[String])

object BuildScope {
  def project: BuildScope = {
    BuildScope(project = ProjectRef.Default, modules = Nil, files = Nil)
  }

  def project(project: ProjectRef): BuildScope = {
    BuildScope(project = project, modules = Nil, files = Nil)
  }

  def modules(project: ProjectRef, modules: String*): BuildScope = {
    BuildScope(project = project, modules = modules, files = Nil)
  }

  def files(project: ProjectRef, paths: Path*): BuildScope = {
    BuildScope(project = project, modules = Nil, files = paths.map(_.toString))
  }
}
