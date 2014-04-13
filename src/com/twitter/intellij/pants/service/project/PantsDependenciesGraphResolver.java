package com.twitter.intellij.pants.service.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PantsDependenciesGraphResolver extends PantsResolverBase {

  private final boolean generateJars;
  private ProjectInfo projectInfo = null;

  public PantsDependenciesGraphResolver(@NotNull String projectPath, @NotNull PantsExecutionSettings settings, boolean isPreviewMode) {
    super(projectPath, settings);
    generateJars = !isPreviewMode;
  }

  @Override
  protected void fillArguments(GeneralCommandLine commandLine) {
    commandLine.addParameter("goal");
    if (generateJars) {
      commandLine.addParameter("resolve");
    }
    commandLine.addParameter("dependencies-graph");
    for (String targetName : settings.getTargetNames()) {
      if ("".equals(targetName)) {
        // otherwise pants lists everything twice it seems.
        commandLine.addParameter(projectPath);
      } else {
        commandLine.addParameter(projectPath + ":" + targetName);
      }
    }
    commandLine.addParameter("--no-dependencies-graph-format");
  }

  @Override
  protected void parse(List<String> out, List<String> err) {
    projectInfo = null;
    if (out.isEmpty()) throw new ExternalSystemException("Not output from pants");
    final String data = out.iterator().next();
    try {
      projectInfo = parseProjectInfoFromJSON(data);
    } catch (JsonSyntaxException e) {
      throw new ExternalSystemException("Can't parse output", e);
    }
  }

  public static ProjectInfo parseProjectInfoFromJSON(String data) throws JsonSyntaxException {
    return new Gson().fromJson(data, ProjectInfo.class);
  }

  public void addInfo(DataNode<ProjectData> projectInfoDataNode) {
    if (projectInfo == null) return;

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();

    // create all modules with source roots. no libs and dependencies
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String targetName = entry.getKey();
      final DataNode<ModuleData> moduleData = createModuleData(
        projectInfoDataNode, targetName, entry.getValue()
      );
      modules.put(targetName, moduleData);
    }

    // add dependencies
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String target : targetInfo.targets) {
        if (!modules.containsKey(target)) {
          // todo: investigate
          continue;
        }
        final DataNode<ModuleData> submoduleDataNode = modules.get(target);
        final ModuleDependencyData moduleDependencyData = new ModuleDependencyData(
          moduleDataNode.getData(),
          submoduleDataNode.getData()
        );
        moduleDataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
      }
    }

    // todo: add libs
  }

  private DataNode<ModuleData> createModuleData(DataNode<ProjectData> projectInfoDataNode, String targetName, TargetInfo targetInfo) {
    final String[] pathAndTarget = targetName.split(":");
    final String path = pathAndTarget[0];
    final ModuleData moduleData = new ModuleData(
      targetName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      targetName,
      PathUtil.getParentPath(path),
      path
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, PathUtil.getParentPath(projectPath));
    for (String root : targetInfo.roots) {
      final ExternalSystemSourceType source =
        targetInfo.test_target ? ExternalSystemSourceType.TEST : ExternalSystemSourceType.SOURCE;
      contentRoot.storePath(source, root);
    }
    moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);

    return moduleDataNode;
  }

  public static class ProjectInfo {
    // id(org:name:version) to jars
    public Map<String, List<String>> libraries;
    // name to info
    public Map<String, TargetInfo> targets;
  }

  public static class TargetInfo {
    /**
     * List of libraries. Just names.
     */
    public List<String> libraries;
    /**
     * List of dependencies.
     */
    public List<String> targets;
    /**
     * List of source roots.
     */
    public List<String> roots;
    /**
     * Is test target
     */
    public boolean test_target;
  }
}
