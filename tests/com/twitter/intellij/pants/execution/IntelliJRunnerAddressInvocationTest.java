// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfigurationFactory;

import java.util.HashSet;
import java.util.Set;

public class IntelliJRunnerAddressInvocationTest extends OSSPantsIntegrationTest {


  private Module createModuleWithSerializedAddresses(String path, Set addresses) {
    Module module = ModuleManager.getInstance(myProject).newModule(path, ModuleTypeId.JAVA_MODULE);
    // Make it a Pants module
    module.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, PantsConstants.SYSTEM_ID.getId());
    module.setOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY, PantsUtil.dehydrateTargetAddresses(addresses)
    );
    return module;
  }


  public void testAddressInvoked() throws Throwable {

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        HashSet<String> address_1 = Sets.newHashSet("x:x");
        HashSet<String> address_2 = Sets.newHashSet("y:y");
        HashSet<String> address_3 = Sets.newHashSet("z:z");
        Module module1 = createModuleWithSerializedAddresses("a", address_1);
        Module module2 = createModuleWithSerializedAddresses("b", address_2);
        Module module3 = createModuleWithSerializedAddresses("c", address_3);

        // Make module1 depend on module2 and module3
        ModifiableRootModel model = ModuleRootManager.getInstance(module1).getModifiableModel();
        model.addModuleOrderEntry(module2);
        model.addModuleOrderEntry(module3);
        model.commit();

        assertTrue(ModuleManager.getInstance(myProject).isModuleDependent(module1, module2));
        assertTrue(ModuleManager.getInstance(myProject).isModuleDependent(module1, module3));


        ScalaTestRunConfiguration configuration =
          new ScalaTestRunConfiguration(
            myProject,
            new ScalaTestRunConfigurationFactory(new ScalaTestConfigurationType()),
            "dummy"
          );
        configuration.setModule(module1);
        PantsMakeBeforeRun run = new PantsMakeBeforeRun(myProject);
        Set<String> addressesToCompile = run.getTargetAddressesToCompile(configuration);

        assertEquals(address_1, addressesToCompile);
      }
    });
  }
}
