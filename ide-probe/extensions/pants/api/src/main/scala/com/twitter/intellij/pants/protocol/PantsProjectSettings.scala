package com.twitter.intellij.pants.protocol

import com.twitter.ideprobe.protocol.Setting

case class PantsProjectSettings(
    selectedTargets: Seq[String],
    loadSourcesAndDocsForLibs: Boolean,
    incrementalProjectImport: Boolean,
    useIdeaProjectJdk: Boolean,
    importSourceDepsAsJars: Boolean,
    useIntellijCompiler: Boolean
)

case class PantsProjectSettingsChangeRequest(
    selectedTargets: Setting[Seq[String]] = Setting.Unchanged,
    loadSourcesAndDocsForLibs: Setting[Boolean] = Setting.Unchanged,
    incrementalProjectImport: Setting[Boolean] = Setting.Unchanged,
    useIdeaProjectJdk: Setting[Boolean] = Setting.Unchanged,
    importSourceDepsAsJars: Setting[Boolean] = Setting.Unchanged,
    useIntellijCompiler: Setting[Boolean] = Setting.Unchanged
)
