// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala

import org.jetbrains.bsp.project.importing.BspResolverNamingExtension
import org.jetbrains.bsp.project.importing.BspResolverDescriptors.ModuleDescription

class PantsBspResolverNamingExtension extends BspResolverNamingExtension {

  def libraryData(moduleDescription: ModuleDescription): Option[String] = empty

  def libraryTestData(moduleDescription: ModuleDescription): Option[String] = empty

  private val empty = Some("\u200b")

}
