package com.twitter.intellij.pants.indexing;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.intellij.indexing.shared.java.maven.MavenPackageCoordinatesInferenceExtension;
import com.intellij.indexing.shared.java.maven.MavenPackageId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PantsMavenPackageCoordinatesInferenceExtension implements MavenPackageCoordinatesInferenceExtension {

    // We are handling maven coursier paths in following format:
    // .cache/pants/coursier/https/[repository]/[repository]/[group]/[artifact]/[version]/artifact-version.jar

    @Nullable
    @Override
    public MavenPackageId infer(@NotNull String path, @NotNull List<String> reversePathElements) {
        Stream<String> elements = reversePathElements.stream();
        int cachePosition = reversePathElements.indexOf("coursier");
        if (cachePosition < 7) return null;
        List<String> groupIdList = reversePathElements.subList(3, cachePosition - 3 );
        Collections.reverse(groupIdList);
        String groupId = String.join(".", groupIdList);
        String versionId = reversePathElements.get(1);
        String artifactId = reversePathElements.get(2);
        return new MavenPackageId(groupId, artifactId, versionId);
    }
}