// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import com.twitter.intellij.pants.util.PantsUtil;
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

  public void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    if (projectInfo == null) return;
    projectInfo.fixCyclicDependencies();

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();

    // create all modules. no libs, dependencies and source roots
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
      final DataNode<ModuleData> moduleData = createModuleData(
        projectInfoDataNode, targetName, targetInfo.getRoots(), targetInfo.getSourcesType()
      );
      modules.put(targetName, moduleData);
    }

    // IntelliJ doesn't support when several modules have the same source root

    final Map<SourceRoot, DataNode<ModuleData>> modulesForRootsAndInfo = handleCommonRoots(projectInfoDataNode, modules);

    // source roots
    for (String mainTarget : projectInfo.getTargets().keySet()) {
      final TargetInfo targetInfo = projectInfo.getTarget(mainTarget);
      if (!modules.containsKey(mainTarget) || targetInfo.getRoots().isEmpty()) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      final ContentRootData contentRoot = findChildData(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      if (contentRoot == null) {
        LOG.warn("no content root for " + mainTarget);
        continue;
      }
      for (SourceRoot root : targetInfo.getRoots()) {
        final DataNode<ModuleData> sourceRootModule = modulesForRootsAndInfo.get(root);

        if (moduleDataNode != sourceRootModule && sourceRootModule != null) {
          // todo: is it always exported?
          addModuleDependency(moduleDataNode, sourceRootModule, true);
          continue;
        }
        addSourceRoot(contentRoot, root, targetInfo.getTargetType());
      }
      if (isEmpty(contentRoot)) {
        removeAllChildren(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      } else {
        addExcludes(
          contentRoot,
          ContainerUtil.findAll(
            targetInfo.getRoots(),
            new Condition<SourceRoot>() {
              @Override
              public boolean value(SourceRoot root) {
                return !modulesForRootsAndInfo.containsKey(root);
              }
            }
          )
        );
      }
    }


    // add dependencies
    for (String mainTarget : projectInfo.getTargets().keySet()) {
      final TargetInfo targetInfo = projectInfo.getTarget(mainTarget);
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

    // add libs
    for (String mainTarget : projectInfo.getTargets().keySet()) {
      final TargetInfo targetInfo = projectInfo.getTarget(mainTarget);
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String libraryId : targetInfo.getLibraries()) {
        if (targetInfo.isScalaTarget() && PantsScalaUtil.isScalaLib(libraryId)) {
          // skip Scala. Will be added by PantsScalaDataService
          continue;
        }
        // todo: is it always exported?
        createLibraryData(moduleDataNode, libraryId, true);
      }
    }

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
    @NotNull final ContentRootData contentRoot,
    @NotNull List<SourceRoot> roots
  ) {
    final Set<File> rootFiles = new HashSet<File>();
    for (SourceRoot sourceType : roots) {
      rootFiles.add(new File(sourceType.getSourceRootRegardingSourceType(null)));
    }

    for (File root : rootFiles) {
      PantsUtil.traverseFilesRecursively(
        root,
        new Processor<File>() {
          @Override
          public boolean process(final File file) {
            if (file.isDirectory() && !containsSourceRoot(file)) {
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

  private boolean isEmpty(@NotNull ContentRootData contentRoot) {
    for (ExternalSystemSourceType sourceType : ExternalSystemSourceType.values()) {
      if (!contentRoot.getPaths(sourceType).isEmpty()) {
        return false;
      }
    }

    return true;
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

  private <T> List<T> findChildren(DataNode<?> dataNode, com.intellij.openapi.externalSystem.model.Key<T> key) {
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

  private <T> void removeAllChildren(DataNode<?> dataNode, com.intellij.openapi.externalSystem.model.Key<T> key) {
    final Collection<DataNode<T>> toRemove = ExternalSystemApiUtil.findAll(dataNode, key);
    dataNode.getChildren().removeAll(toRemove);
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

  private Map<SourceRoot, DataNode<ModuleData>> handleCommonRoots(
    DataNode<ProjectData> projectInfoDataNode,
    Map<String, DataNode<ModuleData>> modules
  ) {
    final Map<SourceRoot, List<Pair<String, TargetInfo>>> sourceRoot2Targets = projectInfo.getSourceRoot2TargetMapping();
    final Map<SourceRoot, DataNode<ModuleData>> result = new HashMap<SourceRoot, DataNode<ModuleData>>();

    for (Map.Entry<SourceRoot, List<Pair<String, TargetInfo>>> entry : sourceRoot2Targets.entrySet()) {
      final List<Pair<String, TargetInfo>> targetNameAndInfos = entry.getValue();
      if (targetNameAndInfos.size() > 1) {
        final SourceRoot originalSourceRoot = entry.getKey();
        final Pair<String, TargetInfo> targetWithOneRoot = ContainerUtil.find(
          targetNameAndInfos,
          new Condition<Pair<String, TargetInfo>>() {
            @Override
            public boolean value(Pair<String, TargetInfo> info) {
              return info.getSecond().getRoots().size() == 1;
            }
          }
        );

        if (targetWithOneRoot != null) {
          final DataNode<ModuleData> moduleDataDataNode = modules.get(targetWithOneRoot.getFirst());
          if (moduleDataDataNode != null) {
            LOG.debug("Found common source root target " + targetWithOneRoot.getFirst());
            result.put(originalSourceRoot, moduleDataDataNode);
          }
          else {
            LOG.warn("Bad common source root " + originalSourceRoot + " for " + targetWithOneRoot.getFirst());
          }
        }
        else {
          final Iterator<Pair<String, TargetInfo>> iterator = targetNameAndInfos.iterator();
          TargetInfo commonInfo = iterator.next().getSecond();
          while (iterator.hasNext()) {
            commonInfo = commonInfo.intersect(iterator.next().getSecond());
          }

          // we need to check that all other modules for all other common roots
          // have this root in their roots
          final List<SourceRoot> filteredCommonRoots =
            ContainerUtil.findAll(
              commonInfo.getRoots(),
              new Condition<SourceRoot>() {
                @Override
                public boolean value(SourceRoot root) {
                  for (Pair<String, TargetInfo> pair : sourceRoot2Targets.get(root)) {
                    final TargetInfo targetInfo = pair.getSecond();
                    if (!targetInfo.getRoots().contains(originalSourceRoot)) {
                      // Example:
                      // Target A has roots a and b, Target B has a and b too
                      // but Target C has b only and c only. Then we can't extract a and b.
                      // a and b should be in a separate module
                      return false;
                    }
                  }

                  return true;
                }
              }
            );
          commonInfo.setRoots(new HashSet<SourceRoot>(filteredCommonRoots));
          final PantsSourceType commonSourceRootType = PantsUtil.getSourceTypeForTargetType(commonInfo.getTargetType());

          final String commonPath = PantsUtil.findCommonRoot(
            ContainerUtil.map(
              commonInfo.getRoots(), new Function<SourceRoot, String>() {
                @Override
                public String fun(SourceRoot root) {
                  // get as a resource to get a pretty name
                  final String path = root.getSourceRootRegardingSourceType(PantsSourceType.RESOURCE);
                  return myWorkDirectory == null ? path :
                         FileUtil.getRelativePath(myWorkDirectory.getPath(), path, File.separatorChar);
                }
              }
            )
          );

          final String commonRootsFakeTargetName = commonInfo.getRoots().size() == 1 ?
                                                   commonInfo.getRoots().iterator().next().getPackagePrefix() :
                                                   "common_packages";
          final String commonName = commonPath + ":" + commonRootsFakeTargetName;
          final DataNode<ModuleData> rootModuleData =
            createModuleData(
              projectInfoDataNode,
              commonName,
              commonInfo.getRoots(),
              commonSourceRootType
            );

          final ContentRootData contentRoot = findChildData(rootModuleData, ProjectKeys.CONTENT_ROOT);
          if (contentRoot != null) {
            for (SourceRoot sourceRoot : commonInfo.getRoots()) {
              addSourceRoot(contentRoot, sourceRoot, commonInfo.getTargetType());
            }
          }

          for (SourceRoot root : commonInfo.getRoots()) {
            result.put(root, rootModuleData);
          }
          modules.put(commonName, rootModuleData);
          projectInfo.getTargets().put(commonName, commonInfo);
        }
      }
    }

    return result;
  }

  private void createLibraryData(@NotNull DataNode<ModuleData> moduleDataNode, String libraryId, boolean exported) {
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
    library.setExported(exported);
    moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
  }

  @NotNull
  private DataNode<ModuleData> createModuleData(
    DataNode<ProjectData> projectInfoDataNode,
    String targetName,
    Collection<SourceRoot> roots,
    @Nullable PantsSourceType rootType
  ) {
    final int index = targetName.lastIndexOf(':');
    final String path = targetName.substring(0, index);
    return createModuleData(projectInfoDataNode, targetName, path, roots, rootType);
  }

  @NotNull
  private DataNode<ModuleData> createModuleData(
    DataNode<ProjectData> projectInfoDataNode,
    String targetName,
    @NotNull String path,
    Collection<SourceRoot> roots,
    @Nullable final PantsSourceType rootType
  ) {
    final String contentRootPath = StringUtil.notNullize(
      PantsUtil.findCommonRoot(
        ContainerUtil.map(
          roots,
          new Function<SourceRoot, String>() {
            @Override
            public String fun(SourceRoot root) {
              return root.getSourceRootRegardingSourceType(rootType);
            }
          }
        )
      ),
      path
    );

    final File[] files = myWorkDirectory != null ? new File(myWorkDirectory, path).listFiles() : null;
    final File BUILDFile = files == null ? null : ContainerUtil.find(
      files,
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
        BUILDFile == null ? null : FileUtil.getRelativePath(myWorkDirectory, BUILDFile),
        path
      )
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    if (!roots.isEmpty()) {
      final ContentRootData contentRoot = new ContentRootData(
        PantsConstants.SYSTEM_ID,
        contentRootPath
      );
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
    }

    return moduleDataNode;
  }

  public void resolve(@Nullable ProcessAdapter processAdapter) {
    try {
      final File outputFile = FileUtil.createTempFile("pants_run", ".out");
      final GeneralCommandLine command = getCommand(outputFile);
      final Process process = command.createProcess();
      final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
      if (processAdapter != null) {
        processHandler.addProcessListener(processAdapter);
      }
      final ProcessOutput processOutput = processHandler.runProcess();
      if (processOutput.getStdout().contains("no such option")) {
        throw new ExternalSystemException("Pants doesn't have necessary APIs. Please upgrade you pants!");
      }
      if (processOutput.checkSuccess(LOG)) {
        final String output = FileUtil.loadFile(outputFile);
        parse(output, processOutput.getStderrLines());
      }
      else {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          System.err.println("Pants execution failure!");
          System.err.println(processOutput.getStdout());
          System.err.println(processOutput.getStderr());
        }
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
      String relativeProjectPath = PantsUtil.getRelativeProjectPath(projectPath, myWorkDirectory);

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
    catch (PantsException exception) {
      throw new ExternalSystemException(exception);
    }
  }
}
