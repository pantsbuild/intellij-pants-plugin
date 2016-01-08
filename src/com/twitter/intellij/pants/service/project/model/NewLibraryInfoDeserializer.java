// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

public class NewLibraryInfoDeserializer implements JsonDeserializer<NewLibraryInfo> {
  public static NewLibraryInfoDeserializer INSTANCE = new NewLibraryInfoDeserializer();

  @Override
  public NewLibraryInfo deserialize(JsonElement element, Type type, final JsonDeserializationContext context) throws JsonParseException {
    NewLibraryInfo result = new NewLibraryInfo();
    final JsonObject object = element.getAsJsonObject();
    for (Map.Entry<String, JsonElement> node : object.entrySet()) {
      result.put(node.getKey(), String.valueOf(node.getValue()));
    }
    return result;
  }
}
