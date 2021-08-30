name := "build-external-api-jar"

version.in(ThisBuild) := "0.1"
scalaVersion.in(ThisBuild) := "2.13.1"
resolvers.in(ThisBuild) ++= Dependencies.ideProbe.resolvers
skip in publish := true
libraryDependencies += Dependencies.ideProbe.driver
