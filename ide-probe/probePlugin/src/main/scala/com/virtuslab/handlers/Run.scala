package com.twitter.handlers

import java.util.UUID
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.JavaPsiFacade
import com.twitter.idea.RunnerSettingsWithProcessOutput
import com.twitter.ideprobe.protocol.ProcessResult
import com.twitter.ideprobe.protocol.RunConfiguration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

object Run extends IntelliJApi {
  def run(mainClass: RunConfiguration)(implicit ec: ExecutionContext): ProcessResult = {
    val configuration = registerObservableConfiguration(mainClass)

    val project = Projects.resolve(mainClass.module.project)
    launch(project, configuration)
    await(configuration.processResult())
  }

  private def launch(project: Project, configuration: RunnerSettingsWithProcessOutput): Unit = {
    val environment = ExecutionUtil
      .createEnvironment(new DefaultRunExecutor, configuration)
      .activeTarget()
      .build()

    ExecutionManager.getInstance(project).restartRunProfile(environment)
  }

  private def registerObservableConfiguration(mainClass: RunConfiguration): RunnerSettingsWithProcessOutput = {
    val (project, module) = Modules.resolve(mainClass.module)

    val configuration = {
      val psiClass = {
        import com.intellij.psi.search.GlobalSearchScope._
        val scope = moduleWithDependenciesScope(module)
        DumbService.getInstance(project).waitForSmartMode()
        JavaPsiFacade.getInstance(project).findClass(mainClass.fqn, scope)
      }

      val name = UUID.randomUUID()
      val configuration = new ApplicationConfiguration(name.toString, project)
      configuration.setMainClass(psiClass)
      configuration
    }

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)
    RunManager.getInstance(project).addConfiguration(settings)

    new RunnerSettingsWithProcessOutput(settings)
  }

  final class ProcessTerminationListener(environment: ExecutionEnvironment) extends ExecutionListener with Disposable {
    private val terminated = Promise[Int]()

    def exitCode: Future[Int] = this.terminated.future

    override def processNotStarted(id: String, env: ExecutionEnvironment): Unit = {
      if (env == environment) {
        terminated.tryFailure(new IllegalStateException("Process not started"))
        Disposer.dispose(this)
      }
    }

    override def processTerminated(
        id: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ): Unit = {
      if (env == environment) {
        this.terminated.trySuccess(exitCode)
        Disposer.dispose(this)
      }
    }

    override def dispose(): Unit = {
      terminated.tryFailure(new IllegalStateException("Listener disposed"))
    }
  }

}
