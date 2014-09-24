package com.twitter.intellij.pants.service.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsSourceType;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PantsResolver {
  private final boolean generateJars;
  private final String projectPath;
  private final PantsExecutionSettings settings;
  private final Logger LOG = Logger.getInstance(getClass());
  @Nullable
  protected File myWorkDirectory = null;
  private ProjectInfo projectInfo = null;

  public PantsResolver(@NotNull String projectPath, @NotNull PantsExecutionSettings settings, boolean isPreviewMode) {
    this.projectPath = projectPath;
    this.settings = settings;
    generateJars = !isPreviewMode;
  }

  public static ProjectInfo parseProjectInfoFromJSON(String data) throws JsonSyntaxException {
    return new Gson().fromJson(data, ProjectInfo.class);
  }

  private void parse(final String output, List<String> err) {
    projectInfo = null;
    if (output.isEmpty()) throw new ExternalSystemException("Not output from pants");
    try {
      projectInfo = parseProjectInfoFromJSON(output);
    }
    catch (JsonSyntaxException e) {
      LOG.warn("Can't parse output\n" + output, e);
      throw new ExternalSystemException("Can't parse project structure!");
    }
  }

  public void addInfo(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    if (projectInfo == null) return;

    // IntelliJ doesn't support when several modules have the same source root
    final Map<SourceRoot, String> modulesForRootsAndInfo = findTargetsForCommonRoots();

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();

    // create all modules. no libs, dependencies and source roots
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String targetName = entry.getKey();
      if (StringUtil.startsWith(targetName, ":scala-library")) {
        // we already have it in libs
        continue;
      }
      final TargetInfo targetInfo = entry.getValue();
      if (targetInfo.isEmpty()) {
        LOG.info("Skipping " + targetName + " because it is empty");
        continue;
      }
      final DataNode<ModuleData> moduleData = createModuleData(
        projectInfoDataNode, targetName, targetInfo
      );
      modules.put(targetName, moduleData);
    }

    // source roots
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget) || targetInfo.roots.isEmpty()) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      final ContentRootData contentRoot = findChildData(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      if (contentRoot == null) {
        LOG.warn("no content root for " + mainTarget);
        continue;
      }
      for (SourceRoot root : targetInfo.roots) {
        final String targetForSourceRoot = modulesForRootsAndInfo.get(root);
        final DataNode<ModuleData> sourceRootModule = targetForSourceRoot != null ? modules.get(targetForSourceRoot) : null;
        if (!mainTarget.equals(targetForSourceRoot) && sourceRootModule != null) {
          addModuleDependency(moduleDataNode, sourceRootModule);
          continue;
        }
        try {
          final PantsSourceType rootType = PantsUtil.getSourceTypeForTargetType(targetInfo.target_type);
          // resource source root shouldn't have a package prefix
          final String packagePrefix = PantsSourceType.isResource(rootType) ? null : StringUtil.nullize(root.package_prefix);
          contentRoot.storePath(rootType.toExternalSystemSourceType(), root.source_root, packagePrefix);
        }
        catch (IllegalArgumentException e) {
          LOG.warn(e);
          // todo(fkorotkov): log and investigate exceptions from ContentRootData.storePath(ContentRootData.java:94)
        }
      }
    }


    // add dependencies
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String target : targetInfo.targets) {
        if (!modules.containsKey(target)) {
          continue;
        }
        addModuleDependency(moduleDataNode, modules.get(target));
      }
    }

    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String libraryId : targetInfo.libraries) {
        // skip Scala. Will be added by ScalaPantsDataService
        if (!StringUtil.startsWith(libraryId, "org.scala-lang:scala-library")) {
          createLibraryData(moduleDataNode, libraryId);
        }
      }
    }

    for (String resolverClassName : settings.getResolverExtensionClassNames()) {
      try {
        Object resolver = Class.forName(resolverClassName).newInstance();
        if (resolver instanceof PantsResolverExtension) {
          ((PantsResolverExtension)resolver).resolve(projectInfo, modules);
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  @Nullable
  private <T> T findChildData(DataNode<?> dataNode, com.intellij.openapi.externalSystem.model.Key<T> key) {
    for (DataNode<?> child : dataNode.getChildren()) {
      T childData = child.getData(key);
      if (childData != null) {
        return childData;
      }
    }
    return null;
  }

  private void addModuleDependency(DataNode<ModuleData> moduleDataNode, DataNode<ModuleData> submoduleDataNode) {
    final ModuleDependencyData moduleDependencyData = new ModuleDependencyData(
      moduleDataNode.getData(),
      submoduleDataNode.getData()
    );
    // todo: is it always exported?
    moduleDependencyData.setExported(true);
    moduleDataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
  }

  private Map<SourceRoot, String> findTargetsForCommonRoots() {
    final Map<SourceRoot, List<Pair<String, TargetInfo>>> sourceRoot2Targets =
      new HashMap<SourceRoot, List<Pair<String, TargetInfo>>>();
    for (Map.Entry<String, TargetInfo> entry : projectInfo.targets.entrySet()) {
      final TargetInfo targetInfo = entry.getValue();
      for (SourceRoot sourceRoot : targetInfo.roots) {
        List<Pair<String, TargetInfo>> targetInfos = sourceRoot2Targets.get(sourceRoot);
        if (targetInfos == null) {
          targetInfos = new ArrayList<Pair<String, TargetInfo>>();
          sourceRoot2Targets.put(sourceRoot, targetInfos);
        }
        targetInfos.add(Pair.create(entry.getKey(), targetInfo));
      }
    }

    final Map<SourceRoot, String> result = new HashMap<SourceRoot, String>();

    for (Map.Entry<SourceRoot, List<Pair<String, TargetInfo>>> entry : sourceRoot2Targets.entrySet()) {
      final List<Pair<String, TargetInfo>> targetNameAndInfos = entry.getValue();
      if (targetNameAndInfos.size() > 1) {
        SourceRoot sourceRoot = entry.getKey();
        final Pair<String, TargetInfo> targetWithOneRoot = ContainerUtil.find(
          targetNameAndInfos,
          new Condition<Pair<String, TargetInfo>>() {
            @Override
            public boolean value(Pair<String, TargetInfo> info) {
              return info.getSecond().roots.size() == 1;
            }
          }
        );

        if (targetWithOneRoot != null) {
          result.put(sourceRoot, targetWithOneRoot.getFirst());
        } else {
          LOG.warn("Bad common source root " + sourceRoot);
        }
      }
    }

    return result;
  }

  private void createLibraryData(@NotNull DataNode<ModuleData> moduleDataNode, String libraryId) {
    final List<String> libraryJars = projectInfo.getLibraries(libraryId);
    if (libraryJars.isEmpty() && generateJars) {
      // log only we tried to resolve libs
      LOG.warn("No info for library: " + libraryId);
    }
    if (libraryJars.isEmpty()) {
      return;
    }
    final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, libraryId);
    for (String jarPath : libraryJars) {
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
    final int index = targetName.lastIndexOf(':');
    final String path = targetName.substring(0, index);

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

    final String moduleName = PantsUtil.getCanonicalModuleName(targetName);

    final ModuleData moduleData = new ModuleData(
      moduleName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      moduleName,
      projectInfoDataNode.getData().getIdeProjectFileDirectoryPath() + "/" + moduleName,
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
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
    }

    return moduleDataNode;
  }

  public void resolve(final ExternalSystemTaskId taskId, final ExternalSystemTaskNotificationListener listener) {
    try {
      final File outputFile = FileUtil.createTempFile("pants_run", ".out");
      final GeneralCommandLine command = getCommand(outputFile);
      final Process process = command.createProcess();
      final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
      processHandler.addProcessListener(
        new ProcessAdapter() {
          @Override
          public void onTextAvailable(ProcessEvent event, Key outputType) {
            listener.onTaskOutput(taskId, event.getText(), outputType == ProcessOutputTypes.STDOUT);
          }
        }
      );
      final ProcessOutput processOutput = processHandler.runProcess();
      if (processOutput.getStdout().contains("no such option")) {
        throw new ExternalSystemException("Pants doesn't have necessary APIs. Please upgrade you pants!");
      }
      if (processOutput.checkSuccess(LOG)) {
        final String output = FileUtil.loadFile(outputFile);
        parse(output, processOutput.getStderrLines());
      }
      else {
        throw new ExternalSystemException(
          "Failed to update the project!\n\n" + processOutput.getStdout() + "\n\n" + processOutput.getStderr()
        );
      }
    }
    catch (ExecutionException e) {
      throw new ExternalSystemException(e);
    }
    catch (IOException ioException) {
      throw new ExternalSystemException(ioException);
    }
  }

  protected GeneralCommandLine getCommand(final File outputFile) {
    try {
      final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath);
      myWorkDirectory = commandLine.getWorkDirectory();
      commandLine.addParameter("goal");
      if (generateJars) {
        commandLine.addParameter("resolve");
      }
      final File workDirectory = commandLine.getWorkDirectory();
      final File projectFile = new File(projectPath);
      final String relativeProjectPath =
        FileUtil.getRelativePath(workDirectory, projectFile.isDirectory() ? projectFile : projectFile.getParentFile());

      if (relativeProjectPath == null) {
        throw new ExternalSystemException(
          String.format("Can't find relative path for a target %s from dir %s", projectPath, workDirectory.getPath())
        );
      }

      commandLine.addParameter("depmap");
      if (settings.isAllTargets()) {
        commandLine.addParameter(relativeProjectPath + File.separator + "::");
      }
      else {
        for (String targetName : settings.getTargetNames()) {
          commandLine.addParameter(relativeProjectPath + File.separator + ":" + targetName);
        }
      }
      commandLine.addParameter("--depmap-project-info");
      commandLine.addParameter("--depmap-project-info-formatted");
      commandLine.addParameter("--depmap-output-file=" + outputFile.getPath());
      return commandLine;
    }
    catch (PantsException exception) {
      throw new ExternalSystemException(exception);
    }
  }

  public static class ProjectInfo {
    private final Logger LOG = Logger.getInstance(getClass());
    // id(org:name:version) to jars
    public Map<String, List<String>> libraries;
    // name to info
    public Map<String, TargetInfo> targets;

    public List<String> getLibraries(@NotNull String libraryId) {
      if (libraries.containsKey(libraryId) && libraries.get(libraryId).size() > 0) {
        return libraries.get(libraryId);
      }
      int versionIndex = libraryId.lastIndexOf(':');
      if (versionIndex == -1) {
        return Collections.emptyList();
      }
      final String libraryName = libraryId.substring(0, versionIndex);
      for (Map.Entry<String, List<String>> libIdAndJars : libraries.entrySet()) {
        final String currentLibraryId = libIdAndJars.getKey();
        if (!StringUtil.startsWith(currentLibraryId, libraryName)) {
          continue;
        }
        final List<String> currentJars = libIdAndJars.getValue();
        if (!currentJars.isEmpty()) {
          LOG.info("Using " + currentLibraryId + " instead of " + libraryId);
          return currentJars;
        }
      }
      return Collections.emptyList();
    }

    @Override
    public String toString() {
      return "ProjectInfo{" +
             "libraries=" + libraries +
             ", targets=" + targets +
             '}';
    }
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
     * Target type.
     */
    public String target_type;

    public boolean isEmpty() {
      return libraries.isEmpty() && targets.isEmpty() && roots.isEmpty();
    }

    @Override
    public String toString() {
      return "TargetInfo{" +
             "libraries=" + libraries +
             ", targets=" + targets +
             ", roots=" + roots +
             ", target_type='" + target_type + '\'' +
             '}';
    }
  }

  public static class SourceRoot {
    public String source_root;
    public String package_prefix;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SourceRoot root = (SourceRoot)o;

      if (package_prefix != null ? !package_prefix.equals(root.package_prefix) : root.package_prefix != null) return false;
      if (source_root != null ? !source_root.equals(root.source_root) : root.source_root != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = source_root != null ? source_root.hashCode() : 0;
      result = 31 * result + (package_prefix != null ? package_prefix.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "SourceRoot{" +
             "source_root='" + source_root + '\'' +
             ", package_prefix='" + package_prefix + '\'' +
             '}';
    }
  }
}
