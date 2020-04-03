package com.twitter.ideprobe.ide.intellij

import com.twitter.ideprobe.dependencies.IntelliJVersion
import com.twitter.ideprobe.dependencies.Plugin

case class IntellijConfig(version: IntelliJVersion, plugins: Seq[Plugin] = Seq.empty)
