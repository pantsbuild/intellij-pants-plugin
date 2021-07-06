import sbt._

object Dependencies {
  val junit = Seq(
    "junit" % "junit" % "4.13" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  object ideProbe {

    val version = "0.10.1"
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

    val jUnitDriver = apply("junit-driver")
  }
}
