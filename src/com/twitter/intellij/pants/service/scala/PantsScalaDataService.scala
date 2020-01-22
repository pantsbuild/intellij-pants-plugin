// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala

import java.io.File
import java.util
import java.util.Collections

import com.intellij.ProjectTopics
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.{DataNode, ExternalSystemException, Key, ProjectKeys}
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.{ModuleListener, Project}
import com.intellij.openapi.roots.impl.libraries.LibraryEx.ModifiableModelEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Computable
import com.twitter.intellij.pants.util.PantsUtil
import org.jetbrains.plugins.scala.findUsages.compilerReferences.ScalaCompilerReferenceService
import org.jetbrains.plugins.scala.project.{LibraryExt, ScalaLibraryProperties, ScalaLibraryType, Version}
import com.twitter.intellij.pants.settings.PantsSettings
import com.twitter.intellij.pants.util.PantsConstants

import scala.collection.JavaConverters.{asScalaSetConverter, iterableAsScalaIterableConverter}

object PantsScalaDataService {
  val LOG: Logger = Logger.getInstance(classOf[PantsScalaDataService])
}

class PantsScalaDataService extends ProjectDataService[ScalaModelData, Library] {

  import PantsScalaDataService._

  def getTargetDataKey: Key[ScalaModelData] = ScalaModelData.KEY

  override def importData(
    toImport: util.Collection[DataNode[ScalaModelData]],
    projectData: ProjectData,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ): Unit = {
    toImport.asScala.toSet.foreach[Unit](doImport(_, modelsProvider))

    // Workaround for bug in Scala Plugin https://youtrack.jetbrains.com/issue/SCL-16768
    //
    // Correct initialization of `ScalaCompilerReferenceService` is required for "Find usages"
    // feature to work correctly. Unfortunately, it's broken in that way, it does nothing when
    // there are no modules in the project right after the moment, when the project is opened.
    // This happens when a project is imported, the module creation starts after opening project.
    val pantsSettings = ExternalSystemApiUtil.getSettings(project, PantsConstants.SYSTEM_ID).asInstanceOf[PantsSettings]
    if (pantsSettings.isUseIntellijCompiler) {
      doOnceOnPantsModuleAdded(project) {
        ScalaCompilerReferenceService(project).initializeReferenceService()
      }
    }
  }

  private def doOnceOnPantsModuleAdded(project: Project)(action: => Unit): Unit = {
    project.getMessageBus.connect().subscribe(ProjectTopics.MODULES, new ModuleListener {
      var done = false

      override def moduleAdded(project: Project, module: Module): Unit = {
        if (!done) {
          if (PantsUtil.isPantsModule(module)) {
            action
            done = true;
          }
        }
      }
    })
  }

  private def doImport(scalaNode: DataNode[ScalaModelData], modelsProvider: IdeModifiableModelsProvider) {
    val scalaData: ScalaModelData = scalaNode.getData

    val scalaLibId: String = scalaData.getScalaLibId

    LOG.debug(s"Setting up $scalaLibId as Scala SDK")

    val compilerVersion: Version =
      scalaLibId.split(":").toSeq.lastOption
      .map(Version(_))
      .getOrElse(throw new ExternalSystemException("Cannot determine Scala compiler version for module " +
                                                   scalaNode.getData(ProjectKeys.MODULE).getExternalName))

    val scalaLibrary: Library = modelsProvider.getAllLibraries.find(_.getName.contains(scalaLibId))
      .getOrElse(throw new ExternalSystemException("Cannot find project Scala library " +
                                                   compilerVersion.presentation +
                                                   " for module " +
                                                   scalaNode.getData(ProjectKeys.MODULE).getExternalName))

    if (!scalaLibrary.isScalaSdk) {
      val properties = ScalaLibraryProperties(Some(compilerVersion.presentation))
      properties.compilerClasspath = scalaData.getClasspath.asScala.toSeq.map(new File(_))
      val modifiableModelEx = modelsProvider.getModifiableLibraryModel(scalaLibrary).asInstanceOf[ModifiableModelEx]
      modifiableModelEx.setKind(ScalaLibraryType().getKind)
      modifiableModelEx.setProperties(properties)
    } else {
      LOG.debug(s"${scalaLibrary.getName} is already a Scala SDK")
    }
  }

  override def computeOrphanData(toImport: util.Collection[DataNode[ScalaModelData]],
                                 projectData: ProjectData,
                                 project: Project,
                                 modelsProvider: IdeModifiableModelsProvider
  ): Computable[util.Collection[Library]] =
    () => Collections.emptyList()

  override def removeData(toRemove: Computable[util.Collection[Library]],
                          toIgnore: util.Collection[DataNode[ScalaModelData]],
                          projectData: ProjectData,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider): Unit = { }
}
