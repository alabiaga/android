/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.npw.module.recipes.benchmarkModule

/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.ide.common.repository.GradleVersion
import com.android.repository.Revision
import com.android.tools.idea.gradle.npw.project.GradleBuildSettings.needsExplicitBuildToolsVersion
import com.android.tools.idea.npw.module.recipes.addKotlinIfNeeded
import com.android.tools.idea.npw.module.recipes.benchmarkModule.src.androidTest.exampleBenchmarkJava
import com.android.tools.idea.npw.module.recipes.benchmarkModule.src.androidTest.exampleBenchmarkKt
import com.android.tools.idea.templates.RepositoryUrlManager
import com.android.tools.idea.templates.resolveDependency
import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.npw.module.recipes.benchmarkModule.src.main.androidManifestXml
import com.android.tools.idea.npw.module.recipes.benchmarkModule.src.androidTest.androidManifestXml as testAndroidManifestXml

fun RecipeExecutor.generateBenchmarkModule(
  moduleData: ModuleTemplateData
) {
  val projectData = moduleData.projectTemplateData
  val testOut = moduleData.testDir
  val packageName = moduleData.packageName
  val moduleOut = moduleData.rootDir
  val buildToolsVersion = projectData.buildToolsVersion
  val (minApi, _, buildApi, targetApi, targetApiString, buildApiString) = moduleData.apis
  val language = projectData.language

  val repoUrlManager = RepositoryUrlManager.get()

  val bgp = resolveDependency(repoUrlManager, "androidx.benchmark:benchmark-gradle-plugin:+", "1.0.0")

  addClasspathDependency(bgp)

  addIncludeToSettings(moduleData.name)
  save(benchmarkProguardRules(), moduleOut.resolve("benchmark-proguard-rules.pro"))

  val bg = buildGradle(
    needsExplicitBuildToolsVersion(GradleVersion.parse(projectData.gradlePluginVersion), Revision.parseRevision(buildToolsVersion)),
    buildApiString ?: buildApi.toString(),
    buildToolsVersion,
    minApi,
    targetApiString ?: targetApi.toString(),
    language,
    projectData.gradlePluginVersion
  )

  save(bg, moduleOut.resolve("build.gradle"))
  addDependency("androidx.test:runner:+", "androidTestImplementation")
  addDependency("androidx.test.ext:junit:+", "androidTestImplementation")
  addDependency("junit:junit:4.12", "androidTestImplementation")
  addDependency(
    resolveDependency(repoUrlManager, "androidx.benchmark:benchmark-junit4:+", "1.0.0"),
    "androidTestImplementation"
  )

  save(androidManifestXml(packageName), moduleOut.resolve("src/main/AndroidManifest.xml"))
  save(testAndroidManifestXml(packageName), moduleOut.resolve("src/androidTest/AndroidManifest.xml"))

  if (language == Language.Kotlin) {
    save(exampleBenchmarkKt(packageName), testOut.resolve("ExampleBenchmark.kt"))
  }
  else {
    save(exampleBenchmarkJava(packageName), testOut.resolve("ExampleBenchmark.java"))
  }

  addKotlinIfNeeded(projectData, noKtx = true)
}
