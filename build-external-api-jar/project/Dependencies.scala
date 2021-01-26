import sbt._

object Dependencies {
  val junit = Seq(
    "junit" % "junit" % "4.13" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  object ideProbe {

    val version = "0.3.0+28-b36b0a51+20210114-1358-SNAPSHOT"
    val resolvers = Seq(
      Resolver.sonatypeRepo("public"),
      Resolver.sonatypeRepo("snapshots"),
      MavenRepository(
        "jetbrains-3rd",
        "https://jetbrains.bintray.com/intellij-third-party-dependencies")
    )

    def apply(name: String): ModuleID = {
      "org.virtuslab.ideprobe" %% name % version
    }

    val jUnitDriver = apply("junit-driver")
  }
}
