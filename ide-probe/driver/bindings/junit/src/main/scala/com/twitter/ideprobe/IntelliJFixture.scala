package com.twitter.ideprobe

import java.nio.file.Files
import java.nio.file.Path
import com.twitter.ideprobe.dependencies.IntelliJVersion
import com.twitter.ideprobe.dependencies.Plugin
import com.twitter.ideprobe.ide.intellij.DriverConfig
import com.twitter.ideprobe.ide.intellij.InstalledIntelliJ
import com.twitter.ideprobe.ide.intellij.IntelliJFactory
import com.twitter.ideprobe.ide.intellij.RunningIde
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.dependencies.IntelliJVersion
import com.twitter.ideprobe.dependencies.Plugin
import com.twitter.ideprobe.ide.intellij.DriverConfig
import com.twitter.ideprobe.ide.intellij.InstalledIntelliJ
import com.twitter.ideprobe.ide.intellij.IntelliJFactory
import com.twitter.ideprobe.ide.intellij.RunningIde
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final class IntelliJFixture(
    workspaceTemplate: WorkspaceTemplate,
    factory: IntelliJFactory,
    version: IntelliJVersion,
    val plugins: Seq[Plugin],
    val config: Config
)(implicit ec: ExecutionContext) {

  def withDisplay(): IntelliJFixture = {
    copy(driverConfig = factory.config.copy(headless = false))
  }

  def copy(
      workspaceTemplate: WorkspaceTemplate = workspaceTemplate,
      plugins: Seq[Plugin] = plugins,
      config: Config = config,
      driverConfig: DriverConfig = factory.config
  ): IntelliJFixture = {
    new IntelliJFixture(workspaceTemplate, factory.withConfig(driverConfig), version, plugins, config)
  }

  def rule = new IntelliJRule(this)

  def run = new SingleRunIntelliJ(this)

  def withWorkspace = new MultipleRunsIntelliJ(this)

  def setupWorkspace(): Path = {
    val workspace = Files.createTempDirectory("ideprobe-workspace")
    workspaceTemplate.setupIn(workspace)
    workspace
  }

  def deleteWorkspace(workspace: Path): Unit = {
    workspace.delete()
  }

  def installIntelliJ(): InstalledIntelliJ = {
    factory.create(version, plugins)
  }

  def deleteIntelliJ(installedIntelliJ: InstalledIntelliJ): Unit = {
    withRetries(maxRetries = 10)(installedIntelliJ.root.delete())
  }

  def startIntelliJ(workspace: Path, installedIntelliJ: InstalledIntelliJ): RunningIde = {
    val runningIde = installedIntelliJ.startIn(workspace)
    val probe = runningIde.probe
    probe.awaitIdle()
    Runtime.getRuntime.addShutdownHook(new Thread(() => runningIde.shutdown()))
    runningIde
  }

  def closeIntellij(runningIde: RunningIde): Unit = {
    runningIde.shutdown()
  }

  @tailrec
  private def withRetries(maxRetries: Int, delay: FiniteDuration = 1.second)(block: => Unit): Unit = {
    try block
    catch {
      case e: Exception =>
        if (maxRetries == 0) throw e
        else {
          Thread.sleep(delay.toMillis)
          withRetries(maxRetries - 1)(block)
        }
    }
  }

}

object IntelliJFixture {
  private val ConfigRoot = "probe"

  def apply(
      workspaceTemplate: WorkspaceTemplate = WorkspaceTemplate.Empty,
      version: IntelliJVersion = IntelliJVersion.V2019_3_1,
      intelliJFactory: IntelliJFactory = IntelliJFactory.Default,
      plugins: Seq[Plugin] = Seq.empty,
      environment: Config = Config.Empty
  )(implicit ec: ExecutionContext): IntelliJFixture = {
    new IntelliJFixture(workspaceTemplate, intelliJFactory, version, plugins, environment)
  }

  def fromConfig(config: Config, path: String = ConfigRoot)(implicit ec: ExecutionContext): IntelliJFixture = {
    val probeConfig = config[IdeProbeConfig](path)
    new IntelliJFixture(
      workspaceTemplate = probeConfig.workspace.map(WorkspaceTemplate.from).getOrElse(WorkspaceTemplate.Empty),
      factory = IntelliJFactory.from(probeConfig.resolvers, probeConfig.driver),
      version = probeConfig.intellij.version,
      plugins = probeConfig.intellij.plugins,
      config = config
    )
  }

}
