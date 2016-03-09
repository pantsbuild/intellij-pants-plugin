// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.serialization;

import com.google.gson.Gson;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import com.twitter.intellij.pants.jps.incremental.model.impl.JpsPantsModuleExtensionImpl;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PantsJpsModelSerializerExtension extends JpsModelSerializerExtension {
  // same as in ExternalSystemConstants but it is used in an external process so we can't use it directly
  @NonNls @NotNull public static final String EXTERNAL_SYSTEM_ID_KEY = "external.system.id";
  @NonNls @NotNull public static final String LINKED_PROJECT_PATH_KEY = "external.linked.project.path";
  public static final Gson gson = new Gson();
  @Nullable
  public static JpsPantsModuleExtension findPantsModuleExtension(@Nullable JpsModule module) {
    return module != null ? module.getContainer().getChild(JpsPantsModuleExtension.ROLE) : null;
  }

  @NotNull
  @Override
  public List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.singletonList(new PantsJpsProjectExtensionSerializer());
  }

  @Override
  public void loadModuleOptions(@NotNull JpsModule module, @NotNull Element rootElement) {
    super.loadModuleOptions(module, rootElement);
    final Element facetManager = ContainerUtil.find(
      rootElement.getChildren("component"),
      new Condition<Element>() {
        @Override
        public boolean value(Element element) {
          return StringUtil.equalsIgnoreCase(element.getAttributeValue("name"), "FacetManager");
        }
      }
    );
    final List<Element> facets = facetManager != null ? facetManager.getChildren() : Collections.<Element>emptyList();
    if (!facets.isEmpty()) {
      // facet will handle compilation/transpilation or maybe it's Python module and we don't need to do anything
      return;
    }

    final String externalSystemId = rootElement.getAttributeValue(EXTERNAL_SYSTEM_ID_KEY);
    final String linkedProjectPath = rootElement.getAttributeValue(LINKED_PROJECT_PATH_KEY);
    final String targetAddressesValue = StringUtil.nullize(rootElement.getAttributeValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY));
    final String addressInfosJson = StringUtil.nullize(rootElement.getAttributeValue(PantsConstants.PANTS_TARGET_ADDRESS_INFOS_KEY));
    Set<TargetAddressInfo> targetInfoSet = gson.fromJson(addressInfosJson, HashSet.class);
    if (PantsConstants.PANTS.equals(externalSystemId) && targetAddressesValue != null && linkedProjectPath != null) {
      final Set<String> targetAddresses = new HashSet<String>(StringUtil.split(targetAddressesValue, ","));
      final JpsPantsModuleExtensionImpl moduleExtensionElement =
        new JpsPantsModuleExtensionImpl(linkedProjectPath, targetAddresses, targetInfoSet);
      module.getContainer().setChild(JpsPantsModuleExtension.ROLE, moduleExtensionElement);
    }
  }
}
