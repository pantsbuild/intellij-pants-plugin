package com.twitter.intellij.pants

import java.nio.file.Path

import com.twitter.intellij.pants.OpenProjectTestFixture.TestData
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.RunningIntellijPerSuite
import org.virtuslab.ideprobe.Shell
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.SourceFolder
import org.virtuslab.ideprobe.protocol.VcsRoot
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

// Actual test classes:
class OpenProjectTestPants extends CommonOpenProjectTests {
  override def intelliJ: RunningIntelliJFixture = OpenProjectTestPants.intelliJ

  // TODO see how can it be supported with BSP
  @Test def hasPythonFacetsSetup(): Unit = {
    val project = intelliJ.probe.projectModel()
    val pythonModules = intelliJ.config[Set[String]]("project.pythonModules")
    project.modules.map(_.name).filter(pythonModules.contains).foreach { module =>
      val facets = PantsProbeDriver(intelliJ.probe).getPythonFacets(ModuleRef(module))
      Assert.assertTrue(s"Unexpected python facets $facets", facets.size == 1)
      val Seq(facet) = facets
      Assert.assertTrue(s"No sdk in python facet $facet", facet.sdk.isDefined)
      val Some(sdk) = facet.sdk
      Assert.assertTrue(s"Sdk does not have home path: $sdk", sdk.homePath.nonEmpty)
      Assert.assertEquals("Incorrect sdk type", "Python SDK", sdk.typeId)
    }
  }

}

class OpenProjectTestBsp extends CommonOpenProjectTests {
  override def intelliJ: RunningIntelliJFixture = OpenProjectTestBsp.intelliJ

  // TODO add to common OpenProjectTest when it works with pants
  @Test def hasGitRepositoryRootDetected(): Unit = {
    val actualVcsRoots = intelliJ.probe.vcsRoots()
    val expectedRoot = VcsRoot("Git", intelliJ.workspace)
    assertEquals(Seq(expectedRoot), actualVcsRoots)
  }

}

abstract class CommonOpenProjectTests {

  def intelliJ: RunningIntelliJFixture

  @Test def hasExpectedName(): Unit = {
    val expectedProject = intelliJ.config[String]("project.name")
    val projectModel = intelliJ.probe.projectModel()
    Assert.assertEquals(expectedProject, projectModel.name)
  }

  @Test def hasExpectedModules(): Unit = {
    def relative(absolutePath: Path): Path = intelliJ.workspace.relativize(absolutePath)

    val projectModel = intelliJ.probe.projectModel()

    val expectedModules = intelliJ.config[Seq[TestData.Module]]("project.modules")
    val importedModules = projectModel.modules.map { module =>
      TestData.Module(module.name, module.contentRoots.all.map(sf => TestData.ContentRoot(relative(sf.path), sf.kind, sf.packagePrefix)))
    }

    Assert.assertTrue(
      s"Expected modules: $expectedModules, actual modules: $importedModules (not a subset)",
      expectedModules.toSet.subsetOf(importedModules.toSet))
  }

  @Test def hasProjectSdkSet(): Unit = {
    val projectSdk = intelliJ.probe.projectSdk()
    Assert.assertTrue(s"Project without sdk", projectSdk.isDefined)
  }

  @Test def hasModuleSdksSet(): Unit = {
    val project = intelliJ.probe.projectModel()
    val expectedModules = intelliJ.config[Seq[TestData.Module]]("project.modules").map(_.name)

    val modulesWithoutSdk = project.modules
      .filter(module => module.kind.isDefined && expectedModules.contains(module.name))
      .map(m => ModuleRef(m.name))
      .filter(module => intelliJ.probe.moduleSdk(module).isEmpty)

    Assert.assertTrue(s"Modules without sdk: $modulesWithoutSdk", modulesWithoutSdk.isEmpty)
  }

}

object OpenProjectTestFixture extends ConfigFormat {
  object TestData {
    case class ContentRoot(path: Path, kind: SourceFolder.Kind, packagePrefix: Option[String])
    case class Module(name: String, contentRoots: Set[ContentRoot])
    implicit val contentRootReader: ConfigReader[ContentRoot] = deriveReader[ContentRoot]
    implicit val moduleReader: ConfigReader[Module] = deriveReader[Module]
  }
}

// common fixtures configuration:

// Because RunningIntellijPerSuite uses @BeforeClass, which must be static, this trait must be
// mixed in to an companion object of actual test class
trait OpenProjectTestFixture extends PantsTestSuite with RunningIntellijPerSuite with ConfigFormat {
  override protected def baseFixture = fixtureFromConfig()

  override def beforeAll(): Unit = {
    Shell.run(in = intelliJ.workspace, "git", "init")
    openProject()
  }

  def openProject(): ProjectRef
}

object OpenProjectTestPants extends OpenProjectTestFixture {
  override def openProject(): ProjectRef = openProjectWithPants(intelliJ)
}

object OpenProjectTestBsp extends OpenProjectTestFixture {
  override def openProject(): ProjectRef = openProjectWithBsp(intelliJ)
}
