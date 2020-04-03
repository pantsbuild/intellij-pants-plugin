package com.twitter.ideprobe.protocol

final case class ModuleRef(project: ProjectRef, name: String)

object ModuleRef {
  def apply(project: String, name: String): ModuleRef = {
    ModuleRef(ProjectRef(project), name)
  }
}
