/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.sync;

import static com.android.tools.idea.gradle.project.sync.ModuleSetupContext.MODULES_BY_GRADLE_PATH_KEY;
import static com.android.tools.idea.gradle.project.sync.Modules.createUniqueModuleId;

import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet;
import com.android.tools.idea.gradle.project.sync.setup.module.ModuleFinder;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link ModuleSetupContext}.
 */
public class ModuleSetupContextTest extends HeavyPlatformTestCase {
  private IdeModifiableModelsProviderImpl myModelsProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myModelsProvider = new IdeModifiableModelsProviderImpl(getProject());
  }

  public void testGetModuleFinder() {
    Module app = createModule("app");
    Module lib = createModule("lib");
    Module javaLib = createModule("javaLib");

    createModuleFacet(app);
    createModuleFacet(lib);
    createModuleFacet(javaLib);

    ModuleSetupContext context = new ModuleSetupContext.Factory().create(app, myModelsProvider);

    Project project = getProject();
    ModuleFinder moduleFinder = context.getModuleFinder();
    assertNotNull(moduleFinder);

    // Verify that modules can be found by Gradle path.
    assertSame(app, moduleFinder.findModuleByGradlePath(":app"));
    assertSame(lib, moduleFinder.findModuleByGradlePath(":lib"));
    assertSame(javaLib, moduleFinder.findModuleByGradlePath(":javaLib"));

    // Verify that modules can be found by module id.
    String projectFolderPath = myProject.getBasePath();
    assertSame(app, moduleFinder.findModuleByModuleId(createUniqueModuleId(projectFolderPath, ":app")));
    assertSame(lib, moduleFinder.findModuleByModuleId(createUniqueModuleId(projectFolderPath, ":lib")));
    assertSame(javaLib, moduleFinder.findModuleByModuleId(createUniqueModuleId(projectFolderPath, ":javaLib")));

    assertSame(moduleFinder, project.getUserData(MODULES_BY_GRADLE_PATH_KEY));

    ModuleSetupContext.removeSyncContextDataFrom(project);
    assertNull(project.getUserData(MODULES_BY_GRADLE_PATH_KEY));
  }

  private void createModuleFacet(@NotNull Module module) {
    FacetManager facetManager = FacetManager.getInstance(module);
    GradleFacet facet = facetManager.createFacet(GradleFacet.getFacetType(), module.getName(), null);
    facet.getConfiguration().GRADLE_PROJECT_PATH = ":" + module.getName();

    ModifiableFacetModel facetModel = myModelsProvider.getModifiableFacetModel(module);
    facetModel.addFacet(facet);
  }
}