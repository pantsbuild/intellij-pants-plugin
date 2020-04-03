package com.twitter.ideprobe.dependencies

final case class IntelliJVersion(build: String) extends AnyVal

object IntelliJVersion {
  val V193_5233_102 = IntelliJVersion("193.5233.102")
  val V2019_3_1 = IntelliJVersion("2019.3.1")
  val V201_5259_13 = IntelliJVersion("201.5259.13-EAP-SNAPSHOT")
  val Latest = V201_5259_13
}
