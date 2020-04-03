import java.nio.file.Paths
import sbt.Def
import sbt.Keys.loadedBuild
import sbt.ProjectRef
import sbt._

object CI {
  private val excluded = Set("ide-probe", "ci")
  lazy val generateScripts = taskKey[Seq[File]]("Generate CI scripts")

  def groupedProjects(): Def.Initialize[Task[Map[String, Seq[ProjectRef]]]] = Def.task {
    val extensionDir = Paths.get(loadedBuild.value.root).resolve("extensions")

    loadedBuild.value.allProjectRefs
      .filterNot { case (_, project) => excluded.contains(project.id) }
      .groupBy {
        case (_, project) =>
          val projectPath = project.base.toPath
          if (projectPath.startsWith(extensionDir)) extensionDir.relativize(projectPath).getName(0).toString
          else "probe"
      }
      .mapValues(_.map(_._1))
  }

  def generateTestScript(group: String, projects: Seq[ProjectRef]): sbt.File = {
    val script = file(s"ci/test-$group")
    val arguments = projects.map(ref => s"; ${ref.project} / test").mkString
    val content = s"""|#!/bin/sh
                      |
                      |export IDEPROBE_DISPLAY=xvfb
                      |export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
                      |
                      |sbt "$arguments"
                      |""".stripMargin
    IO.write(script, content)
    script
  }
}
