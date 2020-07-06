import sbt._

object Dependencies {
  val junit = Seq(
    "junit" % "junit" % "4.12" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  object ideProbe {
    val version = "0.0.0+13-87a51417+20200708-1038-SNAPSHOT"

    def apply(name: String): ModuleID = {
      "org.virtuslab.ideprobe" %% name % version
    }

    val api = apply("api")
    val driver = apply("driver")
    val jUnitDriver = apply("junit-driver")
    val probePlugin = apply("probe-plugin")
  }

}
