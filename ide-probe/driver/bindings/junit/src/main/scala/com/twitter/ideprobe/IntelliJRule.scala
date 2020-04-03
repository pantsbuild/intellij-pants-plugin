package com.twitter.ideprobe

import java.nio.file.Path
import com.twitter.ideprobe.ide.intellij.InstalledIntelliJ
import com.twitter.ideprobe.ide.intellij.IntelliJPaths
import com.twitter.ideprobe.ide.intellij.RunningIde
import org.junit.rules.ExternalResource

class IntelliJRule(fixture: IntelliJFixture) extends ExternalResource {
  private[ideprobe] var currentWorkspace: Path = _
  private[ideprobe] var installedIntelliJ: InstalledIntelliJ = _
  private[ideprobe] var runningIde: RunningIde = _

  def workspace: Path = currentWorkspace

  def probe: ProbeDriver = runningIde.probe

  def config: Config = fixture.config

  def paths: IntelliJPaths = installedIntelliJ.paths

  override def before(): Unit = {
    currentWorkspace = fixture.setupWorkspace()
    installedIntelliJ = fixture.installIntelliJ()
    runningIde = fixture.startIntelliJ(workspace, installedIntelliJ)
  }

  override def after(): Unit = {
    Option(runningIde).foreach(fixture.closeIntellij)
    Option(installedIntelliJ).foreach(fixture.deleteIntelliJ)
    Option(currentWorkspace).foreach(fixture.deleteWorkspace)
    runningIde = null
    installedIntelliJ = null
    currentWorkspace = null
  }
}
