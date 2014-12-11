// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
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
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class PantsUtil {
  private static final Logger LOG = Logger.getInstance(PantsUtil.class);

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
    return PantsConstants.BUILD.equals(FileUtil.getNameWithoutExtension(name));
  }

  /**
   * Checks if it's a BUILD file or folder under a Pants project
   *
   * @param file - a BUILD file or a directory
   */
  public static boolean isPantsProjectFolder(VirtualFile file) {
    if (file.isDirectory()) {
      return findPantsExecutable(file) != null;
    }
    return isBUILDFileName(file.getName());
  }

  private static final String PANTS_VERSION_REGEXP = "pants_version: (.+)";
  private static final String PEX_RELATIVE_PATH = ".pants.d/bin/pants.pex";

  @Nullable
  public static String findPantsVersion(@NotNull Project project) {
    final VirtualFile pantsIniFile = findPantsIniFile(project);
    return pantsIniFile == null ? null : findVersionInFile(pantsIniFile);
  }

  @Nullable
  public static VirtualFile findPantsIniFile(@NotNull Project project) {
    final VirtualFile pantsWorkingDir = findPantsWorkingDir(project);
    return pantsWorkingDir != null ? pantsWorkingDir.findChild(PantsConstants.PANTS_INI) : null;
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
    final VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
    return userHomeDir != null ? userHomeDir.findFileByRelativePath(PEX_RELATIVE_PATH) : null;
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
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    return findPantsExecutable(buildFile);
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
  public static File findPantsWorkingDir(@Nullable File file) {
    final File pantsExecutable = findPantsExecutable(file);
    return pantsExecutable != null ? pantsExecutable.getParentFile() : null;
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

  @Nullable
  public static String findCommonRoot(List<String> roots) {
    final Iterator<String> iterator = roots.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    String result = pathWithTrailingSeparator(iterator.next());
    while (iterator.hasNext()) {
      result = StringUtil.commonPrefix(result, pathWithTrailingSeparator(iterator.next()));
      if (!result.endsWith("/")) {
        // means something like
        // foo/bar/
        // foo/barjava/
        result = pathWithTrailingSeparator(VfsUtil.getParentDir(result));
      }
    }
    return result;
  }

  private static String pathWithTrailingSeparator(@Nullable String path) {
    return path == null || path.endsWith("/") ? path : (path + "/");
  }

  public static GeneralCommandLine defaultCommandLine(@NotNull String projectPath) throws PantsException {
    final File buildFile = new File(projectPath);
    if (!buildFile.exists()) {
      throw new PantsException("Couldn't find BUILD file: " + projectPath);
    }
    final File pantsExecutable = PantsUtil.findPantsExecutable(buildFile);
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

  public static List<String> listAllTargets(@NotNull String projectPath) throws PantsException {
    final GeneralCommandLine commandLine = defaultCommandLine(projectPath);
    commandLine.addParameter("goal");
    commandLine.addParameter("list");
    try {
      final File temporaryFile = FileUtil.createTempFile("pants_run", ".out");
      commandLine.addParameter("--list-output-file=" + temporaryFile.getPath());
      final File workDirectory = commandLine.getWorkDirectory();
      final String relativePath = FileUtil.getRelativePath(workDirectory, new File(projectPath).getParentFile());

      if (relativePath == null) {
        throw new PantsException(String.format("Can't find relative path from %s to %s", workDirectory.getPath(), projectPath));
      }

      commandLine.addParameter(relativePath + "::");

      final String commandOutput = ScriptRunnerUtil.getProcessOutput(commandLine);
      if (commandOutput.contains("no such option")) {
        throw new PantsException("Pants doesn't have necessary APIs. Please upgrade you pants!");
      }
      final String processOutput = FileUtil.loadFile(temporaryFile);
      return ContainerUtil.map(
        ContainerUtil.filter(
          StringUtil.splitByLines(processOutput),
          new Condition<String>() {
            @Override
            public boolean value(String s) {
              return s.startsWith(relativePath + ":");
            }
          }
        ),
        new Function<String, String>() {
          @Override
          public String fun(String target) {
            int index = target.lastIndexOf(':');
            return target.substring(index + 1);
          }
        }
      );
    }
    catch (ExecutionException e) {
      throw new PantsException(e);
    }
    catch (IOException e) {
      throw new PantsException(e.getMessage());
    }
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
    return targetName.replace(':', '_').replace('/', '_').replace('\\', '_');
  }

  @Nullable @NonNls
  public static PantsTargetAddress getTargetAddressFromModule(@NotNull @Nls Module module) {
    if (!isPantsModule(module)) {
      return null;
    }
    final String targetAddress = module.getOptionValue(ExternalSystemConstants.LINKED_PROJECT_ID_KEY);
    return targetAddress != null ? PantsTargetAddress.fromString(targetAddress) : null;
  }

  public static boolean isPantsModule(@NotNull @Nls Module module) {
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
    final String linkedPantsBUILD = module.getOptionValue(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
    final String linkedPantsBUILDUrl = linkedPantsBUILD != null ? VfsUtil.pathToUrl(linkedPantsBUILD) : null;
    final VirtualFile virtualFile = linkedPantsBUILDUrl != null ? VirtualFileManager.getInstance().refreshAndFindFileByUrl(linkedPantsBUILDUrl) : null;
    if (virtualFile == null) {
      return null;
    }
    return isBUILDFile(virtualFile) ? virtualFile : findBUILDFile(virtualFile);
  }

  public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> map, Function<V1, V2> fun) {
    final HashMap<K, V2> result = new HashMap<K, V2>(map.size());
    for (K key : map.keySet()) {
      final V1 originalValue = map.get(key);
      result.put(key, fun.fun(originalValue));
    }
    return result;
  }

  public static String getRelativeProjectPath(@NotNull String projectPath, @NotNull File workDirectory){
    final File projectFile = new File(projectPath);
    return FileUtil.getRelativePath(workDirectory, projectFile.isDirectory() ? projectFile : projectFile.getParentFile());
  }

  public static void refreshAllProjects(Project project) {
    final ImportSpecBuilder specBuilder = new ImportSpecBuilder(project, PantsConstants.SYSTEM_ID);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      specBuilder.use(ProgressExecutionMode.MODAL_SYNC);
    }
    ExternalSystemUtil.refreshProjects(specBuilder);
  }

  /**
   * {@code processor} should return false if we don't want to step into the file.
   */
  public static void traverseFilesRecursively(@NotNull File root, @NotNull Processor<File> processor) {
    final LinkedList<File> queue = new LinkedList<File>();
    queue.add(root);
    while (!queue.isEmpty()) {
      final File file = queue.removeFirst();

      if (!processor.process(file)) {
        continue;
      }

      final File[] children = file.listFiles();
      if (children != null) {
        ContainerUtil.addAll(queue, children);
      }
    }
  }
}
