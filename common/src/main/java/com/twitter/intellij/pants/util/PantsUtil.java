// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.model.PantsVersion;
import com.twitter.intellij.pants.model.SimpleExportResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.impl.sdk.JpsSdkImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PantsUtil {
  public static final Gson gson = new Gson();
  public static final Type TYPE_LIST_STRING = new TypeToken<List<String>>() {
  }.getType();
  public static final Type TYPE_SET_STRING = new TypeToken<Set<String>>() {
  }.getType();
  public static final Type TYPE_MAP_STRING_INTEGER = new TypeToken<Map<String, Integer>>() {
  }.getType();
  public static final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory() {
      @Override
      public Thread newThread(@NotNull Runnable r) {
        return new Thread(r, "Pants-Plugin-Pool");
      }
    });

  private static final Logger LOG = Logger.getInstance(PantsUtil.class);
  private static final List<String> PYTHON_PLUGIN_IDS = ContainerUtil.immutableList("PythonCore", "Pythonid");
  private static final String PANTS_VERSION_REGEXP = "pants_version: (.+)";
  private static final String PEX_RELATIVE_PATH = ".pants.d/bin/pants.pex";

  /**
   * This aims to prepares for any breakage we might introduce from pants side, in which case we can adjust the version
   * of Pants `idea-plugin` goal to be greater than 0.1.0.
   *
   * @see <a href="https://github.com/pantsbuild/pants/blob/d31ec5b4b1fb4f91e5beb685539ea14278dc62cf/src/python/pants/backend/project_info/tasks/idea_plugin_gen.py#L28">Pants `idea-plugin` goal version</a>
   */
  private static final String PANTS_IDEA_PLUGIN_VERESION_MIN = "0.0.1";
  private static final String PANTS_IDEA_PLUGIN_VERESION_MAX = "0.1.0";

  /**
   * @param vFile a virtual file pointing at either a file or a directory
   * @return <code>Optional.empty()</code> if `vFile` is not a BUILD file or if it is a directory that
   * does not contain one
   * @deprecated {@link #findBUILDFiles(VirtualFile)} should be used instead, as this is likely
   * a sign that you're missing BUILD files
   */
  @Deprecated
  public static Optional<VirtualFile> findBUILDFile(@Nullable VirtualFile vFile) {
    return findBUILDFiles(vFile).stream().findFirst();
  }

  /**
   * @param vFile a virtual file pointing at either a file or a directory
   * @return a collection with one item if `vFile` is a valid BUILD file, an empty collection
   * if `vFile` is a file but not a valid BUILD file, or if `vFile` is a directory then all
   * the valid build files that are in it.
   */
  @NotNull
  public static Collection<VirtualFile> findBUILDFiles(@NotNull VirtualFile vFile) {
    if (vFile.isDirectory()) {
      return Stream.of(vFile.getChildren()).filter(f -> isBUILDFileName(f.getName())).collect(Collectors.toList());
    }

    if (isBUILDFileName(vFile.getName())) {
      return Collections.singleton(vFile);
    }

    return Collections.emptyList();
  }


  public static boolean isBUILDFilePath(@NotNull String path) {
    return isBUILDFileName(PathUtil.getFileName(path));
  }

  private static boolean isBUILDFile(@NotNull VirtualFile virtualFile) {
    return !virtualFile.isDirectory() && isBUILDFileName(virtualFile.getName());
  }

  public static boolean isBUILDFileName(@NotNull String name) {
    return StringUtil.equalsIgnoreCase(PantsConstants.BUILD, FileUtil.getNameWithoutExtension(name));
  }

  /**
   * Checks if it's a BUILD file or folder under a Pants project
   * Does not consider if the file is a BUILD file under a Pants project, only
   * that the file is a directory under a pants project.
   *
   * @param file - a BUILD file or a directory
   */
  public static boolean isPantsProjectFile(VirtualFile file) {
    if (file.isDirectory()) {
      return findPantsExecutable(file).isPresent();
    }
    return isBUILDFileName(file.getName());
  }

  /**
   * Almost exactly like isPantsProjectFile, but checks that the BUILD
   * file is under a Pants project.
   *
   * @param file - a BUILD file
   */
  public static boolean isFileUnderPantsRepo(VirtualFile file) {
    return findPantsExecutable(file).isPresent();
  }

  public static boolean isScalaRelatedTestRunConfiguration(RunConfiguration rc) {
    return rc.getClass().getPackage().getName().startsWith(PantsConstants.SCALA_PLUGIN_PACKAGE_TEST_PREFIX);
  }

  public static Optional<String> findPantsVersion(Optional<VirtualFile> workingDir) {
    final Optional<VirtualFile> pantsIniFile = findPantsIniFile(workingDir);
    return pantsIniFile.flatMap(PantsUtil::findVersionInFile);
  }

  public static Optional<VirtualFile> findPantsIniFile(Optional<VirtualFile> workingDir) {
    return workingDir.map(file -> file.findChild(PantsConstants.PANTS_INI));
  }

  public static Optional<VirtualFile> findPantsTomlFile(Optional<VirtualFile> workingDir) {
    return workingDir.map(file -> file.findChild(PantsConstants.PANTS_TOML));
  }

  public static boolean isCompatibleProjectPantsVersion(String projectPath, String minVersion) {
    return PantsUtil.findPantsExecutable(projectPath)
      .flatMap(exec -> PantsOptions.getPantsOptions(exec.getPath()).get("pants_version"))
      .map(version -> PantsUtil.isCompatiblePantsVersion(version, minVersion))
      .orElse(false);
  }

  public static boolean isCompatiblePantsVersion(String current, String minimum) {
    PantsVersion version1 = new PantsVersion(current);
    PantsVersion version2 = new PantsVersion(minimum);
    return version1.compareTo(version2) >= 0;
  }

  private static Optional<String> findVersionInFile(@NotNull VirtualFile file) {
    try {
      final String fileContent = VfsUtilCore.loadText(file);
      final List<String> matches = StringUtil.findMatches(
        fileContent, Pattern.compile(PANTS_VERSION_REGEXP)
      );
      return matches.stream().findFirst();
    }
    catch (IOException e) {
      return Optional.empty();
    }
  }

  public static Optional<VirtualFile> findFolderWithPex() {
    return findFolderWithPex(Optional.ofNullable(VfsUtil.getUserHomeDir()));
  }

  public static Optional<VirtualFile> findFolderWithPex(Optional<VirtualFile> userHomeDir) {
    return findFileRelativeToDirectory(PEX_RELATIVE_PATH, userHomeDir);
  }

  public static Optional<VirtualFile> findPexVersionFile(@NotNull VirtualFile folderWithPex, @NotNull String pantsVersion) {
    final String filePrefix = "pants-" + pantsVersion;
    return Optional.ofNullable(ContainerUtil.find(
      folderWithPex.getChildren(), new Condition<VirtualFile>() {
        @Override
        public boolean value(VirtualFile virtualFile) {
          return "pex".equalsIgnoreCase(virtualFile.getExtension()) && virtualFile.getName().startsWith(filePrefix);
        }
      }
    ));
  }

  public static Optional<VirtualFile> getFileInSelectedEditor(@Nullable Project project) {
    Optional<Editor> editor = Optional.ofNullable(project)
      .flatMap(p -> Optional.ofNullable(FileEditorManager.getInstance(p).getSelectedTextEditor()));
    Optional<EditorImpl> editorImpl = editor
      .flatMap(e -> e instanceof EditorImpl ? Optional.of((EditorImpl) e) : Optional.empty());
    return editorImpl.map(EditorImpl::getVirtualFile);
  }

  public static Optional<VirtualFile> getFileForEvent(@Nullable AnActionEvent e) {
    return Optional.ofNullable(e)
      .flatMap(ev -> Optional.ofNullable(ev.getData(CommonDataKeys.VIRTUAL_FILE)));
  }

  public static Optional<Module> getModuleForFile(@NotNull VirtualFile file, @NotNull Project project) {
    return Optional.ofNullable(ModuleUtil.findModuleForFile(file, project));
  }

  public static Optional<File> findBuildRoot(@NotNull File file) {
    return findPantsExecutable(file).map(File::getParentFile);
  }

  public static Optional<VirtualFile> findBuildRoot(@NotNull String filePath) {
    return findPantsExecutable(filePath).map(VirtualFile::getParent);
  }

  public static Optional<VirtualFile> findBuildRoot(@NotNull Project project) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      Optional<VirtualFile> buildRoot = findBuildRoot(module);
      if (buildRoot.isPresent()) {
        return buildRoot;
      }
    }
    return Optional.empty();
  }

  public static Optional<VirtualFile> findBuildRoot(@NotNull PsiFile psiFile) {
    final VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
    return virtualFile != null ? findBuildRoot(virtualFile) : findBuildRoot(psiFile.getProject());
  }

  public static Optional<VirtualFile> findBuildRoot(@NotNull Module module) {
    final VirtualFile moduleFile = module.getModuleFile();
    Optional<VirtualFile> fromModuleFile = Optional.ofNullable(moduleFile).flatMap(PantsUtil::findBuildRoot);
    if (fromModuleFile.isPresent()) {
      return fromModuleFile;
    }
    else {
      final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
      for (VirtualFile contentRoot : rootManager.getContentRoots()) {
        final Optional<VirtualFile> buildRoot = findBuildRoot(contentRoot);
        if (buildRoot.isPresent()) {
          return buildRoot;
        }
      }
      for (ContentEntry contentEntry : rootManager.getContentEntries()) {
        VirtualFile contentEntryFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(contentEntry.getUrl());
        final Optional<VirtualFile> buildRoot = findBuildRoot(contentEntryFile);
        if (buildRoot.isPresent()) {
          return buildRoot;
        }
      }
      return Optional.empty();
    }
  }

  public static Optional<VirtualFile> findBuildRoot(@Nullable VirtualFile file) {
    return findPantsExecutable(file).map(VirtualFile::getParent);
  }

  public static Optional<VirtualFile> findDistExportClasspathDirectory(@NotNull Project project) {
    Optional<VirtualFile> buildRoot = findBuildRoot(project);
    return buildRoot
      .map(s -> {
             String exportClasspathDir = s.getPath() + File.separator + "dist" + File.separator + "export-classpath";
             return LocalFileSystem.getInstance().refreshAndFindFileByPath(exportClasspathDir);
           }
      );
  }

  public static Optional<VirtualFile> findProjectManifestJar(@NotNull Project project) {
    Optional<VirtualFile> classpathDir = findDistExportClasspathDirectory(project);
    return classpathDir.flatMap(
      file -> {
        String manifestUrl = file.getUrl() + "/manifest.jar";
        VirtualFile manifest = VirtualFileManager.getInstance().refreshAndFindFileByUrl(manifestUrl);
        return Optional.ofNullable(manifest);
      });
  }

  public static GeneralCommandLine defaultCommandLine(@NotNull Project project) throws PantsException {
    Optional<VirtualFile> pantsExecutable = findPantsExecutable(project);
    return defaultCommandLine(
      pantsExecutable.orElseThrow(
        () -> new PantsException("Couldn't find pants executable for: " + project.getProjectFilePath())).getPath()
    );
  }

  public static GeneralCommandLine defaultCommandLine(@NotNull String projectPath) throws PantsException {
    final Optional<File> pantsExecutable = findPantsExecutable(new File(projectPath));
    return defaultCommandLine(pantsExecutable.orElseThrow(() -> new PantsException("Couldn't find pants executable for: " + projectPath)));
  }

  @NotNull
  public static GeneralCommandLine defaultCommandLine(@NotNull File pantsExecutable) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    final String pantsExecutablePath = StringUtil.notNullize(
      System.getProperty("pants.executable.path"),
      pantsExecutable.getAbsolutePath()
    );
    commandLine.setExePath(pantsExecutablePath);
    final String workingDir = pantsExecutable.getParentFile().getAbsolutePath();
    return commandLine.withWorkDirectory(workingDir);
  }

  public static Collection<String> listAllTargets(@NotNull String projectPath) throws PantsException {
    if (!PantsUtil.isBUILDFilePath(projectPath)) {
      return Lists.newArrayList();
    }
    GeneralCommandLine cmd = PantsUtil.defaultCommandLine(projectPath);
    try (TempFile tempFile = TempFile.create("list", ".out")) {
      cmd.addParameters(
        "list",
        Paths.get(projectPath).getParent().toString() + ':',
        String.format("%s=%s", PantsConstants.PANTS_CLI_OPTION_LIST_OUTPUT_FILE,
                      tempFile.getFile().getPath()
        )
      );
      final ProcessOutput processOutput = PantsUtil.getCmdOutput(cmd, null);
      if (processOutput.checkSuccess(LOG)) {
        // output only exists if "list" task succeeds
        final String output = FileUtil.loadFile(tempFile.getFile());
        return Arrays.asList(output.split("\n"));
      }
      else {
        List<String> errorLogs = Lists.newArrayList(
          String.format(
            "Could not list targets: Pants exited with status %d",
            processOutput.getExitCode()
          ),
          String.format("argv: '%s'", cmd.getCommandLineString()),
          "stdout:",
          processOutput.getStdout(),
          "stderr:",
          processOutput.getStderr()
        );
        final String errorMessage = String.join("\n", errorLogs);
        LOG.warn(errorMessage);
        throw new PantsException(errorMessage);
      }
    }
    catch (IOException | ExecutionException e) {
      final String processCreationFailureMessage =
        String.format(
          "Could not execute command: '%s' due to error: '%s'",
          cmd.getCommandLineString(),
          e.getMessage()
        );
      LOG.warn(processCreationFailureMessage, e);
      throw new PantsException(processCreationFailureMessage);
    }
  }

  public static String removeWhitespace(@NotNull String text) {
    return text.replaceAll("\\s", "");
  }

  public static boolean isGeneratableFile(@NotNull String path) {
    // todo(fkorotkov): make it configurable or get it from patns.
    // maybe mark target as a target that generates sources and
    // we need to refresh the project for any change in the corresponding module
    // https://github.com/pantsbuild/intellij-pants-plugin/issues/13
    return FileUtilRt.extensionEquals(path, PantsConstants.THRIFT_EXT) ||
           FileUtilRt.extensionEquals(path, PantsConstants.ANTLR_EXT) ||
           FileUtilRt.extensionEquals(path, PantsConstants.ANTLR_4_EXT) ||
           FileUtilRt.extensionEquals(path, PantsConstants.PROTOBUF_EXT);
  }

  @NotNull
  @Nls
  public static String getCanonicalModuleName(@NotNull @NonNls String targetName) {
    // Do not use ':' because it is used as a separator in a classpath
    // while running the app. As well as path separators
    return replaceDelimitersInTargetName(targetName, '_');
  }

  @NotNull
  @Nls
  public static String getCanonicalTargetId(@NotNull @NonNls String targetName) {
    return replaceDelimitersInTargetName(targetName, '.');
  }

  private static String replaceDelimitersInTargetName(@NotNull @NonNls String targetName, char delimeter) {
    return targetName.replace(':', delimeter).replace('/', delimeter).replace('\\', delimeter);
  }

  @NotNull
  public static List<PantsTargetAddress> getTargetAddressesFromModule(@Nullable Module module) {
    if (module == null || !isPantsModule(module)) {
      return Collections.emptyList();
    }
    final String targets = module.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
    if (targets == null) {
      return Collections.emptyList();
    }
    return ContainerUtil.mapNotNull(
      hydrateTargetAddresses(targets),
      PantsTargetAddress::fromString
    );
  }

  @NotNull
  public static List<String> getNonGenTargetAddresses(@Nullable Module module) {
    if (module == null) {
      return Collections.emptyList();
    }
    if (!isSourceModule(module)) {
      return Collections.emptyList();
    }
    return getNonGenTargetAddresses(getTargetAddressesFromModule(module));
  }

  public static boolean isSourceModule(@NotNull Module module) {
    // A source module must either contain content root(s),
    // or depending on other module(s). Otherwise it can be a gen module
    // or 3rdparty module placeholder.
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    return moduleRootManager.getDependencies().length > 0 ||
           moduleRootManager.getContentRoots().length > 0 ||
           moduleRootManager.getContentEntries().length > 0;
  }

  @NotNull
  public static List<String> getNonGenTargetAddresses(@NotNull List<PantsTargetAddress> targets) {
    return targets
      .stream()
      .map(PantsTargetAddress::toString)
      .filter(s -> !PantsUtil.isGenTarget(s))
      .collect(Collectors.toList());
  }

  public static boolean isPantsProject(@NotNull Project project) {
    return ExternalProjectUtil.isExternalProject(project, PantsConstants.SYSTEM_ID);
  }

  public static boolean isFastpassProject(@NotNull Project project) {
    return PantsUtil.findPantsExecutable(project).isPresent() && PantsUtil.isBspProject(project);
  }

  /**
   * Determine whether a project is trigger by Pants `idea-plugin` goal by
   * looking at the "pants_idea_plugin_version" property.
   */
  public static boolean isSeedPantsProject(@NotNull Project project) {
    class SeedPantsProjectKeys {
      private static final String PANTS_IDEA_PLUGIN_VERSION = "pants_idea_plugin_version";
    }

    if (isPantsProject(project)) {
      return false;
    }
    String version = PropertiesComponent.getInstance(project).getValue(SeedPantsProjectKeys.PANTS_IDEA_PLUGIN_VERSION);
    if (version == null) {
      return false;
    }
    if (versionCompare(version, PANTS_IDEA_PLUGIN_VERESION_MIN) < 0 ||
        versionCompare(version, PANTS_IDEA_PLUGIN_VERESION_MAX) > 0
    ) {
      Messages.showInfoMessage(project, PantsBundle.message("pants.idea.plugin.goal.version.unsupported"), "Version Error");
      return false;
    }
    return true;
  }

  public static boolean isPantsModule(@NotNull Module module) {
    return ExternalProjectUtil.isExternalModule(module, PantsConstants.SYSTEM_ID);
  }

  @NotNull
  public static PantsSourceType getSourceTypeForTargetType(@Nullable String targetType, Boolean isSynthetic) {
    try {
      if (isSynthetic && targetType != null) {
        return PantsSourceType.SOURCE_GENERATED;
      }
      return targetType == null ? PantsSourceType.SOURCE :
             PantsSourceType.valueOf(StringUtil.toUpperCase(targetType));
    }
    catch (IllegalArgumentException e) {
      LOG.warn("Got invalid source type " + targetType, e);
      return PantsSourceType.SOURCE;
    }
  }

  public static boolean isResource(PantsSourceType sourceType) {
    return sourceType == PantsSourceType.RESOURCE || sourceType == PantsSourceType.TEST_RESOURCE;
  }

  public static Optional<String> findModuleAddress(@Nullable Module module) {
    if (module == null) return Optional.empty();

    ExternalSystemModulePropertyManager externalSystemModulePropertyManager = ExternalSystemModulePropertyManager.getInstance(module);
    String path = externalSystemModulePropertyManager.getLinkedProjectPath();
    if (path == null) {
      return Optional.empty();
    }
    return getPathFromAddress(module, path);
  }

  public static Optional<VirtualFile> findBUILDFileForModule(@NotNull Module module) {

    final Optional<VirtualFile> virtualFile =
      findModuleAddress(module)
        .map(VfsUtil::pathToUrl)
        .flatMap(s -> Optional.ofNullable(VirtualFileManager.getInstance().findFileByUrl(s)));

    return virtualFile.flatMap(file -> isBUILDFile(file) ? Optional.of(file) : findBUILDFile(virtualFile.orElse(null)));
  }

  public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> map, Function<V1, V2> fun) {
    final Map<K, V2> result = new HashMap<>(map.size());
    for (K key : map.keySet()) {
      final V1 originalValue = map.get(key);
      final V2 newValue = fun.fun(originalValue);
      if (newValue != null) {
        result.put(key, newValue);
      }
    }
    return result;
  }

  public static <K, V> Map<K, V> filterByValue(Map<K, V> map, Condition<V> condition) {
    final Map<K, V> result = new HashMap<>(map.size());
    for (Map.Entry<K, V> entry : map.entrySet()) {
      final K key = entry.getKey();
      final V value = entry.getValue();
      if (condition.value(value)) {
        result.put(key, value);
      }
    }
    return result;
  }

  public static Optional<String> getRelativeProjectPath(@NotNull String projectFile) {
    final Optional<File> buildRoot = findBuildRoot(new File(projectFile));
    return buildRoot.flatMap(file -> getRelativeProjectPath(file, projectFile));
  }

  public static Optional<String> getRelativeProjectPath(@NotNull File workDirectory, @NotNull String projectPath) {
    final File projectFile = new File(projectPath);
    return getRelativeProjectPath(workDirectory, projectFile);
  }

  public static Optional<String> getRelativeProjectPath(@NotNull File workDirectory, @NotNull File projectFile) {
    return Optional
      .ofNullable(FileUtil.getRelativePath(workDirectory, projectFile.isDirectory() ? projectFile : projectFile.getParentFile()));
  }

  public static void refreshAllProjects(@NotNull Project project) {
    if (isPantsProject(project) || isSeedPantsProject(project)) {
      ExternalProjectUtil.refresh(project, PantsConstants.SYSTEM_ID);
    }
    else if (isBspProject(project)) {
      ExternalProjectUtil.refresh(project, ProjectSystemId.findById("BSP"));
    }
  }

  public static boolean isBspModule(Module module) {
    return ExternalSystemApiUtil.isExternalSystemAwareModule(PantsConstants.BSP_SYSTEM_ID, module);
  }


  public static boolean isBspProject(Project project) {
    return Arrays.stream(ModuleManager.getInstance(project).getModules()).anyMatch(PantsUtil::isBspModule);
  }

  public static Optional<VirtualFile> findFileByAbsoluteOrRelativePath(
    @NotNull String fileOrDirPath,
    @NotNull Project project
  ) {
    final VirtualFile absoluteVirtualFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(fileOrDirPath));
    if (absoluteVirtualFile != null) {
      return Optional.of(absoluteVirtualFile);
    }
    return findFileRelativeToBuildRoot(project, fileOrDirPath);
  }

  public static Optional<VirtualFile> findFileRelativeToBuildRoot(@NotNull Project project, @NotNull String fileOrDirPath) {
    final Optional<VirtualFile> buildRoot = findBuildRoot(project);
    return findFileRelativeToDirectory(fileOrDirPath, buildRoot);
  }

  public static Optional<VirtualFile> findFileRelativeToBuildRoot(@NotNull PsiFile psiFile, @NotNull String fileOrDirPath) {
    final Optional<VirtualFile> buildRoot = findBuildRoot(psiFile);
    return findFileRelativeToDirectory(fileOrDirPath, buildRoot);
  }

  private static Optional<VirtualFile> findFileRelativeToDirectory(@NotNull @Nls String fileOrDirPath, Optional<VirtualFile> directory) {
    return directory.flatMap(file -> Optional.ofNullable(file.findFileByRelativePath(fileOrDirPath)));
  }

  /**
   * {@code processor} should return false if we don't want to step into the directory.
   */
  public static void traverseDirectoriesRecursively(@NotNull File root, @NotNull Processor<File> processor) {
    final LinkedList<File> queue = new LinkedList<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      final File file = queue.removeFirst();
      if (file.isFile()) {
        continue;
      }
      if (!processor.process(file)) {
        continue;
      }

      final File[] children = file.listFiles();
      if (children != null) {
        ContainerUtil.addAll(queue, children);
      }
    }
  }

  public static Optional<String> getPathFromAddress(@NotNull Module module, @NotNull String key) {
    final String address = module.getOptionValue(key);
    return PantsTargetAddress.extractPath(address);
  }

  public static void copyDirContent(@NotNull File fromDir, @NotNull File toDir) throws IOException {
    final File[] children = ObjectUtils.notNull(fromDir.listFiles(), ArrayUtil.EMPTY_FILE_ARRAY);
    for (File child : children) {
      final File target = new File(toDir, child.getName());
      if (child.isFile()) {
        FileUtil.copy(child, target);
      }
      else {
        FileUtil.copyDir(child, target, false);
      }
    }
  }

  public static ProcessOutput getCmdOutput(
    @NotNull GeneralCommandLine command,
    @Nullable ProcessAdapter processAdapter
  ) throws ExecutionException {
    final CapturingProcessHandler processHandler =
      new CapturingProcessHandler(command.createProcess(), Charset.defaultCharset(), command.getCommandLineString());
    if (processAdapter != null) {
      processHandler.addProcessListener(processAdapter);
    }
    return processHandler.runProcess();
  }

  public static ProcessOutput getCmdOutput(
    @NotNull Process process,
    @NotNull String commandLineString,
    @Nullable ProcessAdapter processAdapter
  ) {
    final CapturingProcessHandler processHandler = new CapturingProcessHandler(process, Charset.defaultCharset(), commandLineString);
    if (processAdapter != null) {
      processHandler.addProcessListener(processAdapter);
    }
    return processHandler.runProcess();
  }

  public static boolean isPythonAvailable() {
    for (String pluginId : PYTHON_PLUGIN_IDS) {
      final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(pluginId));
      if (plugin != null && plugin.isEnabled()) {
        return true;
      }
    }

    return false;
  }

  @Contract("null -> false")
  public static boolean isExecutable(@Nullable String filePath) {
    if (filePath == null) {
      return false;
    }
    final File file = new File(filePath);
    return file.exists() && file.isFile() && file.canExecute();
  }

  @NotNull
  public static String resolveSymlinks(@NotNull String path) {
    try {
      return new File(path).getCanonicalPath();
    }
    catch (IOException e) {
      throw new ExternalSystemException("Can't resolve symbolic links for " + path, e);
    }
  }

  @NotNull
  public static String fileNameWithoutExtension(@NotNull String name) {
    int index = name.lastIndexOf('.');
    if (index < 0) return name;
    return name.substring(0, index);
  }

  @NotNull
  public static <T> List<T> findChildren(@NotNull DataNode<?> dataNode, @NotNull Key<T> key) {
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

  /**
   * @param project JpsProject
   * @return Path to IDEA Project JDK if exists, else null
   */
  @Nullable
  public static String getJdkPathFromExternalBuilder(@NotNull JpsProject project) {
    JpsSdkReference<?> sdkReference = project.getSdkReferencesTable().getSdkReference(JpsJavaSdkType.INSTANCE);
    if (sdkReference != null) {
      String sdkName = sdkReference.getSdkName();
      JpsLibrary lib = project.getModel().getGlobal().getLibraryCollection().findLibrary(sdkName);
      if (lib != null && lib.getProperties() instanceof JpsSdkImpl) {
        return ((JpsSdkImpl<?>) lib.getProperties()).getHomePath();
      }
    }
    return null;
  }

  /**
   * @return Path to IDEA Project JDK if exists, else null
   */
  @Nullable
  public static String getJdkPathFromIntelliJCore() {
    // Followed example in com.twitter.intellij.pants.testFramework.PantsIntegrationTestCase.setUpInWriteAction()
    final Sdk sdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    String javaHome = null;
    if (sdk.getHomeDirectory() != null) {
      javaHome = sdk.getHomeDirectory().getParent().getPath();
    }
    return javaHome;
  }

  /**
   * @param jdkPath path to IDEA Project JDK
   * @return --jvm-distributions-paths with the parameter if jdkPath is not null,
   * otherwise the flag with empty parameter so user can tell there is issue finding the IDEA project JDK.
   */
  @NotNull
  public static String getJvmDistributionPathParameter(@Nullable final String jdkPath) throws Exception {
    if (jdkPath != null) {
      HashMap<String, List<String>> distributionFlag = new HashMap<>();
      distributionFlag.put(System.getProperty("os.name").toLowerCase(), Collections.singletonList(jdkPath));
      return PantsConstants.PANTS_CLI_OPTION_JVM_DISTRIBUTIONS_PATHS + "=" + new Gson().toJson(distributionFlag);
    }
    else {
      throw new Exception("No IDEA Project JDK Found");
    }
  }

  @NotNull
  public static Set<String> hydrateTargetAddresses(@NotNull String addresses) {
    // There may be serialization behavior change on {@link com.intellij.openapi.module.Module.setOption}
    // ["abc/efg"] will get serialized correctly by gson.toJson to "[\"abc/123\"]".
    // However when data is read back from {@link com.intellij.openapi.module.Module.getOptionValue},
    // it becomes "[&amp;quot;abc/123&amp;quot;]", then gson.fromJson would fail on it.
    String tmp = addresses
      .replace("&amp;", "&")
      .replace("&quot;", "\"");
    return gson.fromJson(tmp, TYPE_SET_STRING);
  }

  @NotNull
  public static String dehydrateTargetAddresses(@NotNull Set<String> addresses) {
    return gson.toJson(addresses);
  }

  public static boolean isGenTarget(@NotNull String address) {
    return StringUtil.startsWithIgnoreCase(address, ".pants.d") ||
           StringUtil.startsWithIgnoreCase(address, PantsConstants.PANTS_PROJECT_MODULE_ID_PREFIX) ||
           // Checking "_synthetic_resources" is a temporary fix. It also needs to match the postfix added from pants in
           // src.python.pants.backend.python.targets.python_target.PythonTarget#_synthetic_resources_target
           // TODO: The long term solution is collect non-synthetic targets at pre-compile stage
           // https://github.com/pantsbuild/intellij-pants-plugin/issues/83
           address.toLowerCase().endsWith("_synthetic_resources");
  }

  public static Set<String> filterGenTargets(@NotNull Collection<String> addresses) {
    return addresses.stream().filter(s -> !isGenTarget(s)).collect(Collectors.toSet());
  }

  /**
   * Copied from: http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
   * Compares two version strings.
   * <p/>
   * Use this instead of String.compareTo() for a non-lexicographical
   * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
   *
   * @param str1 a string of ordinal numbers separated by decimal points.
   * @param str2 a string of ordinal numbers separated by decimal points.
   * @return The result is a negative integer if str1 is _numerically_ less than str2.
   * The result is a positive integer if str1 is _numerically_ greater than str2.
   * The result is zero if the strings are _numerically_ equal.
   * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
   */
  public static Integer versionCompare(String str1, String str2) {
    String[] vals1 = str1.split("\\.");
    String[] vals2 = str2.split("\\.");
    int i = 0;
    // set index to first non-equal ordinal or length of shortest version string
    while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
      i++;
    }
    // compare first non-equal ordinal number
    if (i < vals1.length && i < vals2.length) {
      int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
      return Integer.signum(diff);
    }
    // the strings are equal or one string is a substring of the other
    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
    else {
      return Integer.signum(vals1.length - vals2.length);
    }
  }

  /**
   * Reliable way to find pants executable by a project once it is imported.
   * Use project's module in project to find the `buildRoot`,
   * then use `buildRoot` to find pantsExecutable.
   */
  public static Optional<VirtualFile> findPantsExecutable(@NotNull Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    if (modules.length == 0) {
      return Optional.empty();
    }
    for (Module module : modules) {
      Optional<VirtualFile> buildRoot = findBuildRoot(module);
      if (buildRoot.isPresent()) {
        return findPantsExecutable(buildRoot.get());
      }
    }
    return Optional.empty();
  }

  private static VirtualFile findFileByPath(@NotNull String projectPath) {
    VirtualFile projectFile = LocalFileSystem.getInstance().findFileByPath(projectPath);
    if(projectFile != null) {
      return projectFile;
    } else {
      return LocalFileSystem.getInstance().refreshAndFindFileByPath(projectPath);
    }
  }

  public static Optional<VirtualFile> findPantsExecutable(@NotNull String projectPath) {
    final VirtualFile buildFile = findFileByPath(projectPath);
    return findPantsExecutable(buildFile);
  }

  public static Optional<File> findPantsExecutable(@NotNull File file) {
    Optional<VirtualFile> vf = findPantsExecutable(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file));
    return vf.map(virtualFile -> new File(virtualFile.getPath()));
  }

  private static Optional<VirtualFile> findPantsExecutable(@Nullable VirtualFile file) {
    if (file == null) return Optional.empty();
    if (file.exists() && file.isDirectory()) {
      final VirtualFile pantsFile = file.findChild(PantsConstants.PANTS);
      if (pantsFile != null && !pantsFile.isDirectory()) {
        return Optional.of(pantsFile);
      }
    }
    return findPantsExecutable(file.getParent());
  }

  public static List<String> convertToTargetSpecs(String importPath, List<String> targetNames) {
    File importPathFile = new File(importPath);
    final String projectDir =
      isBUILDFileName(importPathFile.getName()) ? importPathFile.getParent() : importPathFile.getPath();
    final Optional<String> relativeProjectDir = getRelativeProjectPath(projectDir);
    // If relativeProjectDir is null, that means the projectDir is already relative.
    String relativePath = relativeProjectDir.orElse(projectDir);
    if (targetNames.isEmpty()) {
      return Collections.singletonList(relativePath + "::");
    }
    else {
      return targetNames.stream().map(targetName -> relativePath + ":" + targetName).collect(Collectors.toList());
    }
  }

  public static void synchronizeFiles() {
    /**
     * Run in SYNC in unit test mode, and {@link com.twitter.intellij.pants.testFramework.PantsIntegrationTestCase.doImport}
     * is required to be wrapped in WriteAction. Otherwise it will run in async mode.
     */
    if (ApplicationManager.getApplication().isUnitTestMode() && ApplicationManager.getApplication().isWriteAccessAllowed()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        FileDocumentManager.getInstance().saveAllDocuments();
        SaveAndSyncHandler.getInstance().refreshOpenFiles();
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(false); /** synchronous */
      });
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> {
        FileDocumentManager.getInstance().saveAllDocuments();
        SaveAndSyncHandler.getInstance().refreshOpenFiles();
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true); /** asynchronous */
      });
    }
  }

  public static void invalidatePluginCaches() {
    PantsOptions.clearCache();
    SimpleExportResult.clearCache();
  }

  public static List<String> parseCmdParameters(@Nullable String cmdArgsLine) {
    return Optional.ofNullable(cmdArgsLine).map(ParametersListUtil::parse).orElse(new ArrayList<>());
  }

  public static void invokeLaterIfNeeded(Runnable task) {
    // calling directly ensures that the underlying exception gets propagated and
    // can then be asserted in tests - otherwise it gets swallowed underneath
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      task.run();
    }
    else {
      UIUtil.invokeLaterIfNeeded(task);
    }
  }
}
