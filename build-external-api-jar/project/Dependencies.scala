import sbt._

object Dependencies {
  object ideProbe {
    val version = "0.15.0"
    val resolvers = Seq(
      Resolver.sonatypeRepo("public"),
      Resolver.sonatypeRepo("snapshots"),
      MavenRepository(
        "jetbrains-3rd",
        "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    )

    def apply(name: String): ModuleID = {
      "org.virtuslab.ideprobe" %% name % version
    }

    val driver = apply("driver")
  }
}
