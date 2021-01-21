name := "build-external-api-jar"

version.in(ThisBuild) := "0.1"
scalaVersion.in(ThisBuild) := "2.13.1"
resolvers.in(ThisBuild) ++= Dependencies.ideProbe.resolvers
skip in publish := true

lazy val tests = project
  .in(file("tests"))
  .settings(
    name := "pants-tests",
    libraryDependencies += Dependencies.ideProbe.jUnitDriver,
    libraryDependencies ++= Dependencies.junit
  )
