package com.twitter.handlers

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.twitter.ideprobe.protocol.ModuleRef

object Modules extends IntelliJApi {
  def resolve(module: ModuleRef): (Project, Module) = {
    val project = Projects.resolve(module.project)
    val modules = ModuleManager.getInstance(project).getModules

    modules.find(_.getName == module.name) match {
      case Some(module) =>
        (project, module)
      case None =>
        val helpMessage =
          if (modules.isEmpty) "There are no open modules"
          else s"Available modules are: ${modules.map(_.getName).mkString(",")}"

        error(s"Could not find module [${module.name}] inside project [${module.name}]. $helpMessage")

    }
  }

  def sdk(moduleRef: ModuleRef): Option[String] = {
    val (_, module) = resolve(moduleRef)
    val sdk = ModuleRootManager.getInstance(module).getSdk
    Option(sdk).map(_.getName)
  }
}
