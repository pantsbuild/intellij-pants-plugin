import sbt._

object Dependencies {
  val junit = Seq(
    "junit" % "junit" % "4.12" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  object ideProbe {
    val resolvers = Seq(
      Resolver.sonatypeRepo("public"),
      Resolver.sonatypeRepo("snapshots"),
      MavenRepository("jetbrains-3rd", "https://jetbrains.bintray.com/intellij-third-party-dependencies")
    )

    val version = "0.1.3+41-e3e6ebad-SNAPSHOT"

    def apply(name: String): ModuleID = {
      "org.virtuslab.ideprobe" %% name % version
    }

    val api = apply("api")
    val driver = apply("driver")
    val jUnitDriver = apply("junit-driver")
    val probePlugin = apply("probe-plugin")
  }

}
