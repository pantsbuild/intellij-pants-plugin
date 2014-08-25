package com.twitter.intellij.pants.service.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.scala.ScalaModelData;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class PantsResolver extends PantsResolverBase {

  private final boolean generateJars;
  private ProjectInfo projectInfo = null;

  public PantsResolver(@NotNull String projectPath, @NotNull PantsExecutionSettings settings, boolean isPreviewMode) {
    super(projectPath, settings);
    generateJars = !isPreviewMode;
  }

  public static ProjectInfo parseProjectInfoFromJSON(String data) throws JsonSyntaxException {
    return new Gson().fromJson(data, ProjectInfo.class);
  }

  @Override
  protected void fillArguments(GeneralCommandLine commandLine) {
    commandLine.addParameter("goal");
    if (generateJars) {
      commandLine.addParameter("resolve");
    }
    final File workDirectory = commandLine.getWorkDirectory();
    final String relativeProjectPath = FileUtil.getRelativePath(workDirectory, new File(projectPath).getParentFile());

    if (relativeProjectPath == null) {
      throw new ExternalSystemException(
        String.format("Can't find relative path for a target %s from dir %s", projectPath, workDirectory.getPath())
      );
    }

    commandLine.addParameter("depmap");
    for (String targetName : settings.getTargetNames()) {
      commandLine.addParameter(relativeProjectPath + File.separator + ":" + targetName);
    }
    commandLine.addParameter("--depmap-project-info");
    commandLine.addParameter("--depmap-project-info-formatted");
  }

  @Override
  protected void parse(List<String> out, List<String> err) {
    projectInfo = null;
    if (out.isEmpty()) throw new ExternalSystemException("Not output from pants");
    final String data = out.iterator().next();
    try {
      projectInfo = parseProjectInfoFromJSON(data);
    }
    catch (JsonSyntaxException e) {
      throw new ExternalSystemException("Can't parse output\n" + data, e);
    }
  }

  public void addInfo(DataNode<ProjectData> projectInfoDataNode) {
    if (projectInfo == null) return;

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();

    // create all modules with source roots. no libs and dependencies
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String targetName = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      final DataNode<ModuleData> moduleData = createModuleData(
        projectInfoDataNode, targetName, targetInfo
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
        // todo: is it always exported?
        moduleDependencyData.setExported(true);
        moduleDataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
      }
    }

    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String libraryId : targetInfo.libraries) {
        if (!projectInfo.libraries.containsKey(libraryId)) {
          // todo: investigate
          continue;
        }
        // todo(fkorotkov): provide Scala info from the goal
        if (StringUtil.startsWith(libraryId, "org.scala-lang:scala-library")) {
          createScalaFacet(moduleDataNode, libraryId);
        }
        createLibraryData(moduleDataNode, libraryId);
      }
    }
  }

  private void createScalaFacet(DataNode<ModuleData> moduleDataNode, String libraryId) {
    final ScalaModelData scalaModelData = new ScalaModelData(PantsConstants.SYSTEM_ID);
    final Set<File> files = new HashSet<File>();
    for (String jarPath : projectInfo.libraries.get(libraryId)) {
      final File libFile = new File(jarPath);
      if (libFile.exists()) {
        files.add(libFile);
      }
      final File compilerFile = new File(StringUtil.replace(jarPath, "scala-library", "scala-compiler"));
      if (compilerFile.exists()) {
        files.add(compilerFile);
      }
    }
    scalaModelData.setScalaCompilerJars(files);
    moduleDataNode.createChild(ScalaModelData.KEY, scalaModelData);
  }

  private void createLibraryData(DataNode<ModuleData> moduleDataNode, String libraryId) {
    final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, libraryId);
    for (String jarPath : projectInfo.libraries.get(libraryId)) {
      // todo: sources + docs
      libraryData.addPath(LibraryPathType.BINARY, jarPath);
    }
    final LibraryDependencyData library = new LibraryDependencyData(
      moduleDataNode.getData(),
      libraryData,
      LibraryLevel.MODULE
    );
    // todo: is it always exported?
    library.setExported(true);
    moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
  }

  private DataNode<ModuleData> createModuleData(DataNode<ProjectData> projectInfoDataNode, String targetName, TargetInfo targetInfo) {
    final String[] pathAndTarget = targetName.split(":");
    final String path = pathAndTarget[0];

    final String contentRootPath = StringUtil.notNullize(
      PantsUtil.findCommonRoot(
        ContainerUtil.map(
          targetInfo.roots,
          new Function<SourceRoot, String>() {
            @Override
            public String fun(SourceRoot root) {
              return root.source_root;
            }
          }
        )
      ),
      path
    );

    final File BUILDFile = ContainerUtil.find(
      new File(myWorkDirectory, path).listFiles(),
      new Condition<File>() {
        @Override
        public boolean value(File file) {
          return PantsUtil.isBUILDFileName(file.getName());
        }
      }
    );

    final ModuleData moduleData = new ModuleData(
      targetName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      targetName,
      contentRootPath,
      StringUtil.notNullize(
        FileUtil.getRelativePath(myWorkDirectory, BUILDFile),
        path
      )
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    if (!targetInfo.roots.isEmpty()) {
      final ContentRootData contentRoot = new ContentRootData(
        PantsConstants.SYSTEM_ID,
        contentRootPath
      );
      for (SourceRoot root : targetInfo.roots) {
        final ExternalSystemSourceType source =
          targetInfo.test_target ? ExternalSystemSourceType.TEST : ExternalSystemSourceType.SOURCE;
        contentRoot.storePath(source, root.source_root, StringUtil.nullize(root.package_prefix));
      }
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
    }

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
    public List<SourceRoot> roots;
    /**
     * Is test target
     */
    public boolean test_target;
  }

  public static class SourceRoot {
    public String source_root;
    public String package_prefix;
  }
}
