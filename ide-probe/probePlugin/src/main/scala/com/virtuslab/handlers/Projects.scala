package com.twitter.handlers

import java.nio.file.Path
import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.projectImport.ProjectImportProvider
import com.twitter.ideprobe.protocol
import com.twitter.ideprobe.protocol.ProjectRef
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object Projects extends IntelliJApi {

  def resolve(ref: ProjectRef): Project = {
    ref match {
      case ProjectRef.Default      => current
      case ProjectRef.ByName(name) => findProject(name)
    }
  }

  def current: Project = {
    ProjectManager.getInstance.getOpenProjects.toList match {
      case Nil          => error("No open projects")
      case List(single) => single
      case multiple     => error(s"Ambiguous Projects.current call, multiple projects open: $multiple")
    }
  }

  def open(path: Path): ProjectRef = {
    var project: Option[Project] = None
    BackgroundTasks.withAwaitNone {
      runOnUISync {
        write {
          project = Option(ProjectUtil.openOrImport(path, null, false))
        }
      }
    }
    // project name can change after call to openOrImport during sync
    project.map(toRef).getOrElse(error(s"Could not load project '$path'"))
  }

  def importFromSources(path: Path, handleStep: PartialFunction[Any, Unit]): ProjectRef = {
    val wizard = runOnUISync {
      write {
        val providers = ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions.filter(_.canCreateNewProject)
        val wizard = ImportModuleAction.createImportWizard(null, null, VFS.toVirtualFile(path), providers: _*)

        @tailrec def goThroughWizard(): Unit = {
          val step = wizard.getCurrentStepObject
          handleStep.applyOrElse(step, (_: Any) => ())
          if (wizard.isLast) {
            wizard.doFinishAction()
          } else {
            wizard.doNextAction()
            if (step == wizard.getCurrentStepObject) {
              error(s"Failed to proceed in project import wizard step $step")
            }
            goThroughWizard()
          }
        }

        goThroughWizard()
        wizard
      }
    }
    val modules = BackgroundTasks.withAwaitNone {
      runOnUISync(ImportModuleAction.createFromWizard(null, wizard))
    }
    val project = modules.asScala.headOption.map(_.getProject).getOrElse(Projects.current)
    toRef(project)
  }

  def close(ref: ProjectRef): Unit = {
    val project = resolve(ref)
    runOnUISync {
      ProjectUtil.closeAndDispose(project)
    }
  }

  def model(ref: ProjectRef): protocol.Project = read {
    val project = resolve(ref)
    val modules = ModuleManager.getInstance(project).getSortedModules
    val mappedModules = modules.map { module =>
      val roots = ModuleRootManager.getInstance(module).getContentRoots.map(_.getPath)
      protocol.Module(module.getName, roots, Option(module.getModuleTypeName))
    }

    protocol.Project(project.getName, project.getBasePath, mappedModules)
  }

  def sdk(ref: ProjectRef): Option[String] = read {
    val project = resolve(ref)
    val sdk = ProjectRootManager.getInstance(project).getProjectSdk
    Option(sdk).map(_.getName)
  }

  private def findProject(name: String): Project = {
    val projects = ProjectManager.getInstance.getOpenProjects
    projects
      .find(p => p.getName == name)
      .getOrElse {
        val openProjects =
          if (projects.isEmpty) "There are no open projects"
          else s"Currently open projects are: ${projects.map(_.getName).mkString(", ")}"

        error(s"Could not find project $name. $openProjects")
      }
  }

  private def toRef(project: Project): ProjectRef = ProjectRef(project.getName)
}
