package com.twitter.intellij.pants.protocol

import org.virtuslab.ideprobe.protocol.Sdk

case class PythonFacet(name: String, sdk: Option[Sdk])
