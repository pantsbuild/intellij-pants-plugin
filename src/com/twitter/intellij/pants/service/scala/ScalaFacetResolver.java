package com.twitter.intellij.pants.service.scala;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.service.project.PantsResolver.*;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScalaFacetResolver implements PantsResolverExtension {
  @Override
  public void resolve(
    ProjectInfo projectInfo, Map<String, DataNode<ModuleData>> modules
  ) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      if (moduleDataNode == null) {
        continue; // shouldn't happened because we created all modules for each target
      }
      for (String libraryId : targetInfo.libraries) {
        if (projectInfo.libraries.containsKey(libraryId) && PantsScalaUtil.isScalaLib(libraryId)) {
          // todo(fkorotkov): provide Scala info from the goal
          createScalaFacetFromJars(moduleDataNode, projectInfo.getLibraries(libraryId));

        }
      }
    }
  }

  private void createScalaFacetFromJars(@NotNull DataNode<ModuleData> moduleDataNode, List<String> scalaLibJars) {
    final ScalaModelData scalaModelData = new ScalaModelData(PantsConstants.SYSTEM_ID);
    final Set<File> files = new HashSet<File>();
    for (String jarPath : scalaLibJars) {
      for (String scalaLibNameToAdd : PantsScalaUtil.getScalaLibNamesToAdd()) {
        findAndAddScalaLib(files, jarPath, scalaLibNameToAdd);
      }
    }
    if (!files.isEmpty()) {
      scalaModelData.setScalaCompilerJars(files);
      moduleDataNode.createChild(ScalaModelData.KEY, scalaModelData);
    }
  }

  private void findAndAddScalaLib(Set<File> files, String jarPath, String libName) {
    final File compilerFile = new File(StringUtil.replace(jarPath, "scala-library", libName));
    if (compilerFile.exists()) {
      files.add(compilerFile);
    }
  }
}
