package com.twitter.ideprobe.protocol

final case class Reference(text: String, target: Reference.Target)
object Reference {
  sealed trait Target
  object Target {
    case class File(file: FileRef) extends Target
  }
}
