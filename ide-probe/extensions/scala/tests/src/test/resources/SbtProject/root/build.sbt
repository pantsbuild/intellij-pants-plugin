scalaVersion := "2.13.1"
name := "hello-world"
version := "1.0"

lazy val foo = project.in(file("foo"))

lazy val bar = project
  .in(file("bar"))
  .dependsOn(foo)
