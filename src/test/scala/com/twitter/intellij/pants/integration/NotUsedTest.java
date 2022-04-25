// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import org.junit.Ignore;

public class NotUsedTest extends TargetFileCompletionIntegrationTest {

        @Ignore
        public void testDependencies() {
                String toComplete = "scala_library(    dependencies = [\"example" + CURSOR + "\"])";
                String[] expected = {
                        "examples",
                        "examples/src/scala/org/pantsbuild/example/hello:hello",
                        "examples/src/scala/org/pantsbuild/example/hello/exe:exe",
                        "examples/src/scala/org/pantsbuild/example/hello/welcome:welcome",
                        "examples/src/java/org/pantsbuild/example/hello/greet:greet",
                        "examples/src/resources/org/pantsbuild/example/hello:hello",
                        "examples/src/resources/org/pantsbuild/example:example",
                        "examples/src/resources/org/pantsbuild/example/jaxb:jaxb",
                        "examples/src/resources/org/pantsbuild/example/names:names",
                        "examples/src/resources/org/pantsbuild/example:hello_directory",
                        "examples/src/resources/org/pantsbuild/example:jaxb_directory",
                        "examples/src/resources/org/pantsbuild/example:names_directory",
                };
                completionTest(toComplete, expected);
        }
}