package com.twitter.ideprobe.protocol

case class Project(
    name: String,
    basePath: String,
    modules: Seq[Module]
)

case class Module(name: String, contentRoots: Seq[String], kind: Option[String])
