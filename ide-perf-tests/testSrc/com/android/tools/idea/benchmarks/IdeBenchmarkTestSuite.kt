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
package com.android.tools.idea.benchmarks

import com.android.testutils.JarTestSuiteRunner
import com.android.tools.tests.IdeaTestSuiteBase
import org.junit.runner.RunWith

@RunWith(JarTestSuiteRunner::class)
@JarTestSuiteRunner.ExcludeClasses(IdeBenchmarkTestSuite::class)
class IdeBenchmarkTestSuite : IdeaTestSuiteBase() {
  companion object {
    init {
      try {
        symlinkToIdeaHome(
          "tools/adt/idea/android/testData",
          "tools/base/templates",
          "tools/idea/java")

        setUpOfflineRepo("tools/base/build-system/studio_repo.zip", "out/studio/repo")
        setUpOfflineRepo("tools/adt/idea/android/test_deps.zip", "prebuilts/tools/common/m2/repository")
      }
      catch (e: Throwable) {
        System.err.println("ERROR: Failed to initialize test suite, tests will likely fail following this error")
        e.printStackTrace()
      }
    }
  }
}
