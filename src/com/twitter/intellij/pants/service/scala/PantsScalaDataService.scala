// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala

import java.io.File
import java.util
import java.util.Collections

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.{DataNode, ExternalSystemException, ProjectKeys}
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx.ModifiableModelEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Computable
import org.jetbrains.plugins.scala.project.{LibraryExt, ScalaLanguageLevel, ScalaLibraryProperties, ScalaLibraryType, Version}

import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object PantsScalaDataService {
  val LOG = Logger.getInstance(classOf[PantsScalaDataService])
}

class PantsScalaDataService extends ProjectDataService[ScalaModelData, Library] {

  import PantsScalaDataService._

  def getTargetDataKey = ScalaModelData.KEY

  override def importData(
    toImport: util.Collection[DataNode[ScalaModelData]],
    projectData: ProjectData,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ): Unit = {
    toImport.asScala.toSet.foreach[Unit](doImport(_, modelsProvider))
  }

  private def doImport(scalaNode: DataNode[ScalaModelData], modelsProvider: IdeModifiableModelsProvider) {
    val scalaData = scalaNode.getData

    val scalaLibId = scalaData.getScalaLibId

    LOG.debug(s"Setting up $scalaLibId as Scala SDK")

    val compilerVersion =
      scalaLibId.split(":").toSeq.lastOption
      .map(Version(_))
      .getOrElse(throw new ExternalSystemException("Cannot determine Scala compiler version for module " +
                                                   scalaNode.getData(ProjectKeys.MODULE).getExternalName))

    val scalaLibrary = modelsProvider.getAllLibraries.find(_.getName.contains(scalaLibId))
      .getOrElse(throw new ExternalSystemException("Cannot find project Scala library " +
                                                   compilerVersion.number +
                                                   " for module " +
                                                   scalaNode.getData(ProjectKeys.MODULE).getExternalName))

    if (!scalaLibrary.isScalaSdk) {
      val properties = new ScalaLibraryProperties()
      properties.languageLevel = compilerVersion.toLanguageLevel.getOrElse(ScalaLanguageLevel.Default)
      properties.compilerClasspath = scalaData.getClasspath.asScala.toSeq.map(new File(_))
      val modifiableModelEx = modelsProvider.getModifiableLibraryModel(scalaLibrary).asInstanceOf[ModifiableModelEx]
      modifiableModelEx.setKind(ScalaLibraryType.instance.getKind)
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
    new Computable[util.Collection[Library]] {
      override def compute(): util.Collection[Library] = Collections.emptyList()
    }

  override def removeData(toRemove: Computable[util.Collection[Library]],
                          toIgnore: util.Collection[DataNode[ScalaModelData]],
                          projectData: ProjectData,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider): Unit = { }
}
