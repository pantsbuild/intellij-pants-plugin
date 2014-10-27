package com.twitter.intellij.pants.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
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
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PantsUtil {
  private static final Logger LOG = Logger.getInstance(PantsUtil.class);

  public static final String PANTS = "pants";
  public static final String PANTS_LIBRARY_NAME = "pants";

  public static final String PANTS_INI = "pants.ini";

  private static final String BUILD = "BUILD";
  private static final String THRIFT_EXT = "thrift";
  private static final String ANTLR_EXT = "g";
  private static final String ANTLR_4_EXT = "g4";
  private static final String PROTOBUF_EXT = "proto";

  public static boolean isBUILDFilePath(@NotNull String path) {
    return isBUILDFileName(PathUtil.getFileName(path));
  }

  public static boolean isBUILDFileName(@NotNull String name) {
    return BUILD.equals(FileUtil.getNameWithoutExtension(name));
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
    return pantsWorkingDir != null ? pantsWorkingDir.findChild(PANTS_INI) : null;
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
  public static VirtualFile findPantsWorkingDir(@Nullable VirtualFile file) {
    final VirtualFile pantsExecutable = findPantsExecutable(file);
    return pantsExecutable != null ? pantsExecutable.getParent() : null;
  }

  @Nullable
  public static VirtualFile findPantsExecutable(@Nullable VirtualFile file) {
    if (file == null) return null;
    if (file.isDirectory()) {
      final VirtualFile pantsFile = file.findChild(PantsUtil.PANTS);
      if (pantsFile != null && !pantsFile.isDirectory()) {
        return pantsFile;
      }
    }
    return findPantsExecutable(file.getParent());
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
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (buildFile == null) {
      throw new PantsException("Couldn't find BUILD file: " + projectPath);
    }
    final VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(buildFile);
    if (pantsExecutable == null) {
      throw new PantsException("Couldn't find pants executable for: " + projectPath);
    }
    boolean runFromSources = Boolean.valueOf(System.getProperty("pants.dev.run"));
    if (runFromSources) {
      commandLine.getEnvironment().put("PANTS_DEV", "1");
    }

    final String pantsExecutablePath = StringUtil.notNullize(
      System.getProperty("pants.executable.path"),
      pantsExecutable.getPath()
    );
    commandLine.setExePath(pantsExecutablePath);
    final String workingDir = pantsExecutable.getParent().getPath();
    commandLine.setWorkDirectory(workingDir);
    return commandLine;
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

      String commandOutput = ScriptRunnerUtil.getProcessOutput(commandLine);
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
    return FileUtilRt.extensionEquals(path, THRIFT_EXT) ||
           FileUtilRt.extensionEquals(path, ANTLR_EXT) ||
           FileUtilRt.extensionEquals(path, ANTLR_4_EXT) ||
           FileUtilRt.extensionEquals(path, PROTOBUF_EXT);
  }

  public static String getCanonicalModuleName(@NotNull @NonNls String targetName) {
    // Do not use ':' because it is used as a separator in a classpath
    // while running the app. As well as path separators
    return targetName.replace(':', '_').replace('/', '_').replace('\\', '_');
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
          final String linkedPantsBUILD = module.getOptionValue(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
          final VirtualFile moduleBUILDFile = linkedPantsBUILD != null ? workingDir.findFileByRelativePath(linkedPantsBUILD) : null;
          return file.equals(moduleBUILDFile);
        }
      }
    );
  }

  public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> map, Function<V1, V2> fun) {
    final HashMap<K, V2> result = new HashMap<K, V2>(map.size());
    for (K key : map.keySet()) {
      final V1 originalValue = map.get(key);
      result.put(key, fun.fun(originalValue));
    }
    return result;
  }
}
