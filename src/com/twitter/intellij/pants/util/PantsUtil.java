package com.twitter.intellij.pants.util;

import com.intellij.ide.actions.OpenProjectFileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class PantsUtil {
  public static final String PANTS = "pants";
  public static final String PANTS_INTERNAL = "pants_internal";
  public static final String TWITTER = "twitter";
  public static final String BUILD = "BUILD";

  public static final String PANTS_LIBRARY_NAME = "pants";
  public static final String PANTS_INI = "pants.ini";
  public static final String PANTS_PEX = "pants.pex";

  private static final String PANTS_VERSION_REGEXP = "pants_version: (.+)";
  private static final String PEX_RELATIVE_PATH = ".pants.d/bin/pants.pex";

  public static final FileChooserDescriptor BUILD_FILE_CHOOSER_DESCRIPTOR = new OpenProjectFileChooserDescriptor(true) {
    @Override
    public boolean isFileSelectable(VirtualFile file) {
      return BUILD.equals(file.getName());
    }

    @Override
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
      if (!super.isFileVisible(file, showHiddenFiles)) {
        return false;
      }
      return file.isDirectory() || BUILD.equals(file.getName());
    }
  };

  public static final FileChooserDescriptor PANTS_FILE_CHOOSER_DESCRIPTOR =
    new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        return PANTS.equals(file.getName());
      }

      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!super.isFileVisible(file, showHiddenFiles)) {
          return false;
        }
        return file.isDirectory() || PANTS.equals(file.getName());
      }
    };

  @Nullable
  public static String findPantsVersion(@NotNull Project project) {
    final VirtualFile pantsIniFile = findPantsIniFile(project);
    return pantsIniFile == null ? null : findVersionInFile(pantsIniFile);
  }

  @Nullable
  private static String findVersionInFile(VirtualFile file) {
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
  public static VirtualFile findPantsIniFile(@NotNull Project project) {
    return findFileInContentRoots(project, PANTS_INI);
  }

  @Nullable
  public static VirtualFile findLocalPantsPex(@NotNull Project project) {
    return findFileInContentRoots(project, PANTS_PEX);
  }

  @Nullable
  public static VirtualFile findFileInContentRoots(@NotNull Project project, @NotNull @NonNls String fileName) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
        VirtualFile iniFile = root.findChild(fileName);
        if (iniFile != null) {
          return iniFile;
        }
      }
    }
    return null;
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

  @NotNull
  public static String findRelativePathToPantsExecutable(@NotNull String projectPath) {
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    final VirtualFile pantsExecutable = findPantsExecutable(buildFile);
    if (pantsExecutable == null) {
      return projectPath;
    }
    else {
      return StringUtil.notNullize(
        StringUtil.substringAfter(projectPath, pantsExecutable.getParent().getPath()),
        projectPath
      );
    }
  }

  @Nullable
  public static VirtualFile findPantsExecutable(@NotNull String projectPath) {
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    return findPantsExecutable(buildFile);
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
    String result = iterator.next();
    while (iterator.hasNext()) {
      result = StringUtil.commonPrefix(result, iterator.next());
    }
    // /foo/bar/
    // /foo/barBaz
    final int lastSlash = result.lastIndexOf('/');
    return result.substring(0, lastSlash + 1);
  }
}
