import org.jetbrains.sbtidea.AbstractSbtIdeaPlugin
import org.jetbrains.sbtidea.packaging.PackagingKeys
import org.jetbrains.sbtidea.Keys.intellijPluginName
import org.jetbrains.sbtidea.packaging.PackagingKeys.{
  packageArtifact,
  packageArtifactZip,
  packageArtifactZipFile,
  packageLibraryMappings
}
import org.jetbrains.sbtidea.packaging.artifact.ZipPackager
import sbt.Keys._
import sbt._

/** Our sbt plugin for developing idea plugins (replacing the original one) */
object IdeaPluginDevelopment extends AbstractSbtIdeaPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def trigger = noTrigger

  override def projectSettings: Seq[sbt.Setting[_]] = {
    super.projectSettings ++ PackagingKeys.projectSettings ++ overriddenSettings
  }

  lazy val packageArtifactZipFilter = settingKey[File => Boolean]("Files to include in disted zip")

  private val overriddenSettings = Seq(
    // automatically include the dependencies in the plugin
    packageLibraryMappings := {
      val libraries = libraryDependencies.value
      libraries.map(id => (id, Some(s"lib/${id.name}.jar")))
    },
    packageArtifactZipFilter := ((_: File) => true),
    packageArtifactZip := Def.task {
      implicit val stream: TaskStreams = streams.value
      val distDir = packageArtifact.value

      val targetZip = packageArtifactZipFile.value
      IO.delete(targetZip)

      val dirToZip = distDir.toPath.resolveSibling("zip-me")
      IO.delete(dirToZip.toFile)

      val copiedDistDir = dirToZip.resolve(intellijPluginName.value).toFile
      IO.copyDirectory(distDir, copiedDistDir)

      val excludeFilter: FileFilter = (file: File) => !packageArtifactZipFilter.value(file)
      val toDelete = IO.listFiles(copiedDistDir.toPath.resolve("lib").toFile, excludeFilter)
      IO.delete(toDelete)

      new ZipPackager(targetZip.toPath).mergeIntoOne(Seq(dirToZip))

      IO.delete(dirToZip.toFile)

      targetZip
    }.value
  )
}
