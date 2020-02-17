// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework.performance

import java.io.File
import java.util.Collections

import com.intellij.ProjectTopics
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.{ModuleRootAdapter, ModuleRootEvent, ModuleRootListener}
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.twitter.intellij.pants.model.PantsTargetAddress
import com.twitter.intellij.pants.testFramework.PantsIntegrationTestCase
import com.twitter.intellij.pants.util.PantsUtil

import scala.collection.JavaConversions.collectionAsScalaIterable

object PantsPerformanceBenchmark {
  def main(args: Array[String]) {
    def nextOption(map : Map[String, String], list: List[String]) : Map[String, String] = {
      list match {
        case Nil => map
        case "-target" :: value :: tail =>
          nextOption(map ++ Map("target" -> value), tail)
        case "-output" :: value :: tail =>
          nextOption(map ++ Map("output" -> value), tail)
        case "-disabled-plugins-file" :: value :: tail =>
          nextOption(map ++ Map("plugins" -> value), tail)
        case option :: tail => 
          println("Unknown option " + option)
          System.exit(1)
          Map()
      }
    }
    val options = nextOption(Map(), args.toList)
    runBenchmarkAndOutput(options)
    println("Finished!")
    System.exit(0)
  }

  def runBenchmarkAndOutput(options: Map[String, String]) = {
    val pluginsToDisable = options.get("plugins").map(FileUtil.loadLines).map(_.toList).getOrElse(List())
    val timings = runBenchmark(options("target"), pluginsToDisable.toSet)
    val msg = s"Imported ${timings.target} in " +
              s"${timings.projectCreation / 1000}s / " +
              s"indexed in ${timings.projectIndexing / 1000}s"
    FileUtil.appendToFile(new File(options("output")), s"\n$msg")
    println(msg)
  }

  def runBenchmark(path: String, pluginsToDisable: Set[String]): Timings = {
    val address = PantsTargetAddress.fromString(path)
    val buildRoot = PantsUtil.findBuildRoot(new File(address.getPath))
    val benchmark = new PantsPerformanceBenchmark(buildRoot.get(), pluginsToDisable)
    benchmark.setName("performance benchmark")
    benchmark.setUp()
    try {
      benchmark.run(address.getRelativePath)
    }
    finally {
      try {
        benchmark.tearDown()
      }
      catch {
        case ignored: Throwable =>
      }
    }
  }
}

case class Timings(
  target: String,
  projectCreation: Long = 0L,
  projectIndexing: Long = 0L
)

class PantsPerformanceBenchmark(projectFolder: File, pluginsToDisable: Set[String]) extends PantsIntegrationTestCase {
  override protected def getProjectFolder = projectFolder

  override protected def getRequiredPluginIds = {
 //   val allPluginIds = PluginManagerCore.loadDescriptors().map(_.getPluginId.getIdString).toSet
   // (allPluginIds -- pluginsToDisable).toArray
    ???
  }

  def run(target: String): Timings = {
    println(s"Running performance test with ${PluginManagerCore.getPlugins.count(_.isEnabled)} plugins enabled.")
    val importEnd = new Ref(-1L)

    val messageBusConnection = myProject.getMessageBus.connect()
    messageBusConnection.subscribe(
      ProjectTopics.PROJECT_ROOTS,
      new ModuleRootListener {
        override def beforeRootsChange(event: ModuleRootEvent) = {
          // import ends with changing of all the roots
          importEnd.set(System.currentTimeMillis)
        }
      }
    )

    try {
      val importStart = System.currentTimeMillis
      doImport(target)
      DumbService.getInstance(myProject).waitForSmartMode()
      val indexingStart = importEnd.get()
      val indexingEnd   = System.currentTimeMillis

      Timings(
        target          = target,
        projectCreation = importEnd.get() - importStart,
        projectIndexing = indexingEnd - indexingStart
      )
    }
    finally {
      messageBusConnection.disconnect()
    }
  }
}
