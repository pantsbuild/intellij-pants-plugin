package com.twitter.ideprobe.dependencies

object IntelliJResolver {
  private val officialUri = "https://www.jetbrains.com/intellij-repository"
  private val officialGroup = "com/jetbrains/intellij/idea"
  private val officialArtifact = "ideaIC"

  private val officialReleases = fromMaven(s"$officialUri/releases", officialGroup, officialArtifact)
  private val officialSnapshots = fromMaven(s"$officialUri/snapshots", officialGroup, officialArtifact)

  val Official: DependencyResolver[IntelliJVersion] = (key: IntelliJVersion) => {
    if (key.build.endsWith("SNAPSHOT")) officialSnapshots.resolve(key)
    else officialReleases.resolve(key)
  }

  def fromMaven(uri: String, group: String, artifact: String): DependencyResolver[IntelliJVersion] = {
    val repository = new MavenRepository(uri)

    version: IntelliJVersion => {
      val key = MavenRepository.Key(group, artifact, version.build)
      repository.resolve(key)
    }
  }

  def from(config: DependenciesConfig.IntelliJ): DependencyResolver[IntelliJVersion] = {
    config.repository
      .map(maven => fromMaven(maven.uri, maven.group, maven.artifact))
      .getOrElse(Official)
  }
}
