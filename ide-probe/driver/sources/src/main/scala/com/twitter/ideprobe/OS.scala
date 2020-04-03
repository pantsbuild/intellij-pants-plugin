package com.twitter.ideprobe

sealed trait OS
object OS {
  val Current: OS = {
    val name = System.getProperty("os.name").toLowerCase()
    if (name.startsWith("win")) Windows
    else if (name.startsWith("mac")) Mac
    else if (name.contains("nix") || name.contains("nux")) Unix
    else throw new IllegalStateException(s"Unrecognized system: $name")
  }

  case object Windows extends OS
  case object Unix extends OS
  case object Mac extends OS
}
