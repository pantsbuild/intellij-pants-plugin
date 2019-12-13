// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings

;

import java.util

import com.google.common.collect.Lists
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.twitter.intellij.pants.model.PantsExecutionOptions;


object PantsExecutionSettings {
  def createDefault(): PantsExecutionSettings = new PantsExecutionSettings
}

case class PantsExecutionSettings(targetSpecs: util.List[java.lang.String] = Lists.newArrayList(),
                                  libsWithSourcesAndDocs: Boolean = false,
                                  useIdeaProjectJdk: Boolean = false,
                                  isImportSourceDepsAsJars: Boolean = false,
                                  isEnableIncrementalImport: Boolean = false,
                                  useIntellijCompiler: Boolean = false,
                                 ) extends ExternalSystemExecutionSettings with PantsExecutionOptions {
  override def getTargetSpecs: util.List[String] = targetSpecs
}
