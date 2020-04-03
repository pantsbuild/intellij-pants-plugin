package com.twitter.intellij.pants.protocol

import java.nio.file.Path
import com.twitter.ideprobe.jsonrpc.JsonRpc.Method.Request
import com.twitter.ideprobe.protocol.ProjectRef
import com.twitter.ideprobe.jsonrpc.PayloadJsonFormat._
import pureconfig.generic.auto._

object PantsEndpoints {
  val ImportPantsProject = Request[(Path, PantsProjectSettingsChangeRequest), ProjectRef]("pants/project/import")

  val GetPantsProjectSettings = Request[ProjectRef, PantsProjectSettings]("pants/project/settings/get")

  val ChangePantsProjectSettings =
    Request[(ProjectRef, PantsProjectSettingsChangeRequest), Unit]("pants/project/settings/change")

}
