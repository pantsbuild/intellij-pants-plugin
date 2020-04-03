package com.twitter.ideprobe

import java.nio.file.Path
import com.twitter.ideprobe.jsonrpc.JsonRpc.Handler
import com.twitter.ideprobe.jsonrpc.JsonRpcConnection
import com.twitter.ideprobe.jsonrpc.JsonRpcEndpoint
import com.twitter.ideprobe.jsonrpc.JsonRpc
import com.twitter.ideprobe.jsonrpc.JsonRpc.Method
import com.twitter.ideprobe.jsonrpc.JsonRpcConnection
import com.twitter.ideprobe.jsonrpc.JsonRpcEndpoint
import com.twitter.ideprobe.protocol.BuildParams
import com.twitter.ideprobe.protocol.BuildResult
import com.twitter.ideprobe.protocol.BuildScope
import com.twitter.ideprobe.protocol.Endpoints
import com.twitter.ideprobe.protocol.FileRef
import com.twitter.ideprobe.protocol.Freeze
import com.twitter.ideprobe.protocol.IdeMessage
import com.twitter.ideprobe.protocol.IdeNotification
import com.twitter.ideprobe.protocol.InstalledPlugin
import com.twitter.ideprobe.protocol.ModuleRef
import com.twitter.ideprobe.protocol.NavigationQuery
import com.twitter.ideprobe.protocol.NavigationTarget
import com.twitter.ideprobe.protocol.ProcessResult
import com.twitter.ideprobe.protocol.Project
import com.twitter.ideprobe.protocol.ProjectRef
import com.twitter.ideprobe.protocol.Reference
import com.twitter.ideprobe.protocol.RunConfiguration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.reflect.ClassTag

final class ProbeDriver(protected val connection: JsonRpcConnection)(implicit protected val ec: ExecutionContext)
    extends JsonRpcEndpoint {
  protected val handler: Handler = new JsonRpc.Handler

  def pid(): Long = send(Endpoints.PID)

  def openProject(path: Path): ProjectRef = send(Endpoints.OpenProject, path)

  def closeProject(name: ProjectRef = ProjectRef.Default): Unit = send(Endpoints.CloseProject, name)

  def ping(): Unit = send(Endpoints.Ping)

  def plugins: Seq[InstalledPlugin] = send(Endpoints.Plugins).toList

  def shutdown(): Unit = send(Endpoints.Shutdown)

  def invokeActionAsync(id: String): Unit = send(Endpoints.InvokeActionAsync, id)

  def invokeAction(id: String): Unit = send(Endpoints.InvokeAction, id)

  def fileReferences(project: ProjectRef = ProjectRef.Default, path: String): Seq[Reference] = {
    send(Endpoints.FileReferences, FileRef(project, path))
  }

  def find(query: NavigationQuery): List[NavigationTarget] = {
    send(Endpoints.Find, query)
  }

  def errors: Seq[IdeMessage] = send(Endpoints.Messages).filter(_.isError).toList

  def warnings: Seq[IdeMessage] = send(Endpoints.Messages).filter(_.isWarn).toList

  def messages: Seq[IdeMessage] = send(Endpoints.Messages).toList

  def projectModel(name: ProjectRef = ProjectRef.Default): Project = send(Endpoints.ProjectModel, name)

  def awaitIdle(): Unit = send(Endpoints.AwaitIdle)

  def syncFiles(): Unit = send(Endpoints.SyncFiles)

  def freezes: Seq[Freeze] = send(Endpoints.Freezes)

  def build(scope: BuildScope = BuildScope.project): BuildResult = build(BuildParams(scope, rebuild = false))

  def rebuild(scope: BuildScope = BuildScope.project): BuildResult = build(BuildParams(scope, rebuild = true))

  private def build(params: BuildParams): BuildResult = send(Endpoints.Build, params)

  def awaitNotification(title: String): IdeNotification = send(Endpoints.AwaitNotification, title)

  def run(mainClass: RunConfiguration): ProcessResult = send(Endpoints.Run, mainClass)

  def projectSdk(project: ProjectRef = ProjectRef.Default): Option[String] = send(Endpoints.ProjectSdk, project)

  def moduleSdk(module: ModuleRef): Option[String] = send(Endpoints.ModuleSdk, module)

  def as[A](extensionPluginId: String, convert: ProbeDriver => A): A = {
    val isLoaded = plugins.exists(_.id == extensionPluginId)
    if (isLoaded) convert(this)
    else throw new IllegalStateException(s"Extension plugin $extensionPluginId is not loaded")
  }

  def send[T: ClassTag, R: ClassTag](method: Method[T, R], parameters: T): R = {
    Await.result(sendRequest(method, parameters), 4.hours)
  }

  def send[R: ClassTag](method: Method[Unit, R]): R = {
    send(method, ())
  }
}

object ProbeDriver {
  def start(connection: JsonRpcConnection)(implicit ec: ExecutionContext): ProbeDriver = {
    import scala.concurrent.Future
    val driver = new ProbeDriver(connection)
    Future(driver.listen).onComplete(_ => driver.close())
    driver
  }
}
