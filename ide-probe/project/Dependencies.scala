import sbt._

object Dependencies {

  val junit = Seq(
    "junit" % "junit" % "4.12" % Test,
    ("com.novocode" % "junit-interface" % "0.11" % Test).exclude("junit", "junit-dep")
  )

  val scalaParallelCollections = "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"

  val nuProcess = "com.zaxxer" % "nuprocess" % "1.2.6"

  val gson = "com.google.code.gson" % "gson" % "2.8.6"

  val ammonite = "com.lihaoyi" %% "ammonite-ops" % "2.0.4"

  // because idea plugin packager would only take the root jar which has no classes
  // somehow it fails to see the transitive dependencies (even though the code says it should)
  // so here are all the dependencies explicitly
  val pureConfig = {
    val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
    val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
    val pureConfigModules =
      Seq("pureconfig", "pureconfig-macros", "pureconfig-generic", "pureconfig-generic-base", "pureconfig-core")
    pureConfigModules.map { module =>
      "com.github.pureconfig" %% module % "0.12.2"
    } ++ Seq(typesafeConfig, shapeless)
  }

}
