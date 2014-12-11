// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsLibNotFoundInspection extends LocalInspectionTool {
  @NotNull
  public String getGroupDisplayName() {
    return PantsBundle.message("inspections.group.name");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return PantsBundle.message("pants.inspection.library.found");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public String getShortName() {
    return "PantsLibNotFound";
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!PantsUtil.isBUILDFileName(file.getName())) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    final Project project = file.getProject();
    final LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
    final Library libraryByName = libraryTable.getLibraryByName(PantsConstants.PANTS_LIBRARY_NAME);
    if (libraryByName != null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
      file,
      PantsBundle.message("pants.inspection.library.not.found"),
      isOnTheFly,
      new LocalQuickFix[]{INSTALL_FIX},
      ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    );
    return new ProblemDescriptor[]{problemDescriptor};
  }

  private static LocalQuickFix INSTALL_FIX = new InstallQuickFix();

  public static class InstallQuickFix implements LocalQuickFix {
    @NotNull
    @Override
    public String getName() {
      return PantsBundle.message("pants.inspection.fix.it");
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      applyFix(project);
    }

    public static void applyFix(@NotNull Project project) {
      final String pantsVersion = PantsUtil.findPantsVersion(project);
      if (pantsVersion == null) {
        Messages.showErrorDialog(
          project,
          PantsBundle.message("pants.inspection.library.no.version"),
          PantsBundle.message("pants.error.title")
        );
        return;
      }

      final VirtualFile folderWithPex = PantsUtil.findFolderWithPex();
      if (folderWithPex == null) {
        Messages.showErrorDialog(
          project,
          PantsBundle.message("pants.inspection.library.no.pex.folder"),
          PantsBundle.message("pants.error.title")
        );
        return;
      }

      final VirtualFile pexFile = PantsUtil.findPexVersionFile(folderWithPex, pantsVersion);
      if (pexFile == null) {
        Messages.showErrorDialog(
          project,
          PantsBundle.message("pants.inspection.library.no.pex.file", pantsVersion),
          PantsBundle.message("pants.error.title")
        );
        return;
      }
      configureByFile(project, pexFile);
    }

    public static void configureByFile(@NotNull Project project, @NotNull VirtualFile pexFile) {
      final VirtualFile jar = JarFileSystem.getInstance().refreshAndFindFileByPath(pexFile.getPath() + "!/");
      assert jar != null;

      final LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
      final Library library = libraryTable.createLibrary(PantsConstants.PANTS_LIBRARY_NAME);
      final Library.ModifiableModel modifiableModel = library.getModifiableModel();
      modifiableModel.addRoot(jar, OrderRootType.CLASSES);
      modifiableModel.commit();
    }
  }
}
