package com.twitter.intellij.pants.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

public class PantsFacet extends Facet<PantsFacetConfiguration> {
    public static final FacetTypeId<PantsFacet> ID = new FacetTypeId<PantsFacet>("python");

    public PantsFacet(@NotNull FacetType facetType, @NotNull Module module, @NotNull String name, @NotNull PantsFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }
}
