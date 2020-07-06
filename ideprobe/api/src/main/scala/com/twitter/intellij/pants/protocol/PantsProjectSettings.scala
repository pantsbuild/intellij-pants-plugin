package com.twitter.intellij.pants.protocol

import org.virtuslab.ideprobe.protocol.Setting

case class PantsProjectSettings(
    selectedTargets: Seq[String],
    loadSourcesAndDocsForLibs: Boolean,
    incrementalProjectImportDepth: Option[Int],
    useIdeaProjectJdk: Boolean,
    importSourceDepsAsJars: Boolean,
    useIntellijCompiler: Boolean
)

case class PantsProjectSettingsChangeRequest(
    selectedTargets: Setting[Seq[String]] = Setting.Unchanged,
    loadSourcesAndDocsForLibs: Setting[Boolean] = Setting.Unchanged,
    incrementalProjectImportDepth: Setting[Option[Int]] = Setting.Unchanged,
    useIdeaProjectJdk: Setting[Boolean] = Setting.Unchanged,
    importSourceDepsAsJars: Setting[Boolean] = Setting.Unchanged,
    useIntellijCompiler: Setting[Boolean] = Setting.Unchanged
)
