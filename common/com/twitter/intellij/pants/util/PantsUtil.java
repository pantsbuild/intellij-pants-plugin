// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.google.gson.Gson;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import org.jetbrains.annotations.*;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.impl.sdk.JpsSdkImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class PantsUtil {
  private static final Logger LOG = Logger.getInstance(PantsUtil.class);

  private static final List<String> PYTHON_PLUGIN_IDS = ContainerUtil.immutableList("PythonCore", "Pythonid");

  @Nullable
  public static VirtualFile findBUILDFile(@Nullable VirtualFile vFile) {
    if (vFile == null) {
      return null;
    } else if (vFile.isDirectory()) {
      return ContainerUtil.find(
        vFile.getChildren(),
        new Condition<VirtualFile>() {
          @Override
          public boolean value(VirtualFile file) {
            return isBUILDFileName(file.getName());
          }
        }
      );
    }
    return isBUILDFileName(vFile.getName()) ? vFile : null;
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
   *
   * @param file - a BUILD file or a directory
   */
  public static boolean isPantsProjectFile(VirtualFile file) {
    if (file.isDirectory()) {
      return findPantsExecutable(file) != null;
    }
    return isBUILDFileName(file.getName());
  }

  private static final String PANTS_VERSION_REGEXP = "pants_version: (.+)";
  private static final String PEX_RELATIVE_PATH = ".pants.d/bin/pants.pex";

  @Nullable
  public static String findPantsVersion(@Nullable VirtualFile workingDir) {
    final VirtualFile pantsIniFile = findPantsIniFile(workingDir);
    return pantsIniFile == null ? null : findVersionInFile(pantsIniFile);
  }

  @Nullable
  public static VirtualFile findPantsIniFile(@Nullable VirtualFile workingDir) {
    return workingDir != null ? workingDir.findChild(PantsConstants.PANTS_INI) : null;
  }

  @Nullable
  private static String findVersionInFile(@NotNull VirtualFile file) {
    try {
      final String fileContent = VfsUtilCore.loadText(file);
      final List<String> matches = StringUtil.findMatches(
        fileContent, Pattern.compile(PANTS_VERSION_REGEXP)
      );
      return matches.isEmpty() ? null : matches.iterator().next();
    }
    catch (IOException e) {
      return null;
    }
  }

  @Nullable
  public static VirtualFile findFolderWithPex() {
    return findFolderWithPex(VfsUtil.getUserHomeDir());
  }

  @Nullable
  public static VirtualFile findFolderWithPex(@Nullable VirtualFile userHomeDir) {
    return findFileRelativeToDirectory(PEX_RELATIVE_PATH, userHomeDir);
  }

  @Nullable
  public static VirtualFile findPexVersionFile(@NotNull VirtualFile folderWithPex, @NotNull String pantsVersion) {
    final String filePrefix = "pants-" + pantsVersion;
    return ContainerUtil.find(
      folderWithPex.getChildren(), new Condition<VirtualFile>() {
        @Override
        public boolean value(VirtualFile virtualFile) {
          return "pex".equalsIgnoreCase(virtualFile.getExtension()) && virtualFile.getName().startsWith(filePrefix);
        }
      }
    );
  }

  @Nullable
  public static VirtualFile findPantsExecutable(@NotNull String projectPath) {
    // guard against VirtualFileManager throwing an NPE in tests that don't stand up an IDEA instance
    if (ApplicationManager.getApplication() == null) {
      return null;
    }
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    return findPantsExecutable(buildFile);
  }

  @Nullable
  public static File findPantsWorkingDir(@NotNull File file) {
    final File pantsExecutable = findPantsExecutable(file);
    return pantsExecutable != null ? pantsExecutable.getParentFile() : null;
  }

  @Nullable
  public static VirtualFile findPantsWorkingDir(@NotNull String filePath) {
    final VirtualFile pantsExecutable = findPantsExecutable(filePath);
    return pantsExecutable != null ? pantsExecutable.getParent() : null;
  }

  @Nullable
  public static VirtualFile findPantsWorkingDir(@NotNull Project project) {
    return findPantsWorkingDir(project.getProjectFile());
  }

  @Nullable
  public static VirtualFile findPantsWorkingDir(@NotNull PsiFile psiFile) {
    final VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
    return virtualFile != null ? findPantsWorkingDir(virtualFile) : findPantsWorkingDir(psiFile.getProject());
  }

  @Nullable
  public static VirtualFile findPantsWorkingDir(@NotNull Module module) {
    final VirtualFile moduleFile = module.getModuleFile();
    if (moduleFile != null) {
      return findPantsWorkingDir(moduleFile);
    }
    final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (VirtualFile contentRoot : rootManager.getContentRoots()) {
      final VirtualFile pantsWorkingDir = findPantsWorkingDir(contentRoot);
      if (pantsWorkingDir != null) {
        return pantsWorkingDir;
      }
    }
    return null;
  }

  @Nullable
  public static VirtualFile findPantsWorkingDir(@Nullable VirtualFile file) {
    final VirtualFile pantsExecutable = findPantsExecutable(file);
    return pantsExecutable != null ? pantsExecutable.getParent() : null;
  }

  @Nullable
  public static VirtualFile findPantsExecutable(@Nullable VirtualFile file) {
    if (file == null) return null;
    if (file.isDirectory()) {
      final VirtualFile pantsFile = file.findChild(PantsConstants.PANTS);
      if (pantsFile != null && !pantsFile.isDirectory()) {
        return pantsFile;
      }
    }
    return findPantsExecutable(file.getParent());
  }

  @Nullable
  public static File findPantsExecutable(@Nullable File file) {
    if (file == null) return null;
    if (file.isDirectory()) {
      final File pantsFile = new File(file, PantsConstants.PANTS);
      if (pantsFile.exists() && !pantsFile.isDirectory()) {
        return pantsFile;
      }
    }
    return findPantsExecutable(file.getParentFile());
  }

  public static GeneralCommandLine defaultCommandLine(@NotNull String projectPath) throws PantsException {
    final File pantsExecutable = PantsUtil.findPantsExecutable(new File(projectPath));
    if (pantsExecutable == null) {
      throw new PantsException("Couldn't find pants executable for: " + projectPath);
    }
    return defaultCommandLine(pantsExecutable);
  }

  @NotNull
  public static GeneralCommandLine defaultCommandLine(@NotNull File pantsExecutable) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    boolean runFromSources = Boolean.valueOf(System.getProperty("pants.dev.run"));
    if (runFromSources) {
      commandLine.getEnvironment().put("PANTS_DEV", "1");
    }

    final String pantsExecutablePath = StringUtil.notNullize(
      System.getProperty("pants.executable.path"),
      pantsExecutable.getAbsolutePath()
    );
    commandLine.setExePath(pantsExecutablePath);
    final String workingDir = pantsExecutable.getParentFile().getAbsolutePath();
    return commandLine.withWorkDirectory(workingDir);
  }

  public static Collection<String> listAllTargets(@NotNull String projectPath) throws PantsException {
    try {
      final String fileContent = removeWhitespace(FileUtil.loadFile(new File(projectPath)));
      final Set<String> result = new TreeSet<String>();
      result.addAll(StringUtil.findMatches(fileContent, Pattern.compile("\\Wname=(['\"])([\\w-_]+)\\1"), 2));
      return result;
    }
    catch (IOException e) {
      throw new PantsException(e.getMessage());
    }
  }

  public static String removeWhitespace(@NotNull String text) {
    return text.replaceAll("\\s","");
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

  @NotNull @Nls
  public static String getCanonicalModuleName(@NotNull @NonNls String targetName) {
    // Do not use ':' because it is used as a separator in a classpath
    // while running the app. As well as path separators
    return replaceDelimitersInTargetName(targetName, '_');
  }

  @NotNull @Nls
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
    return ContainerUtil.mapNotNull(
      StringUtil.split(StringUtil.notNullize(targets), ","),
      new Function<String, PantsTargetAddress>() {
        @Override
        public PantsTargetAddress fun(String targetAddress) {
          return PantsTargetAddress.fromString(targetAddress);
        }
      }
    );
  }

  public static boolean isPantsProject(@NotNull Project project) {
    return ContainerUtil.exists(
      ModuleManager.getInstance(project).getModules(),
      new Condition<Module>() {
        @Override
        public boolean value(Module module) {
          return isPantsModule(module);
        }
      }
    );
  }

  public static boolean isPantsModule(@NotNull Module module) {
    final String systemId = module.getOptionValue(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
    return StringUtil.equals(systemId, PantsConstants.SYSTEM_ID.getId());
  }

  @NotNull
  public static PantsSourceType getSourceTypeForTargetType(@Nullable String targetType) {
    try {
      return targetType == null ? PantsSourceType.SOURCE :
             PantsSourceType.valueOf(StringUtil.toUpperCase(targetType));
    }
    catch (IllegalArgumentException e) {
      LOG.warn("Got invalid source type " + targetType, e);
      return PantsSourceType.SOURCE;
    }
  }

  public static boolean isResource(PantsSourceType sourceType) {
    return sourceType == PantsSourceType.RESOURCE  || sourceType == PantsSourceType.TEST_RESOURCE;
  }

  @Nullable
  public static Module findModuleForBUILDFile(@NotNull Project project, @Nullable final VirtualFile file) {
    if (file == null || !isBUILDFileName(file.getName())) return null;
    final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(project.getProjectFile());
    if (workingDir == null) {
      return null;
    }
    return ContainerUtil.find(
      ModuleManager.getInstance(project).getModules(),
      new Condition<Module>() {
        @Override
        public boolean value(Module module) {
          final VirtualFile moduleBUILDFile = findBUILDFileForModule(module);
          return file.equals(moduleBUILDFile);
        }
      }
    );
  }

  @Nullable
  public static VirtualFile findBUILDFileForModule(@NotNull Module module) {
    final String linkedPantsBUILD = getPathFromAddress(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
    final String linkedPantsBUILDUrl = linkedPantsBUILD != null ? VfsUtil.pathToUrl(linkedPantsBUILD) : null;
    final VirtualFile virtualFile = linkedPantsBUILDUrl != null ? VirtualFileManager.getInstance().findFileByUrl(linkedPantsBUILDUrl) : null;
    if (virtualFile == null) {
      return null;
    }
    return isBUILDFile(virtualFile) ? virtualFile : findBUILDFile(virtualFile);
  }

  public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> map, Function<V1, V2> fun) {
    final Map<K, V2> result = new HashMap<K, V2>(map.size());
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
    final Map<K, V> result = new HashMap<K, V>(map.size());
    for (Map.Entry<K, V> entry : map.entrySet()) {
      final K key = entry.getKey();
      final V value = entry.getValue();
      if (condition.value(value)) {
        result.put(key, value);
      }
    }
    return result;
  }

  @Nullable
  public static String getRelativeProjectPath(@NotNull File projectFile) {
    final File workingDir = findPantsWorkingDir(projectFile);
    return workingDir == null ? null : getRelativeProjectPath(workingDir, projectFile);
  }

  @Nullable
  public static String getRelativeProjectPath(@NotNull File workDirectory, @NotNull String projectPath) {
    final File projectFile = new File(projectPath);
    return getRelativeProjectPath(workDirectory, projectFile);
  }

  @Nullable
  public static String getRelativeProjectPath(@NotNull File workDirectory, @NotNull File projectFile) {
    return FileUtil.getRelativePath(workDirectory, projectFile.isDirectory() ? projectFile : projectFile.getParentFile());
  }

  public static void refreshAllProjects(@NotNull Project project) {
    if (!PantsUtil.isPantsProject(project)) {
      return;
    }
    final ImportSpecBuilder specBuilder = new ImportSpecBuilder(project, PantsConstants.SYSTEM_ID);
    ProgressExecutionMode executionMode = ApplicationManager.getApplication().isUnitTestMode() ?
                                          ProgressExecutionMode.MODAL_SYNC : ProgressExecutionMode.IN_BACKGROUND_ASYNC;
    specBuilder.use(executionMode);
    ExternalSystemUtil.refreshProjects(specBuilder);
  }

  @Nullable
  public static VirtualFile findFileByAbsoluteOrRelativePath(
    @NotNull String fileOrDirPath,
    @NotNull Project project
  ) {
    final VirtualFile absoluteVirtualFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(fileOrDirPath));
    if (absoluteVirtualFile != null) {
      return absoluteVirtualFile;
    }
    return findFileRelativeToPantsWorkingDir(project, fileOrDirPath);
  }

  @Nullable
  public static VirtualFile findFileRelativeToPantsWorkingDir(@NotNull Project project, @NotNull String fileOrDirPath) {
    final VirtualFile workingDir = findPantsWorkingDir(project);
    return findFileRelativeToDirectory(fileOrDirPath, workingDir);
  }

  @Nullable
  public static VirtualFile findFileRelativeToPantsWorkingDir(@NotNull PsiFile psiFile, @NotNull String fileOrDirPath) {
    final VirtualFile workingDir = findPantsWorkingDir(psiFile);
    return findFileRelativeToDirectory(fileOrDirPath, workingDir);
  }

  @Nullable
  private static VirtualFile findFileRelativeToDirectory(@NotNull @Nls String fileOrDirPath, @Nullable VirtualFile directory) {
    return directory != null ? directory.findFileByRelativePath(fileOrDirPath) : null;
  }

  /**
   * {@code processor} should return false if we don't want to step into the directory.
   */
  public static void traverseDirectoriesRecursively(@NotNull File root, @NotNull Processor<File> processor) {
    final LinkedList<File> queue = new LinkedList<File>();
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

  @Contract(value = "_, null -> null", pure = true)
  public static String getPathFromAddress(@NotNull Module module, @Nullable String key) {
    final String address = key != null ? module.getOptionValue(key) : null;
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
    return getOutput(command.createProcess(), processAdapter);
  }

  public static ProcessOutput getOutput(@NotNull Process process, @Nullable ProcessAdapter processAdapter) {
    final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
    if (processAdapter != null) {
      processHandler.addProcessListener(processAdapter);
    }
    return processHandler.runProcess();
  }

  public static boolean isPythonAvailable() {
    for (String pluginId : PYTHON_PLUGIN_IDS) {
      final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(pluginId));
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

  @Contract(pure=true)
  public static <T> boolean forall(@NotNull Iterable<T> iterable, @NotNull Condition<T> condition) {
    for (T value : iterable) {
      if(!condition.value(value)) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  public static  <T> List<T> findChildren(@NotNull DataNode<?> dataNode, @NotNull Key<T> key) {
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

  public static ProcessOutput getProcessOutput(
    @NotNull GeneralCommandLine command,
    @Nullable ProcessAdapter processAdapter
  ) throws ExecutionException {
    return getOutput(command.createProcess(), processAdapter);
  }

  public static String getPantsOptions(final String pantsExecutable) {
    final GeneralCommandLine exportCommandline = defaultCommandLine(pantsExecutable);
    exportCommandline.addParameters("options", "--no-colors");
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(exportCommandline, null);
      final String stdOut = processOutput.getStdout();
      return stdOut;
    }
    catch (ExecutionException e) {
      throw new PantsException("Failed:" + exportCommandline.getCommandLineString());
    }
  }


  /**
   * @param project JpsProject
   * @return Path to IDEA Project JDK if exists, else null
   */
  @Nullable
  public static String getJdkPathFromExternalBuilder(@NotNull JpsProject project) {
    JpsSdkReference sdkReference = project.getSdkReferencesTable().getSdkReference(JpsJavaSdkType.INSTANCE);
    if (sdkReference != null) {
      String sdkName = sdkReference.getSdkName();
      JpsLibrary lib = project.getModel().getGlobal().getLibraryCollection().findLibrary(sdkName);
      if (lib != null && lib.getProperties() instanceof JpsSdkImpl) {
        return ((JpsSdkImpl)lib.getProperties()).getHomePath();
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
      HashMap<String, List<String>> distributionFlag = new HashMap<String, List<String>>();
      distributionFlag.put(System.getProperty("os.name").toLowerCase(), Arrays.asList(jdkPath));
      return PantsConstants.PANTS_JVM_DISTRIBUTIONS_PATHS_OPTION + "=" + new Gson().toJson(distributionFlag);
    }
    else {
      throw new Exception("No IDEA Project JDK Found");
    }
  }

  class SimpleExportResult {
    public String version;
    public String getVersion() {return version; }
  }

  public static boolean hasTargetIdInExport(final String pantsExecutable) {
    final GeneralCommandLine commandline = defaultCommandLine(pantsExecutable);
    commandline.addParameters("export", "--no-colors");
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(commandline, null);
      final String stdOut = processOutput.getStdout();
      SimpleExportResult simpleExportResult = new Gson().fromJson(stdOut, SimpleExportResult.class);
      return versionCompare(simpleExportResult.getVersion(), "1.0.5") >= 0;
    }
    catch (ExecutionException e) {
      throw new PantsException("Failed:" + commandline.getCommandLineString());
    }
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
}

