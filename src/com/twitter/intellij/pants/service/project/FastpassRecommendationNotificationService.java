// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.ui.PantsToBspProjectAction;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class FastpassRecommendationNotificationService {
  FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();

  final String groupId = "Fastpass recommendation";
  final Logger logger = Logger.getInstance(FastpassRecommendationNotificationService.class);

  static private String readMoreLink() {
    return Optional
      .ofNullable(PropertiesComponent.getInstance().getValue("fastpass.readmore.link"))
      .orElse("https://github.com/scalameta/fastpass/blob/master/readme.md");
  }

  public static FastpassRecommendationNotificationService getInstance() {
    return ServiceManager.getService(FastpassRecommendationNotificationService.class);
  }

  private void showRecommendationNotification(Project project) {
    Notification notification = createRecommendation();
    if(NotificationsConfigurationImpl.getSettings(groupId).getDisplayType() != NotificationDisplayType.NONE) {
      PantsExternalMetricsListenerManager metrics = PantsExternalMetricsListenerManager.getInstance();
      metrics.logEvent("FASTPASS_RECOMMENDATION_SHOWN");
    }
    notification.notify(project);
  }

  @NotNull
  private Notification createRecommendation() {
    String text = PantsBundle.message("fastpass.recommendation.text");
    Notification notification = new Notification(groupId,
                                                 PantsBundle.message("fastpass.recommendation.title"),
                                                 text, NotificationType.INFORMATION
    );
    notification.addAction(new AnAction(PantsBundle.message("fastpass.notification.convert.action")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        PantsExternalMetricsListenerManager.getInstance()
          .logEvent("CONVERTED_TO_FASTPASS_AFTER_RECOMMENDATION");
        new PantsToBspProjectAction().actionPerformed(e);
      }
    });
    notification.addAction(new AnAction(PantsBundle.message("fastpass.notification.read.more")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(readMoreLink());
      }
    });
    return notification;
  }

  public void tick(Project project, Duration buildLength) {
    try {
      if (counter.tick(buildLength, LocalDateTime.now())) {
        showRecommendationNotification(project);
      }
    } catch (Throwable e) {
      logger.error(e);
    }
  }
}
