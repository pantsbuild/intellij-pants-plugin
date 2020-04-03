package com.twitter.ideprobe

import java.nio.file.Path
import java.nio.file.Paths
import com.twitter.ideprobe.Extensions._

sealed trait WorkspaceTemplate {
  def setupIn(workspace: Path): Unit
}

object WorkspaceTemplate {
  def from(config: WorkspaceConfig): WorkspaceTemplate = {
    import com.twitter.ideprobe.dependencies.Resource._
    config match {
      case WorkspaceConfig.Default(resource) =>
        resource match {
          case File(path) =>
            FromFile(path)
          case Http(uri) if uri.getHost == "github.com" =>
            FromGit(uri.toString, None)
          case other =>
            throw new IllegalArgumentException(s"Unsupported workspace template: $other")
        }
      case git: WorkspaceConfig.Git =>
        val repository = git.path
        val ref = Some(git.ref)
        repository match {
          case File(path) =>
            FromGit(path.toString, ref)
          case Http(uri) =>
            FromGit(uri.toString, ref)
          case Unresolved(path, _) =>
            FromGit(path, ref)
        }
    }
  }

  case object Empty extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = ()
  }

  case class FromFile(path: Path) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      path.copyDirContent(workspace)
    }
  }

  case class FromResource(relativePath: String) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      val path = Paths.get(getClass.getResource(s"/$relativePath").getPath).toAbsolutePath
      path.copyDirContent(workspace)
    }
  }

  case class FromGit(repository: String, ref: Option[String]) extends WorkspaceTemplate {
    override def setupIn(workspace: Path): Unit = {
      val cloned = Shell.run("git", "clone", repository, workspace.toString)
      if (cloned.exitCode != 0) throw new IllegalStateException(s"Could not clone git $repository")
      ref.foreach { ref =>
        val checkout = Shell.run(in = workspace, "git", "checkout", ref)
        if (checkout.exitCode != 0) throw new IllegalStateException(s"Could not checkout $ref in $repository")
      }
    }
  }
}
