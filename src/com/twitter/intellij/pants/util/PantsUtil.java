package com.twitter.intellij.pants.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class PantsUtil {
    public static final String PANTS = "pants";
    public static final String TWITTER = "twitter";
    public static final String BUILD = "BUILD";

    public static final String PANTS_LIBRAY_NAME = "pants";
    public static final String PANTS_INI = "pants.ini";

    private static final String PANTS_VERSION_REGEXP = "pants_version: (\\d+(\\.\\d+)*)";
    private static final String PEX_RELATIVE_PATH = ".pants.d/bin/pants.pex";

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
                    fileContent, Pattern.compile(PANTS_VERSION_REGEXP), 1
            );
            return matches.isEmpty() ? null : matches.iterator().next();
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    public static VirtualFile findPantsIniFile(@NotNull Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
                VirtualFile iniFile = root.findChild(PANTS_INI);
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
        return ContainerUtil.find(folderWithPex.getChildren(), new Condition<VirtualFile>() {
            @Override
            public boolean value(VirtualFile virtualFile) {
                return "pex".equalsIgnoreCase(virtualFile.getExtension()) && virtualFile.getName().startsWith(filePrefix);
            }
        });
    }
}
