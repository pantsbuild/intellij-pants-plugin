package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;

import java.util.Map;

public interface PantsResolverExtension {
  ExtensionPointName<PantsResolverExtension> EP_NAME = ExtensionPointName.create("com.intellij.plugins.pants.projectResolver");

  public void resolve(
    PantsResolver.ProjectInfo projectInfo, Map<String, DataNode<ModuleData>> modules
  );
}
