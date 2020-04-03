package com.twitter.handlers

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTaskManager
import com.intellij.util.messages.MessageBusConnection
import com.twitter.ideprobe.protocol.BuildMessage
import com.twitter.ideprobe.protocol.BuildParams
import com.twitter.ideprobe.protocol.BuildResult
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

object Builds extends IntelliJApi {
  def build(params: BuildParams): BuildResult = {
    val project = Projects.resolve(params.scope.project)

    val collector = new BuildResultCollector
    val connection = subscribeForBuildResult(project, collector)
    try {
      BackgroundTasks.withAwaitNone {
        val taskManager = ProjectTaskManager.getInstance(project)
        val promise = if (params.scope.files.nonEmpty) {
          buildFiles(params, project, taskManager)
        } else if (params.scope.modules.nonEmpty) {
          buildModules(params, project, taskManager)
        } else {
          buildProject(params, taskManager)
        }
        promise.blockingGet(4, TimeUnit.HOURS)
        Await.result(collector.buildResult, FiniteDuration(4, TimeUnit.HOURS))
      }
    } finally {
      connection.disconnect()
    }
  }

  private def buildProject(params: BuildParams, taskManager: ProjectTaskManager) = {
    if (params.rebuild) {
      taskManager.rebuildAllModules()
    } else {
      taskManager.buildAllModules()
    }
  }

  private def buildModules(params: BuildParams, project: Project, taskManager: ProjectTaskManager) = {
    val allModules = ModuleManager.getInstance(project).getModules
    val modulesToBuild = allModules.filter(module => params.scope.modules.contains(module.getName))
    if (params.rebuild) {
      taskManager.rebuild(modulesToBuild: _*)
    } else {
      taskManager.build(modulesToBuild: _*)
    }
  }

  private def buildFiles(params: BuildParams, project: Project, taskManager: ProjectTaskManager) = {
    val root = Paths.get(project.getBasePath)
    val files = params.scope.files.map { file =>
      val path = Paths.get(file)
      val absolute = if (path.isAbsolute) path else root.resolve(path)
      VFS.toVirtualFile(absolute)
    }
    taskManager.compile(files: _*)
  }

  private def subscribeForBuildResult(project: Project, listener: CompilationStatusListener): MessageBusConnection = {
    val connection = project.getMessageBus.connect()
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener)
    connection
  }

  class BuildResultCollector extends CompilationStatusListener {
    private val promise = Promise[BuildResult]()

    def buildResult: Future[BuildResult] = {
      promise.future
    }

    override def compilationFinished(
        aborted: Boolean,
        errors: Int,
        warnings: Int,
        compileContext: CompileContext
    ): Unit = {
      def messages(category: CompilerMessageCategory): Seq[BuildMessage] = {
        compileContext.getMessages(category).map { msg =>
          BuildMessage(Option(msg.getVirtualFile).map(_.toString), msg.getMessage)
        }
      }

      val buildResult = BuildResult(
        aborted,
        messages(CompilerMessageCategory.ERROR),
        messages(CompilerMessageCategory.WARNING),
        messages(CompilerMessageCategory.INFORMATION),
        messages(CompilerMessageCategory.STATISTICS)
      )

      promise.success(buildResult)
    }
  }
}
