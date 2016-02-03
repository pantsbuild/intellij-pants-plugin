// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.common.collect.Iterators;
import com.google.gson.*;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class TargetInfoDeserializer implements JsonDeserializer<TargetInfo> {
  public static TargetInfoDeserializer INSTANCE = new TargetInfoDeserializer();

  @Override
  public TargetInfo deserialize(JsonElement element, Type type, final JsonDeserializationContext context) throws JsonParseException {
    final JsonObject object = element.getAsJsonObject();
    final List<String> targets = getStringListField(object, "targets");
    final List<String> libraries = getStringListField(object, "libraries");
    final List<String> excludes = getStringListField(object, "excludes");
    final List<SourceRoot> sourceRoots = getFieldAsList(
      object.getAsJsonArray("roots"),
      new Function<JsonElement, SourceRoot>() {
        @Override
        public SourceRoot fun(JsonElement element) {
          return context.deserialize(element, SourceRoot.class);
        }
      }
    );
    final TargetAddressInfo addressInfo = context.deserialize(element, TargetAddressInfo.class);
    return new TargetInfo(
      new HashSet<TargetAddressInfo>(Collections.singleton(addressInfo)),
      new HashSet<String>(targets),
      new HashSet<String>(libraries),
      new HashSet<String>(excludes),
      new HashSet<SourceRoot>(sourceRoots)
    );
  }

  @NotNull
  private List<String> getStringListField(JsonObject object, String memberName) {
    if (!object.has(memberName)) {
      return Collections.emptyList();
    }
    return getFieldAsList(
      object.getAsJsonArray(memberName),
      new Function<JsonElement, String>() {
        @Override
        public String fun(JsonElement element) {
          return element.getAsString();
        }
      }
    );
  }

  private <T> List<T> getFieldAsList(JsonArray jsonArray, Function<JsonElement, T> fun) {
    return ContainerUtil.mapNotNull(
      Iterators.toArray(jsonArray.iterator(), JsonElement.class),
      fun
    );
  }
}
