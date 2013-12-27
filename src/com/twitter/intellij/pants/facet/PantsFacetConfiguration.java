package com.twitter.intellij.pants.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.twitter.intellij.pants.facet.ui.PantsFacetEditorTab;
import org.jdom.Element;

public class PantsFacetConfiguration extends PantsFacetSettings implements FacetConfiguration {
    private static final String EXECUTABLE = "pantsExecutablePath";
    private static final String SUPPORT = "pantsSupportPath";

    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[]{
                new PantsFacetEditorTab(editorContext)
        };
    }

    public void readExternal(Element element) throws InvalidDataException {
        setExecutablePath(element.getAttributeValue(EXECUTABLE));
        setSupportFolderPath(element.getAttributeValue(SUPPORT));
    }

    public void writeExternal(Element element) throws WriteExternalException {
        if (getExecutablePath() != null) {
            element.setAttribute(EXECUTABLE, getExecutablePath());
        }
        if (getSupportFolderPath() != null) {
            element.setAttribute(SUPPORT, getSupportFolderPath());
        }
    }
}
