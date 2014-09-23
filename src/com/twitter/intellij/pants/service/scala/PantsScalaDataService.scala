package com.twitter.intellij.pants.service.scala

import java.io.File
import java.util

import com.intellij.facet.FacetManager
import com.intellij.openapi.externalSystem.model.{DataNode, ProjectKeys}
import com.intellij.openapi.externalSystem.service.project.{PlatformFacade, ProjectStructureHelper}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs._
import com.twitter.intellij.pants.service.scala.ScalaPantsDataService._
import io.netty.util.internal.StringUtil
import org.jetbrains.plugins.scala.config._
import org.jetbrains.plugins.scala.util.LibrariesUtil

import scala.collection.JavaConverters._

class ScalaPantsDataService(platformFacade: PlatformFacade, helper: ProjectStructureHelper)
  extends AbstractDataService[ScalaModelData, ScalaFacet](ScalaModelData.KEY) {

  def doImportData(toImport: util.Collection[DataNode[ScalaModelData]], project: Project) {
    toImport.asScala.foreach { facetNode =>
      val scalaData = facetNode.getData

      val compilerLibrary = {
        val classpath = scalaData.getScalaCompilerJars.asScala.toSet

        findPantsScalaCompilerLibraryInForClassPath(project)(classpath).getOrElse(
          createScalaCompilerLibraryIn(project)(classpath)
        )
      }

      def setup(facet: ScalaFacet) {
        facet.compilerLibraryId = LibraryId(compilerLibrary.getName, LibraryLevel.Project)
      }

      val moduleName = facetNode.getData(ProjectKeys.MODULE).getExternalName.replaceAll("/", "_")
      Option(helper.findIdeModule(moduleName, project)) foreach { module =>
        ScalaFacet.findIn(module).map(setup).getOrElse(
          ScalaFacet.createIn(module)(setup))

        LibrariesUtil.addLibrary(compilerLibrary, module)
      }
    }
  }

  def doRemoveData(toRemove: util.Collection[_ <: ScalaFacet], project: Project) {
    toRemove.asScala.foreach(delete)
  }
}

object ScalaPantsDataService {
  private val pantsScalaLibraryNamePrefix = "Pants:: scala-compiler-bundle"

  def findPantsScalaCompilerLibraryInForClassPath(project: Project)(classpath: Set[File]): Option[Library] = {
    val compilerVersion = findCompilerVersion(classpath)
    ProjectLibraryTable.getInstance(project).getLibraries find {
      // if compilerVersion is None then let's just pick first Scala compiler lib
      _.getName.startsWith(scalaCompilerLibraryNameForVersion(compilerVersion))
    } map { library =>
      val libraryClasses = library.getFiles(OrderRootType.CLASSES).toSet
      val jarsToAdd = classpath flatMap { file =>
        Option(JarFileSystem.getInstance().findLocalVirtualFileByPath(file.getAbsolutePath))
      } filterNot libraryClasses.contains

      if (jarsToAdd.nonEmpty) {
        // update jars for existing scala lib
        val model = library.getModifiableModel
        jarsToAdd.foreach(model.addRoot(_, OrderRootType.CLASSES))
        model.commit()
      }
      library
    }
  }

  def createScalaCompilerLibraryIn(project: Project)(classpath: Set[File]): Library = {
    val library = ProjectLibraryTable.getInstance(project).createLibrary()
    val model = library.getModifiableModel
    model.setName(scalaCompilerLibraryNameFor(classpath))
    classpath.foreach(file => model.addRoot(VfsUtil.getUrlForLibraryRoot(file), OrderRootType.CLASSES))
    model.commit()
    library
  }

  private def scalaCompilerLibraryNameFor(classpath: Set[File]) =
    scalaCompilerLibraryNameForVersion(findCompilerVersion(classpath))

  private def scalaCompilerLibraryNameForVersion(compilerVersion: Option[String]): String = {
    pantsScalaLibraryNamePrefix + compilerVersion.fold("")("-" + _)
  }

  private def findCompilerVersion(classpath: Set[File]): Option[String] = {
    classpath.find(_.getName.startsWith("scala-compiler-"))
      .map(FileUtil.getNameWithoutExtension).map(_.replaceAll("scala-compiler-", ""))
  }

  def delete(facet: ScalaFacet) {
    val facetManager = FacetManager.getInstance(facet.getModule)
    val model = facetManager.createModifiableModel
    model.removeFacet(facet)
    model.commit()
  }
}

