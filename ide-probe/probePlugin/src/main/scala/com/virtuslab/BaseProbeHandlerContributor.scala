package com.twitter

import com.twitter.handlers.Actions
import com.twitter.handlers.App
import com.twitter.handlers.BackgroundTasks
import com.twitter.handlers.Builds
import com.twitter.handlers.Freezes
import com.twitter.handlers.IdeMessages
import com.twitter.handlers.Modules
import com.twitter.handlers.Navigation
import com.twitter.handlers.Notifications
import com.twitter.handlers.PSI
import com.twitter.handlers.Plugins
import com.twitter.handlers.Projects
import com.twitter.handlers.Run
import com.twitter.handlers.VFS
import com.twitter.ideprobe.jsonrpc.JsonRpc
import com.twitter.ideprobe.protocol.Endpoints
import scala.concurrent.ExecutionContext

class BaseProbeHandlerContributor extends ProbeHandlerContributor {
  private implicit val ec: ExecutionContext = IdeProbeService.executionContext

  override def registerHandlers(handler: JsonRpc.Handler): JsonRpc.Handler = {
    handler
      .on(Endpoints.PID)(_ => App.pid)
      .on(Endpoints.Ping)(_ => ())
      .on(Endpoints.Plugins)(_ => Plugins.list)
      .on(Endpoints.Shutdown)(_ => App.shutdown())
      .on(Endpoints.Messages)(_ => IdeMessages.list)
      .on(Endpoints.Freezes)(_ => Freezes.list)
      .on(Endpoints.InvokeAction)(Actions.invoke)
      .on(Endpoints.InvokeActionAsync)(Actions.invokeAsync)
      .on(Endpoints.FileReferences)(PSI.references)
      .on(Endpoints.Find)(Navigation.find)
      .on(Endpoints.OpenProject)(Projects.open)
      .on(Endpoints.CloseProject)(Projects.close)
      .on(Endpoints.ProjectModel)(Projects.model)
      .on(Endpoints.ProjectSdk)(Projects.sdk)
      .on(Endpoints.ModuleSdk)(Modules.sdk)
      .on(Endpoints.AwaitIdle)(_ => BackgroundTasks.awaitNone())
      .on(Endpoints.Build)(Builds.build)
      .on(Endpoints.SyncFiles)(_ => VFS.syncAll())
      .on(Endpoints.AwaitNotification)(Notifications.await)
      .on(Endpoints.Run)(Run.run)
  }
}
