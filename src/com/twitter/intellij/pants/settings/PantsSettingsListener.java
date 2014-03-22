package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.util.messages.Topic;

/**
 * Created by fedorkorotkov
 */
public interface PantsSettingsListener extends ExternalSystemSettingsListener<PantsProjectSettings> {
  Topic<PantsSettingsListener> TOPIC = Topic.create("Pants-specific settings", PantsSettingsListener.class);
}
