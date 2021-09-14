// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;

public class LibraryInfoDeserializer implements JsonDeserializer<LibraryInfo> {
  public static LibraryInfoDeserializer INSTANCE = new LibraryInfoDeserializer();

  @Override
  public LibraryInfo deserialize(JsonElement element, Type type, final JsonDeserializationContext context) throws JsonParseException {
    LibraryInfo result = new LibraryInfo();
    final JsonObject object = element.getAsJsonObject();
    for (Map.Entry<String, JsonElement> node : object.entrySet()) {
      result.addJar(node.getKey(), node.getValue().getAsString());
    }
    return result;
  }
}
