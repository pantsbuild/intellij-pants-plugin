package com.twitter.ideprobe

import java.nio.file.Files
import java.util.concurrent.Executors
import com.twitter.ideprobe.Extensions._
import com.twitter.ideprobe.dependencies.IntelliJVersion
import org.junit.Assert._
import org.junit.Test
import scala.concurrent.ExecutionContext

final class IntelliJRuleFromConfigTest extends WorkspaceFixture {
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  /**
   * Note that the validity of the properties cannot be check during creation, nor it should be validated then.
   * This test check only that a rule can be created with a given set of properties.
   */
  @Test
  def createsRuleFromConfFile(): Unit = withWorkspace { workspace =>
    val config = Config.fromString(s"""
         |probe {
         |  intellij.version = "${IntelliJVersion.Latest.build}"
         |  workspace.path = "$workspace" 
         |}
         |""".stripMargin)

    assertNotNull(IntelliJFixture.fromConfig(config))
  }

  @Test
  def usesProvidedWorkspaceDirectory(): Unit = withWorkspace { workspace =>
    val file = Files.createTempFile(workspace, "ideprobe", "file")
    val config = Config.fromString(s"""
         |probe {
         |  intellij.version = "${IntelliJVersion.Latest.build}"
         |  workspace.path = "$workspace"
         |}
         |""".stripMargin)
    val rule = IntelliJFixture.fromConfig(config).rule
    try {
      rule.before() // initialize workspace
      val workspaceFile = rule.workspace.resolve(file.getFileName)
      assertTrue(s"Invalid workspace used: ${rule.workspace}", Files.exists(workspaceFile))
    } finally {
      rule.after()
    }
  }

  @Test
  def exposesEnvironmentFromConfigFile(): Unit = withWorkspace { workspace =>
    val config = Config.fromString(s"""
         |probe {
         |  intellij.version = "${IntelliJVersion.Latest.build}"
         |  workspace.path = "$workspace"
         |}
         |key = "value"
         |""".stripMargin)

    val rule = IntelliJFixture.fromConfig(config)

    rule.config.get[String]("key") match {
      case Some(value) =>
        assertEquals("value", value)
      case None =>
        fail("key not found in config")
    }
  }

  @Test
  def clonesGitRepositoryToWorkspace(): Unit = {
    val publicRepository = "https://github.com/VirtusLab/git-machete.git"

    val config = Config.fromString(s"""
         |probe {
         |  intellij.version = "${IntelliJVersion.Latest.build}"
         |  workspace.path = "$publicRepository"
         |}
         |""".stripMargin)

    IntelliJFixture.fromConfig(config).run { intelliJ =>
      assertTrue(intelliJ.workspace.resolve(".git").isDirectory)
    }
  }

  @Test
  def clonesSpecificGitBranchToWorkspace(): Unit = {
    val publicRepository = "https://github.com/VirtusLab/git-machete.git"
    val branch = "develop"

    val config = Config.fromString(s"""
          |probe {
          |  intellij.version = "${IntelliJVersion.Latest.build}"
          |  workspace {
          |    path = "$publicRepository"
          |    branch = "$branch"
          |  }
          |}
          |""".stripMargin)

    IntelliJFixture.fromConfig(config).run { intelliJ =>
      val HEAD = intelliJ.workspace.resolve(".git/HEAD").content().trim
      assertEquals(HEAD, s"ref: refs/heads/$branch")
    }
  }

  @Test
  def checksOutRefByHash(): Unit = {
    val publicRepository = "https://github.com/VirtusLab/git-machete.git"
    val commit = "a1861fc3b70588acfa171000eb365bf75c143472"

    val config = Config.fromString(s"""
          |probe {
          |  intellij.version = "${IntelliJVersion.Latest.build}"
          |  workspace {
          |    path = "$publicRepository"
          |    commit = "$commit"
          |  }
          |}
          |""".stripMargin)

    IntelliJFixture.fromConfig(config).run { intelliJ =>
      val HEAD = intelliJ.workspace.resolve(".git/HEAD").content().trim
      assertEquals(HEAD, commit)
    }
  }

}
