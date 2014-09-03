package com.twitter.intellij.pants.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import org.jetbrains.annotations.NotNull;

public class Utils {
  private static final Logger LOG = Logger.getInstance(Utils.class.getName());

  private enum SOURCE_TYPE {
    SOURCE(ExternalSystemSourceType.SOURCE),
    TEST(ExternalSystemSourceType.TEST),
    RESOURCE(ExternalSystemSourceType.RESOURCE),
    TEST_RESOURCE(ExternalSystemSourceType.TEST_RESOURCE);

    private ExternalSystemSourceType myExternalType;

    private SOURCE_TYPE(@NotNull ExternalSystemSourceType externalType) {
      myExternalType = externalType;
    }

    @NotNull
    public ExternalSystemSourceType getValue() {
      return myExternalType;
    }

    @Override
    public String toString(){
      return myExternalType.toString();
    }
   }

  public static ExternalSystemSourceType getSourceTypeForTargetType(@NotNull String target_type) {
    try {
      return SOURCE_TYPE.valueOf(target_type.toUpperCase()).getValue();
    } catch (java.lang.IllegalArgumentException e) {
      LOG.warn("Got invalid source type " + target_type, e);
      return ExternalSystemSourceType.SOURCE;
    }
  }
}
