package com.twitter.intellij.pants.facet.ui;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.facet.PantsFacetConfiguration;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PantsFacetEditorTab extends FacetEditorTab {

    private final FacetEditorContext myEditorContext;
    private JPanel myMainPanel;
    private TextFieldWithBrowseButton myExecutableField;
    private TextFieldWithBrowseButton mySupportField;

    public PantsFacetEditorTab(FacetEditorContext editorContext) {
        myEditorContext = editorContext;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Pants";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return myMainPanel;
    }

    @Override
    public boolean isModified() {
        return !StringUtil.equals(getFacetConfiguration().getExecutablePath(), getExecutablePath()) ||
                !StringUtil.equals(getFacetConfiguration().getSupportFolderPath(), getSupportPath());
    }

    private String getExecutablePath() {
        return StringUtil.nullize(FileUtil.toSystemIndependentName(myExecutableField.getText()));
    }

    private String getSupportPath() {
        return StringUtil.nullize(FileUtil.toSystemIndependentName(mySupportField.getText()));
    }

    @Override
    public void reset() {
        myExecutableField.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(getFacetConfiguration().getExecutablePath())));
        mySupportField.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(getFacetConfiguration().getSupportFolderPath())));
    }

    @Override
    public void apply() throws ConfigurationException {
        getFacetConfiguration().setExecutablePath(getExecutablePath());
        getFacetConfiguration().setSupportFolderPath(getSupportPath());
    }

    private PantsFacetConfiguration getFacetConfiguration() {
        return ((PantsFacetConfiguration) myEditorContext.getFacet().getConfiguration());
    }

    @Override
    public void disposeUIResources() {

    }
}
