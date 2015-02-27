// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsExecutionException;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PantsResolver {
  private static final Logger LOG = Logger.getInstance(PantsResolver.class);

  private final boolean generateJars;
  private final String projectPath;
  private final PantsExecutionSettings settings;

  @Nullable
  protected File myWorkDirectory = null;
  private ProjectInfo projectInfo = null;

  @Nullable
  public ProjectInfo getProjectInfo() {
    return projectInfo;
  }

  @TestOnly
  protected void setWorkDirectory(@Nullable File workDirectory) {
    myWorkDirectory = workDirectory;
  }

  @TestOnly
  protected void setProjectInfo(ProjectInfo projectInfo) {
    this.projectInfo = projectInfo;
  }

  public PantsResolver(@NotNull String projectPath, @NotNull PantsExecutionSettings settings, boolean isPreviewMode) {
    this.projectPath = projectPath;
    this.settings = settings;
    generateJars = !isPreviewMode;
  }

  public static ProjectInfo parseProjectInfoFromJSON(String data) throws JsonSyntaxException {
    return new Gson().fromJson(data, ProjectInfo.class);
  }

  private void parse(final String output) {
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

  public void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    if (projectInfo == null) return;

    for (PantsProjectInfoModifierExtension modifier : PantsProjectInfoModifierExtension.EP_NAME.getExtensions()) {
      modifier.modify(projectInfo, LOG);
    }

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();
    createAllEmptyModules(projectInfoDataNode, modules);
    addSourceRootsToModules(modules);
    addDependenciesToModules(modules);
    addLibrariesToModules(modules);
    runResolverExtensions(projectInfoDataNode, modules);
  }

  private void createAllEmptyModules(
    @NotNull DataNode<ProjectData> projectInfoDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
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
      final DataNode<ModuleData> moduleData =
        createModuleData(
          projectInfoDataNode,
          targetName,
          targetInfo.getRoots(),
          targetInfo.getSourcesType()
        );
      modules.put(targetName, moduleData);
    }
  }

  private void addSourceRootsToModules(@NotNull Map<String, DataNode<ModuleData>> modules) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String targetAddress = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(targetAddress) || targetInfo.getRoots().isEmpty()) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(targetAddress);

      final List<ContentRootData> contentRoots = findChildren(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      if (contentRoots.isEmpty()) {
        continue;
      }

      addSourceRootsToContentRoots(targetAddress, targetInfo, contentRoots);
      addExcludesToContentRoots(targetInfo, contentRoots);

      if (!settings.isCompileWithIntellij()) {
        addPantsJpsCompileOutputs(targetInfo, moduleDataNode);
      }
    }
  }

  private void addExcludesToContentRoots(@NotNull final TargetInfo targetInfo, @NotNull List<ContentRootData> remainingContentRoots) {
    if (PantsUtil.isResource(targetInfo.getSourcesType())) {
      return; // don't exclude subdirectories of resource sources
    }
    for (final ContentRootData contentRoot : remainingContentRoots) {
      addExcludes(
        targetInfo,
        contentRoot,
        ContainerUtil.findAll(
          targetInfo.getRoots(),
          new Condition<SourceRoot>() {
            @Override
            public boolean value(SourceRoot root) {
              return FileUtil.isAncestor(
                contentRoot.getRootPath(),
                root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()),
                false
              );
            }
          }
        )
      );
    }
  }

  private void addSourceRootsToContentRoots(
    @NotNull String targetAddress,
    @NotNull final TargetInfo targetInfo,
    @NotNull List<ContentRootData> contentRoots
  ) {
    for (final SourceRoot root : targetInfo.getRoots()) {
      final ContentRootData contentRootAncestorOfRoot = ContainerUtil.find(
        contentRoots, new Condition<ContentRootData>() {
          @Override
          public boolean value(ContentRootData contentRoot) {
            return FileUtil.isAncestor(contentRoot.getRootPath(), root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()), false);
          }
        }
      );
      if (contentRootAncestorOfRoot == null) {
        List<String> contentRootPaths = ContainerUtil.map(
          contentRoots, new Function<ContentRootData, String>() {
            @Override
            public String fun(ContentRootData contentRootData) {
              return contentRootData.getRootPath();
            }
          }
        );
        LOG.error(targetAddress + ": found source root: " +
                  root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()) + " outside content roots: " + contentRootPaths);
        continue;
      }

      addSourceRoot(contentRootAncestorOfRoot, root, targetInfo.getTargetType());
    }
  }

  private void addPantsJpsCompileOutputs(@NotNull TargetInfo targetInfo, @NotNull DataNode<ModuleData> moduleDataNode) {
    if (PantsUtil.isResource(targetInfo.getSourcesType())) {
      return;
    }
    String compilerOutputRelativePath = ".pants.d/compile/jvm/java/classes";
    if (targetInfo.isScalaTarget() || targetInfo.hasScalaLib()) {
      compilerOutputRelativePath = ".pants.d/compile/jvm/scala/classes";
    }
    else if (targetInfo.isAnnotationProcessorTarget()) {
      compilerOutputRelativePath = ".pants.d/compile/jvm/apt/classes";
    }
    final String absoluteCompilerOutputPath = new File(myWorkDirectory, compilerOutputRelativePath).getPath();
    final ModuleData moduleData = moduleDataNode.getData();
    moduleData.setInheritProjectCompileOutputPath(false);
    moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, absoluteCompilerOutputPath);
  }

  private void addDependenciesToModules(@NotNull Map<String, DataNode<ModuleData>> modules) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String target : targetInfo.getTargets()) {
        if (!modules.containsKey(target)) {
          continue;
        }
        // todo: is it always exported?
        addModuleDependency(moduleDataNode, modules.get(target), true);
      }
    }
  }

  private void addLibrariesToModules(@NotNull Map<String, DataNode<ModuleData>> modules) {
    final Map<String, LibraryData> idToLibraryData = new HashMap<String, LibraryData>();
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (final String libraryId : targetInfo.getLibraries()) {
        if (targetInfo.isScalaTarget() && PantsScalaUtil.isScalaLib(libraryId)) {
          // skip Scala. Will be added by PantsScalaDataService
          continue;
        }
        final List<String> libraryJars = getLibraryJars(libraryId);
        if (libraryJars.isEmpty()) {
          // no library jars by that id
          continue;
        }
        final LibraryData libraryData =
          ContainerUtil.getOrCreate(
            idToLibraryData,
            libraryId,
            new Factory<LibraryData>() {
              @Override
              public LibraryData create() {
                final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, libraryId);
                for (String jarPath : libraryJars) {
                  // todo: sources + docs
                  libraryData.addPath(LibraryPathType.BINARY, jarPath);
                }
                return libraryData;
              }
            }
          );
        addLibraryDependencyToModule(
          moduleDataNode,
          libraryData,
          true // todo: is it always exported?
        );
      }
    }
  }

  private void runResolverExtensions(@NotNull DataNode<ProjectData> projectInfoDataNode, @NotNull Map<String, DataNode<ModuleData>> modules) {
    for (String resolverClassName : settings.getResolverExtensionClassNames()) {
      try {
        Object resolver = Class.forName(resolverClassName).newInstance();
        if (resolver instanceof PantsResolverExtension) {
          ((PantsResolverExtension)resolver).resolve(projectInfo, projectInfoDataNode, modules);
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  private void addExcludes(
    @NotNull TargetInfo targetInfo,
    @NotNull final ContentRootData contentRoot,
    @NotNull List<SourceRoot> roots
  ) {
    final Set<File> rootFiles = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
    for (SourceRoot sourceRoot : roots) {
      rootFiles.add(new File(sourceRoot.getSourceRootRegardingSourceType(targetInfo.getSourcesType())));
    }

    for (File root : rootFiles) {
      PantsUtil.traverseDirectoriesRecursively(
        root,
        new Processor<File>() {
          @Override
          public boolean process(final File file) {
            if (!containsSourceRoot(file)) {
              contentRoot.storePath(ExternalSystemSourceType.EXCLUDED, file.getAbsolutePath());
              return false;
            }
            return true;
          }

          /**
           * Checks if {@code file} contains or is a source root.
           */
          private boolean containsSourceRoot(@NotNull File file) {
            for (File rootFile : rootFiles) {
              if (FileUtil.isAncestor(file, rootFile, false)) {
                return true;
              }
            }

            return false;
          }
        }
      );
    }
  }

  private void addSourceRoot(@NotNull ContentRootData contentRoot, @NotNull SourceRoot root, @Nullable String targetType) {
    try {
      final PantsSourceType rootType = PantsUtil.getSourceTypeForTargetType(targetType);
      final String packagePrefix = PantsUtil.isResource(rootType) ? null : root.getPackagePrefix();
      contentRoot.storePath(
        rootType.toExternalSystemSourceType(),
        root.getSourceRootRegardingSourceType(rootType),
        StringUtil.nullize(packagePrefix)
      );
    }
    catch (IllegalArgumentException e) {
      LOG.warn(e);
      // todo(fkorotkov): log and investigate exceptions from ContentRootData.storePath(ContentRootData.java:94)
    }
  }

  @NotNull
  private <T> List<T> findChildren(@NotNull DataNode<?> dataNode, @NotNull Key<T> key) {
    return ContainerUtil.mapNotNull(
      ExternalSystemApiUtil.findAll(dataNode, key),
      new Function<DataNode<T>, T>() {
        @Override
        public T fun(DataNode<T> node) {
          return node.getData();
        }
      }
    );
  }

  private void addModuleDependency(DataNode<ModuleData> moduleDataNode, DataNode<ModuleData> submoduleDataNode, boolean exported) {
    final List<ModuleDependencyData> subModuleDeps = findChildren(submoduleDataNode, ProjectKeys.MODULE_DEPENDENCY);
    for (ModuleDependencyData dep : subModuleDeps) {
      if (dep.getTarget().equals(moduleDataNode.getData())) {
        return;
      }
    }
    final ModuleDependencyData moduleDependencyData = new ModuleDependencyData(
      moduleDataNode.getData(),
      submoduleDataNode.getData()
    );
    moduleDependencyData.setExported(exported);
    moduleDataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
  }

  private static void addLibraryDependencyToModule(
    @NotNull DataNode<ModuleData> moduleDataNode,
    @NotNull LibraryData data,
    boolean exported
  ) {
    final LibraryDependencyData library = new LibraryDependencyData(
      moduleDataNode.getData(),
      data,
      LibraryLevel.MODULE
    );
    library.setExported(exported);
    moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
  }
  @NotNull
  private DataNode<ModuleData> createModuleData(
    @NotNull DataNode<ProjectData> projectInfoDataNode,
    @NotNull String targetName,
    @NotNull Collection<SourceRoot> roots,
    @NotNull final PantsSourceType rootType
  ) {
    final String moduleName = PantsUtil.getCanonicalModuleName(targetName);

    final ModuleData moduleData = new ModuleData(
      targetName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      moduleName,
      projectInfoDataNode.getData().getIdeProjectFileDirectoryPath() + "/" + moduleName,
      new File(myWorkDirectory, targetName).getAbsolutePath()
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    if (!roots.isEmpty()) {
      final Collection<SourceRoot> baseSourceRoots = new ArrayList<SourceRoot>();
      for (SourceRoot root : sortRootsAsPaths(roots, rootType)) {
        if (hasAnAncestorRoot(baseSourceRoots, root, rootType)) continue;
        baseSourceRoots.add(root);
      }

      for (SourceRoot baseRoot : baseSourceRoots) {
        final ContentRootData contentRoot = new ContentRootData(
          PantsConstants.SYSTEM_ID,
          baseRoot.getSourceRootRegardingSourceType(rootType)
        );
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
      }
    }

    return moduleDataNode;
  }

  @NotNull
  private List<String> getLibraryJars(@NotNull String libraryId) {
    final List<String> libraryJars = projectInfo.getLibraries(libraryId);
    if (libraryJars.isEmpty() && generateJars) {
      // log only we tried to resolve libs
      LOG.warn("No info for library: " + libraryId);
    }
    return libraryJars;
  }

  private static List<SourceRoot> sortRootsAsPaths(
    @NotNull Collection<SourceRoot> sourceRoots,
    @NotNull final PantsSourceType rootType
  ) {
    final List<SourceRoot> sortedRoots = new ArrayList<SourceRoot>(sourceRoots);
    Collections.sort(
      sortedRoots, new Comparator<SourceRoot>() {
        @Override
        public int compare(SourceRoot o1, SourceRoot o2) {
          final String rootPath1 = o1.getSourceRootRegardingSourceType(rootType);
          final String rootPath2 = o2.getSourceRootRegardingSourceType(rootType);
          return FileUtil.comparePaths(rootPath1, rootPath2);
        }
      }
    );
    return sortedRoots;
  }

  private boolean hasAnAncestorRoot(@NotNull Collection<SourceRoot> baseSourceRoots, @NotNull SourceRoot root, PantsSourceType rootType) {
    for (SourceRoot sourceRoot : baseSourceRoots) {
      if (FileUtil.isAncestor(sourceRoot.getSourceRootRegardingSourceType(rootType), root.getSourceRootRegardingSourceType(rootType), false)) {
        return true;
      }
    }
    return false;
  }

  public void resolve(@Nullable ProcessAdapter processAdapter) {
    try {
      final File outputFile = FileUtil.createTempFile("pants_run", ".out");
      final GeneralCommandLine command = getCommand(outputFile);
      final ProcessOutput processOutput = PantsUtil.getCmdOutput(command, processAdapter);
      if (processOutput.getStdout().contains("no such option")) {
        throw new ExternalSystemException("Pants doesn't have necessary APIs. Please upgrade you pants!");
      }
      final boolean stdOutDebugInfo = ApplicationManager.getApplication().isUnitTestMode() ||
                                      ApplicationManager.getApplication().isInternal();
      if (processOutput.checkSuccess(LOG)) {
        final String output = FileUtil.loadFile(outputFile);
        parse(output);

        final File bootstrapBuildFile = new File(command.getWorkDirectory(), "BUILD");
        if (bootstrapBuildFile.exists() && projectInfo != null && PantsScalaUtil.hasMissingScalaCompilerLibs(projectInfo)) {
          // need to bootstrap tools
          final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(bootstrapBuildFile.getPath());
          commandLine.addParameters("goal", "resolve", "BUILD:");
          final boolean bootstrapped = PantsUtil.getCmdOutput(commandLine, null).checkSuccess(LOG);
          if (bootstrapped && stdOutDebugInfo) {
            System.out.println("Bootstrapped Pants successfully!");
          }
        }
      }
      else {
        if (stdOutDebugInfo) {
          System.err.println("Pants execution failure!");
          System.err.println(processOutput.getStdout());
          System.err.println(processOutput.getStderr());
        }

        throw new PantsExecutionException("Failed to update the project!", processOutput);
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
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath);
    myWorkDirectory = commandLine.getWorkDirectory();
    commandLine.addParameter("goal");
    // in unit test mode it's always preview but we need to know libraries
    // because some jvm_binary targets are actually Scala ones and we need to
    // set a proper com.twitter.intellij.pants.compiler output folder
    if (generateJars || ApplicationManager.getApplication().isUnitTestMode()) {
      commandLine.addParameter("resolve");
    }
    String relativeProjectPath = PantsUtil.getRelativeProjectPath(myWorkDirectory, projectPath);

    if (relativeProjectPath == null) {
      throw new ExternalSystemException(
        String.format("Can't find relative path for a target %s from dir %s", projectPath, myWorkDirectory.getPath())
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
}
