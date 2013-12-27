package com.twitter.intellij.pants.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsFacetType extends FacetType<PantsFacet, PantsFacetConfiguration> {

    @NonNls
    private static final String ID = "Pants";

    public static PantsFacetType getInstance() {
        return findInstance(PantsFacetType.class);
    }

    public PantsFacetType() {
        super(PantsFacet.ID, ID, "Pants Support");
    }

    @Override
    public PantsFacetConfiguration createDefaultConfiguration() {
        return new PantsFacetConfiguration();
    }

    @Override
    public PantsFacet createFacet(@NotNull Module module, String name, @NotNull PantsFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new PantsFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return true;
    }

    public static class PantsFrameworkDetector extends FacetBasedFrameworkDetector<PantsFacet, PantsFacetConfiguration> {
        public PantsFrameworkDetector() {
            super("pants");
        }

        @Override
        public FacetType<PantsFacet, PantsFacetConfiguration> getFacetType() {
            return PantsFacetType.getInstance();
        }

        @NotNull
        @Override
        public FileType getFileType() {
            return PythonFileType.INSTANCE;
        }

        @NotNull
        @Override
        public ElementPattern<FileContent> createSuitableFilePattern() {
            return FileContentPattern.fileContent().withName("BUILD");
        }
    }
}
