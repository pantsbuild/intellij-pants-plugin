package com.twitter.intellij.pants

import java.nio.file.Paths

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
import org.virtuslab.ideprobe.protocol.VcsRoot
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

abstract class OpenProjectTest {

  def intelliJ: RunningIntelliJFixture

  @Test def hasExpectedName(): Unit = {
    val expectedProject = intelliJ.config[String]("project.name")
    val projectModel = intelliJ.probe.projectModel()
    Assert.assertEquals(expectedProject, projectModel.name)
  }

  @Test def hasExpectedModules(): Unit = {
    def relative(absolutePath: String): String = intelliJ.workspace.relativize(Paths.get(absolutePath)).toString

    val projectModel = intelliJ.probe.projectModel()

    val expectedModules = intelliJ.config[Seq[TestData.Module]]("project.modules")
    val importedModules = projectModel.modules.map(module => TestData.Module(module.name, module.contentRoots.map(relative)))

    Assert.assertTrue(
      s"Expected modules: $expectedModules, actual modules: $importedModules",
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

  @Test def hasGitRepositoryRootDetected(): Unit = {
    val actualVcsRoots = intelliJ.probe.vcsRoots()
    val expectedRoot = VcsRoot("Git", intelliJ.workspace)
    assertEquals(Seq(expectedRoot), actualVcsRoots)
  }

}

object OpenProjectTestFixture extends ConfigFormat {
  object TestData {
    case class Module(name: String, contentRoots: Seq[String])
    implicit val moduleReader: ConfigReader[Module] = deriveReader[Module]
  }
}

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

class OpenProjectTestPants extends OpenProjectTest {
  override def intelliJ: RunningIntelliJFixture = OpenProjectTestPants.intelliJ
}


object OpenProjectTestBsp extends OpenProjectTestFixture {
  override def openProject(): ProjectRef = openProjectWithBsp(intelliJ)
}

class OpenProjectTestBsp extends OpenProjectTest {
  override def intelliJ: RunningIntelliJFixture = OpenProjectTestBsp.intelliJ
}
