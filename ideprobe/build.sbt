name := "ideprobe-pants"

organization.in(ThisBuild) := "com.twitter.ideprobe"
version.in(ThisBuild) := "0.1"
scalaVersion.in(ThisBuild) := "2.12.10"
intellijBuild.in(ThisBuild) := "202.6397.94"
resolvers.in(ThisBuild) ++= Dependencies.ideProbe.resolvers
skip in publish := true

import IdeaPluginAdapter._
import IdeaPluginDevelopment._
import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoKeys

/**
 * By default, the sbt-idea-plugin gets applied to all of the projects.
 * We want it only in the plugin projects, so we need to disable it here
 * as well as for each created project separately.
 */
disableIdeaPluginDevelopment()

val pluginSettings = Seq(
  packageMethod := PackagingMethod.Standalone(),
  intellijPlugins ++= Seq(
    "com.intellij.java".toPlugin,
    "JUnit".toPlugin,
    "PythonCore".toPlugin
  )
)

lazy val pantsProbeApi = project
  .in(file("api"))
  .settings(
    name := "pants-probe-api",
    libraryDependencies += Dependencies.ideProbe.api
  )

lazy val pantsProbePlugin = ideaPlugin("probePlugin", id = "pantsProbePlugin")
  .dependsOn(pantsProbeApi)
  .settings(
    libraryDependencies += Dependencies.ideProbe.probePlugin,
    intellijPluginName := "ideprobe-pants",
    packageArtifactZipFilter := { file: File =>
      // We want only this main jar to be packaged, all the library dependencies
      // are already in the probePlugin which will be available in runtime as we
      // depend on it in plugin.xml.
      // The packaging plugin is created to support one plugin per build, so there
      // seems to be no way to prevent including probePlugin.jar in the dist reasonable way.
      file.getName == "pantsProbePlugin.jar"
    },
    intellijPlugins += "com.intellij.plugins.pants:1.15.1.42d84c497b639ef81ebdae8328401e3966588b2c:bleedingedge".toPlugin,
    name := "pants-probe-plugin"
  )

lazy val pantsProbeDriver = project
  .in(file("driver"))
  .enablePlugins(BuildInfoPlugin)
  .disableIdeaPluginDevelopment
  .dependsOn(pantsProbeApi)
  .settings(
    name := "pants-probe-driver",
    libraryDependencies += Dependencies.ideProbe.jUnitDriver,
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "com.twitter.intellij.pants"
  )

lazy val pantsTests = project
  .in(file("tests"))
  .disableIdeaPluginDevelopment
  .dependsOn(pantsProbeDriver)
  .usesIdeaPlugin(pantsProbePlugin)
  .settings(
    name := "pants-tests",
    libraryDependencies ++= Dependencies.junit
  )

lazy val ciSetup = project
  .in(file("ci/setup"))
  .disableIdeaPluginDevelopment
  .dependsOn(pantsTests % "test->test")
  .settings(
    name := "pants-ci-setup",
    libraryDependencies ++= Dependencies.junit
  )

def ideaPlugin(path: String, id: String = null) = {
  val resolvedId = Option(id).getOrElse(path)
  Project(resolvedId, file(path))
    .enableIdeaPluginDevelopment
    .settings(pluginSettings)
}
