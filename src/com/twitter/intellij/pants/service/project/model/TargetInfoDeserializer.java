// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.common.collect.Iterators;
import com.google.gson.*;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class TargetInfoDeserializer implements JsonDeserializer<TargetInfo> {
  public static TargetInfoDeserializer INSTANCE = new TargetInfoDeserializer();

  @Override
  public TargetInfo deserialize(JsonElement element, Type type, final JsonDeserializationContext context) throws JsonParseException {
    final JsonObject object = element.getAsJsonObject();
    final List<String> targets = getStringListField(object.getAsJsonArray("targets"));
    final List<String> libraries = getStringListField(object.getAsJsonArray("libraries"));
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
      new HashSet<String>(libraries),
      new HashSet<String>(targets),
      new HashSet<SourceRoot>(sourceRoots)
    );
  }

  private List<String> getStringListField(JsonArray jsonArray) {
    return getFieldAsList(
      jsonArray,
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
