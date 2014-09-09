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
import com.intellij.openapi.vfs.{VfsUtil, VfsUtilCore}
import com.twitter.intellij.pants.service.scala.ScalaPantsDataService._
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

        findLibraryByClassesIn(project)(classpath).getOrElse(
          createLibraryIn(project)(compilerLibraryNameFor(classpath), classpath))
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
  def findLibraryByClassesIn(project: Project)(classpath: Set[File]): Option[Library] =
    ProjectLibraryTable.getInstance(project).getLibraries.find(has(classpath))

  private def has(classpath: Set[File])(library: Library) =
    library.getFiles(OrderRootType.CLASSES).toSet.map(VfsUtilCore.virtualToIoFile) == classpath

  def compilerLibraryNameFor(classpath: Set[File]): String = {
    val compilerVersion =
      classpath.find(_.getName.startsWith("scala-compiler-"))
        .map(FileUtil.getNameWithoutExtension).map(_.replaceAll("scala-compiler-", ""))

    "Pants:: scala-compiler-bundle" + compilerVersion.fold("")("-" + _)
  }

  def createLibraryIn(project: Project)(name: String, classpath: Set[File]): Library = {
    val library = ProjectLibraryTable.getInstance(project).createLibrary()
    val model = library.getModifiableModel
    model.setName(name)
    classpath.foreach(file => model.addRoot(VfsUtil.getUrlForLibraryRoot(file), OrderRootType.CLASSES))
    model.commit()
    library
  }

  def configure(compilerLibraryName: String, compilerOptions: Seq[String])(facet: ScalaFacet) {
    facet.compilerLibraryId = LibraryId(compilerLibraryName, LibraryLevel.Project)
    facet.compilerParameters = compilerOptions.toArray
  }

  def delete(facet: ScalaFacet) {
    val facetManager = FacetManager.getInstance(facet.getModule)
    val model = facetManager.createModifiableModel
    model.removeFacet(facet)
    model.commit()
  }
}

