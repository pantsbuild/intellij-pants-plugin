package com.twitter.intellij.pants.probe.handlers

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.twitter.intellij.pants.protocol
import com.twitter.intellij.pants.settings.PantsProjectSettings
import com.twitter.intellij.pants.settings.{PantsSettings => PantsSettingsFromPlugin}
import com.twitter.intellij.pants.util.PantsConstants
import com.twitter.handlers.BackgroundTasks
import com.twitter.handlers.Projects
import com.twitter.ideprobe.protocol.ProjectRef
import com.twitter.ideprobe.protocol.Setting
import scala.jdk.CollectionConverters._

object PantsSettings {
  def getProjectSettings(ref: ProjectRef): protocol.PantsProjectSettings = {
    val project = Projects.resolve(ref)
    val pantsSettings = getPantsSettings(project)
    val linked = pantsSettings.getLinkedProjectsSettings.asScala

    protocol.PantsProjectSettings(
      selectedTargets = linked.flatMap(_.getSelectedTargetSpecs.asScala).toSeq,
      loadSourcesAndDocsForLibs = linked.exists(_.libsWithSources),
      incrementalProjectImport = pantsSettings.isEnableIncrementalImport,
      useIdeaProjectJdk = pantsSettings.isUseIdeaProjectJdk,
      importSourceDepsAsJars = linked.exists(_.importSourceDepsAsJars),
      useIntellijCompiler = linked.exists(_.useIntellijCompiler)
    )
  }

  def changeProjectSettings(ref: ProjectRef, toSet: protocol.PantsProjectSettingsChangeRequest): Unit =
    BackgroundTasks.withAwaitNone {
      val project = Projects.resolve(ref)
      val pantsSettings = getPantsSettings(project)

      def setLinkedSetting[A](setting: Setting[A])(f: (PantsProjectSettings, A) => Unit): Unit = {
        setting.foreach(value => pantsSettings.getLinkedProjectsSettings.forEach(f(_, value)))
      }

      def setSetting[A](setting: Setting[A])(f: (PantsSettingsFromPlugin, A) => Unit): Unit = {
        setting.foreach(value => f(pantsSettings, value))
      }

      setSetting(toSet.useIdeaProjectJdk)(_.setUseIdeaProjectJdk(_))
      setSetting(toSet.incrementalProjectImport)(_.setEnableIncrementalImport(_))
      setLinkedSetting(toSet.useIntellijCompiler)(_.useIntellijCompiler = _)
      setLinkedSetting(toSet.importSourceDepsAsJars)(_.importSourceDepsAsJars = _)
      setLinkedSetting(toSet.loadSourcesAndDocsForLibs)(_.libsWithSources = _)
      setLinkedSetting(toSet.selectedTargets.map(_.asJava))(_.setSelectedTargetSpecs(_))
    }

  private def getPantsSettings(project: Project) = {
    ExternalSystemApiUtil.getSettings(project, PantsConstants.SYSTEM_ID).asInstanceOf[PantsSettingsFromPlugin]
  }
}
