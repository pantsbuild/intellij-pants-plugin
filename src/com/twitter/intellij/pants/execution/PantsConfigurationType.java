/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Class RemoteConfigurationFactory
 * @author Jeka
 */
package com.twitter.intellij.pants.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsBundle;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PantsConfigurationType implements ConfigurationType {
  private final ConfigurationFactory myFactory;

  /**
   * reflection
   */
  public PantsConfigurationType() {
    myFactory = new ConfigurationFactory(this) {
      public RunConfiguration createTemplateConfiguration(Project project) {
        return new PantsConfiguration(project, this, PantsBundle.message("pants.name"));
      }
    };
  }

  public String getDisplayName() {
    return PantsBundle.message("pants.debug.configuration.display.name");
  }

  public String getConfigurationTypeDescription() {
    return PantsBundle.message("pants.debug.configuration.description");
  }

  public Icon getIcon() {
    return PantsIcons.Icon;
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{getFactory()};
  }

  @NotNull
  public ConfigurationFactory getFactory() {
    return myFactory;
  }

  @NotNull
  public String getId() {
    return PantsBundle.message("pants.name");
  }

  @Nullable
  public static PantsConfigurationType getInstance() {
    return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), PantsConfigurationType.class);
  }
}
