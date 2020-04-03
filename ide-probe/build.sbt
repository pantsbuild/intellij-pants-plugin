name := "ideprobe"

organization.in(ThisBuild) := "com.twitter"
version.in(ThisBuild) := "0.1"
scalaVersion.in(ThisBuild) := "2.13.1"
intellijBuild.in(ThisBuild) := "201.6668.13"

import IdeaPluginAdapter._
import IdeaPluginDevelopment._

/**
 * By default, the sbt-idea-plugin gets applied to all of the projects.
 * We want it only in the plugin projects, so we need to disable it here
 * as well as for each created project separately.
 */
disableIdeaPluginDevelopment()

val pluginSettings = Seq(
  packageMethod := PackagingMethod.Standalone(),
  intellijPlugins += "com.intellij.java".toPlugin
)

lazy val ci = project.settings(
  CI.generateScripts := {
    CI.groupedProjects().value.toList.map {
      case (group, projects) => CI.generateTestScript(group, projects)
    }
  }
)

lazy val api = project
  .in(file("api"))
  .settings(
    libraryDependencies ++= Dependencies.junit,
    libraryDependencies ++= Dependencies.pureConfig,
    libraryDependencies ++= Seq(
      Dependencies.gson,
      Dependencies.ammonite
    )
  )

lazy val driver = project
  .enablePlugins(BuildInfoPlugin)
  .in(file("driver/sources"))
  .disableIdeaPluginDevelopment
  .dependsOn(api)
  .usesIdeaPlugin(probePlugin)
  .settings(
    name := "driver",
    libraryDependencies ++= Dependencies.junit,
    libraryDependencies ++= Seq(
      Dependencies.scalaParallelCollections,
      Dependencies.nuProcess
    ),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "com.twitter.ideprobe"
  )

lazy val driverTest = project
  .in(file("driver/tests"))
  .disableIdeaPluginDevelopment
  .dependsOn(driver, junitDriver, api % "compile->compile;test->test")
  .usesIdeaPlugin(driverTestPlugin)
  .settings(
    name := "driver-tests",
    libraryDependencies ++= Dependencies.junit
  )

lazy val driverTestPlugin = ideaPlugin("driver/test-plugin", id = "driverTestPlugin")
  .settings(
    intellijPluginName := "driver-test-plugin"
  )

lazy val junitDriver = project
  .in(file("driver/bindings/junit"))
  .disableIdeaPluginDevelopment
  .dependsOn(driver, api % "compile->compile;test->test")
  .settings(
    name := "junit-driver",
    libraryDependencies ++= Dependencies.junit
  )

lazy val probePlugin = ideaPlugin("probePlugin")
  .dependsOn(api)
  .settings(intellijPluginName := "ideprobe")

lazy val pantsProbeApi = project
  .in(file("extensions/pants/api"))
  .dependsOn(api)

lazy val pantsProbePlugin = ideaPlugin("extensions/pants/probePlugin", id = "pantsProbePlugin")
  .dependsOn(pantsProbeApi, probePlugin)
  .settings(
    intellijPluginName := "ideprobe-pants",
    packageArtifactZipFilter := { file: File =>
      // We want only this main jar to be packaged, all the library dependencies
      // are already in the probePlugin which will be available in runtime as we
      // depend on it in plugin.xml.
      // The packaging plugin is created to support one plugin per build, so there
      // seems to be no way to prevent including probePlugin.jar in the dist reasonable way.
      file.getName == "pantsProbePlugin.jar"
    },
    intellijPlugins += "com.intellij.plugins.pants:1.14.0.7ac62cd2faa5173a3b32ffafb2350d31ee14f2b3:Bleedingedge".toPlugin
  )

lazy val pantsProbeDriver = project
  .in(file("extensions/pants/driver"))
  .disableIdeaPluginDevelopment
  .dependsOn(pantsProbeApi, junitDriver)

lazy val pantsTests = project
  .in(file("extensions/pants/tests"))
  .disableIdeaPluginDevelopment
  .dependsOn(pantsProbeDriver, api % "compile->compile;test->test")
  .usesIdeaPlugin(pantsProbePlugin)
  .settings(libraryDependencies ++= Dependencies.junit)

lazy val scalaTests = project
  .in(file("extensions/scala/tests"))
  .disableIdeaPluginDevelopment
  .dependsOn(junitDriver)
  .settings(libraryDependencies ++= Dependencies.junit)

def ideaPlugin(path: String, id: String = null) = {
  val resolvedId = Option(id).getOrElse(path)
  Project(resolvedId, file(path)).enableIdeaPluginDevelopment
    .settings(pluginSettings)
}
