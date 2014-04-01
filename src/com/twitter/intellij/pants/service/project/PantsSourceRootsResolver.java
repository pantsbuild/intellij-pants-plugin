package com.twitter.intellij.pants.service.project;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Collections;
import java.util.List;

public class PantsSourceRootsResolver extends PantsResolverBase {

  private List<String> roots = Collections.emptyList();

  public PantsSourceRootsResolver(String projectPath, PantsExecutionSettings settings) {
    super(projectPath, settings);
  }

  @Override
  protected void fillArguments(GeneralCommandLine commandLine) {
    commandLine.addParameter("goal");
    commandLine.addParameter("roots");
    for (String targetName : settings.getTargetNames()) {
      commandLine.addParameter(projectPath + ":" + targetName);
    }
  }

  @Override
  protected void parse(List<String> out, List<String> err) {
    roots = findRoots(out);
  }

  private List<String> findRoots(List<String> out) {
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (buildFile == null) {
      throw new ExternalSystemException("Couldn't find BUILD file: " + projectPath);
    }
    final VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(buildFile);
    if (pantsExecutable == null) {
      throw new ExternalSystemException("Couldn't find pants executable for: " + projectPath);
    }

    final String workingDir = pantsExecutable.getParent().getPath();
    final List<String> roots = ContainerUtil.map(out, new Function<String, String>() {
      @Override
      public String fun(String line) {
        final String[] parts = line.split(":");
        // todo: check targets(parts[1]) to get root type(sources, tests, etc.)
        return FileUtil.toSystemIndependentName(workingDir + "/" + parts[0]);
      }
    });
    final String buildFolder = buildFile.getParent().getPath();
    return ContainerUtil.findAll(roots, new Condition<String>() {
      @Override
      public boolean value(String root) {
        return root.startsWith(buildFolder);
      }
    });
  }

  @Override
  public void Resolving source rootaddInfo(DataNode<ProjectData> projectDataNode) {
    final String name = "test";
    final ModuleData moduleData = new ModuleData(
      name,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      name,
      PathUtil.getParentPath(projectPath),
      projectPath
    );
    final DataNode<ModuleData> moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData);

    final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, PathUtil.getParentPath(projectPath));
    for (String root : roots) {
      contentRoot.storePath(ExternalSystemSourceType.SOURCE, root);
    }

    moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
  }
}
