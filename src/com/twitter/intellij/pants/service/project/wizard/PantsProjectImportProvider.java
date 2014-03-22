package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

public class PantsProjectImportProvider extends AbstractExternalProjectImportProvider {
  public PantsProjectImportProvider(PantsProjectImportBuilder builder) {
    super(builder, PantsConstants.SYSTEM_ID);
  }

  @Override
  protected boolean canImportFromFile(VirtualFile file) {
    return PantsUtil.BUILD.equals(file.getName());
  }

  @Nullable
  @Override
  public String getFileSample() {
    return "<b>Pants</b> build file (BUILD)";
  }
}
