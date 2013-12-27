package com.twitter.intellij.pants.facet;

import org.jetbrains.annotations.Nullable;

public class PantsFacetSettings {
    @Nullable
    private String executablePath = null;

    @Nullable
    private String supportFolderPath = null;

    @Nullable
    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(@Nullable String executablePath) {
        this.executablePath = executablePath;
    }

    @Nullable
    public String getSupportFolderPath() {
        return supportFolderPath;
    }

    public void setSupportFolderPath(@Nullable String supportFolderPath) {
        this.supportFolderPath = supportFolderPath;
    }
}
