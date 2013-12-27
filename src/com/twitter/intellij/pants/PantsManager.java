package com.twitter.intellij.pants;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsManager {

    @NotNull
    public static PantsManager getInstance(@NotNull Project project) {
        return new PantsManager(project);
    }

    private final Project myProject;

    private PantsManager(@NotNull Project project) {
        myProject = project;
    }

    public boolean isPantsAvailable() {
        return getPantsExecutable() != null;
    }

    @Nullable
    public VirtualFile getPantsExecutable() {
        return myProject.getBaseDir().findChild("pants");
    }

    @Nullable
    public VirtualFile getSupportDir() {
        //todo: make a project setting
        return myProject.getBaseDir().findFileByRelativePath("src/python/twitter/pants");
    }
}
