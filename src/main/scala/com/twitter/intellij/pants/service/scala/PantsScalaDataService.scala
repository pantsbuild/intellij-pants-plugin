// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala

import java.io.File
import java.util
import java.util.Collections

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.{DataNode, ExternalSystemException, Key, ProjectKeys}
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx.ModifiableModelEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Computable
import org.jetbrains.plugins.scala.project.{LibraryExt, ScalaLibraryProperties, ScalaLibraryType, Version}

import scala.jdk.CollectionConverters._
object PantsScalaDataService {
  val LOG: Logger = Logger.getInstance(classOf[PantsScalaDataService])
}

class PantsScalaDataService extends ProjectDataService[ScalaModelData, Library] {

  import PantsScalaDataService._

  def getTargetDataKey: Key[ScalaModelData] = ScalaModelData.KEY

  override def importData(
    toImport: util.Collection[_ <: DataNode[ScalaModelData]],
    projectData: ProjectData,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ): Unit = {
    toImport.asScala.toSet.foreach[Unit](doImport(_, modelsProvider))
  }

  private def doImport(scalaNode: DataNode[ScalaModelData], modelsProvider: IdeModifiableModelsProvider): Unit = {
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

  override def computeOrphanData(toImport: util.Collection[_ <: DataNode[ScalaModelData]],
                                 projectData: ProjectData,
                                 project: Project,
                                 modelsProvider: IdeModifiableModelsProvider
  ): Computable[util.Collection[Library]] =
    new Computable[util.Collection[Library]] {
      override def compute(): util.Collection[Library] = Collections.emptyList()
    }

  override def removeData(toRemove: Computable[_ <: util.Collection[_ <: Library]],
                          toIgnore: util.Collection[_ <: DataNode[ScalaModelData]],
                          projectData: ProjectData,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider): Unit = { }
}
