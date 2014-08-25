package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.generate.tostring.util.StringUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {
  private JLabel myTargetsLabel;
  private CheckBoxList<String> myTargets;

  public PantsProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    super(settings);
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    myTargetsLabel = new JBLabel(PantsBundle.message("pants.settings.text.targets"));
    myTargets = new CheckBoxList<String>();

    content.add(myTargetsLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    content.add(myTargets, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  @Override
  protected boolean isExtraSettingModified() {
    return myTargets.getSelectedValues().length > 0;
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    myTargets.clear();
    myTargets.setPaintBusy(true);
    ProgressManager.getInstance().run(
      new Task.Backgroundable(null, PantsBundle.message("pants.settings.text.loading.targets")) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            final List<String> targets = StringUtil.isEmpty(getInitialSettings().getExternalProjectPath()) ?
                                         Collections.<String>emptyList() :
                                         PantsUtil.listAllTargets(getInitialSettings().getExternalProjectPath());
            UIUtil.invokeLaterIfNeeded(
              new Runnable() {
                @Override
                public void run() {
                  for (String target : targets) {
                    myTargets.addItem(target, target, false);
                  }
                }
              }
            );
          }
          catch (PantsException e) {
            // todo(fkorotkov): proper handling
          }
          finally {
            myTargets.setPaintBusy(false);
          }
        }
      }
    );
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    final List<String> result = new ArrayList<String>();
    for (int i = 0; i < myTargets.getItemsCount(); i++) {
      String target = myTargets.getItemAt(i);
      if (myTargets.isItemSelected(target)) {
        result.add(target);
      }
    }
    settings.setTargets(result);
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    if (myTargets.getSelectedIndices().length == 0) {
      throw new ConfigurationException(PantsBundle.message("pants.error.no.targets.are.selected"));
    }
    return true;
  }
}
