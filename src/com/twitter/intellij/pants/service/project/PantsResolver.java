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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();

    // create all modules with source roots. no libs and dependencies
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

    for (PantsResolverExtension resolver : PantsResolverExtension.EP_NAME.getExtensions()) {
      resolver.resolve(projectInfo, modules);
    }
  }

  private void createLibraryData(@NotNull DataNode<ModuleData> moduleDataNode, String libraryId) {
    final List<String> libraryJars = projectInfo.getLibraries(libraryId);
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
      for (SourceRoot root : targetInfo.roots) {
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
        LOG.warn("Bad library id: " + libraryId);
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
      LOG.warn("No info for library: " + libraryId);
      return Collections.emptyList();
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
  }

  public static class SourceRoot {
    public String source_root;
    public String package_prefix;
  }
}
