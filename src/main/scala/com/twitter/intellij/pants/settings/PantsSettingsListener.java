// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.util.messages.Topic;

/**
 * Created by fedorkorotkov
 */
public interface PantsSettingsListener extends ExternalSystemSettingsListener<PantsProjectSettings> {
  Topic<PantsSettingsListener> TOPIC = Topic.create("Pants-specific settings", PantsSettingsListener.class);
}
