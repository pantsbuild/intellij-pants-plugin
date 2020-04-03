package com.twitter.ideprobe.protocol

import java.nio.file.Path
import com.twitter.ideprobe.jsonrpc.JsonRpc.Method.Notification
import com.twitter.ideprobe.jsonrpc.JsonRpc.Method.Request
import com.twitter.ideprobe.jsonrpc.PayloadJsonFormat._
import pureconfig.generic.auto._

object Endpoints {
  val AwaitNotification = Request[String, IdeNotification]("notification/await")
  val PID = Request[Unit, Long]("pid")
  val Ping = Request[Unit, Unit]("ping")
  val Shutdown = Notification[Unit]("shutdown")
  val Plugins = Request[Unit, Seq[InstalledPlugin]]("plugins")
  val OpenProject = Request[Path, ProjectRef]("project/open")
  val CloseProject = Request[ProjectRef, Unit]("project/close")
  val FileReferences = Request[FileRef, Seq[Reference]]("file/references")
  val Messages = Request[Unit, Seq[IdeMessage]]("messages")
  val InvokeActionAsync = Request[String, Unit]("action/invokeAsync")
  val InvokeAction = Request[String, Unit]("action/invoke")
  val Freezes = Request[Unit, Seq[Freeze]]("freezes")
  val ModuleSdk = Request[ModuleRef, Option[String]]("module/sdk")
  val ProjectSdk = Request[ProjectRef, Option[String]]("project/sdk")
  val ProjectModel = Request[ProjectRef, Project]("project/model")
  val AwaitIdle = Request[Unit, Unit]("awaitIdle")
  val Build = Request[BuildParams, BuildResult]("build")
  val Run = Request[RunConfiguration, ProcessResult]("run")
  val SyncFiles = Request[Unit, Unit]("fs/sync")
  val Find = Request[NavigationQuery, List[NavigationTarget]]("find")
}
