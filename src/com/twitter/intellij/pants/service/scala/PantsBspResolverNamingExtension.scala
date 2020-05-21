// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala

import org.jetbrains.bsp.project.resolver.BspResolverNamingExtension
import org.jetbrains.bsp.project.resolver.BspResolverDescriptors.ModuleDescription

class PantsBspResolverNamingExtension extends BspResolverNamingExtension {

  def libraryData(moduleDescription: ModuleDescription): Option[String] = shortName("dependencies", moduleDescription)

  def libraryTestData(moduleDescription: ModuleDescription): Option[String] = shortName("test dependencies", moduleDescription)

  private def shortName(suffix: String, moduleDescription: ModuleDescription): Option[String] = {
    val name = moduleDescription.data.name
    val shortName = name.split(":").last
    Some(s"$shortName $suffix")
  }

}
