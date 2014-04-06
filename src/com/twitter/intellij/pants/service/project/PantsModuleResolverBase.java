package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import org.jetbrains.annotations.NotNull;

abstract public class PantsModuleResolverBase extends PantsResolverBase {
  public PantsModuleResolverBase(@NotNull String projectPath, @NotNull PantsExecutionSettings settings) {
    super(projectPath, settings);
  }

  public abstract void addInfo(DataNode<ModuleData> moduleDataNode);
}
