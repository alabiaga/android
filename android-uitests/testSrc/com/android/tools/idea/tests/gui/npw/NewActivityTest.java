/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.npw;

import com.android.tools.idea.navigator.AndroidProjectViewPane;
import com.android.tools.idea.npw.platform.Language;
import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture;
import com.android.tools.idea.tests.gui.framework.fixture.npw.ConfigureBasicActivityStepFixture;
import com.android.tools.idea.tests.gui.framework.fixture.npw.ConfigureBasicActivityStepFixture.ActivityTextField;
import com.android.tools.idea.tests.gui.framework.fixture.npw.NewActivityWizardFixture;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import org.fest.swing.timing.Wait;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.intellij.openapi.util.text.StringUtil.getOccurrenceCount;
import static org.junit.Assert.assertEquals;

@RunWith(GuiTestRemoteRunner.class)
public class NewActivityTest {
  private static final String PROVIDED_ACTIVITY = "app/src/main/java/google/simpleapplication/MyActivity.java";
  private static final String PROVIDED_MANIFEST = "app/src/main/AndroidManifest.xml";
  private static final String DEFAULT_ACTIVITY_NAME = "MainActivity";
  private static final String DEFAULT_LAYOUT_NAME = "activity_main";


  @Rule public final GuiTestRule guiTest = new GuiTestRule();

  private EditorFixture myEditor;
  private NewActivityWizardFixture myDialog;
  private ConfigureBasicActivityStepFixture<NewActivityWizardFixture> myConfigActivity;

  @Before
  public void setUp() throws IOException {
    guiTest.importSimpleApplication();
    guiTest.ideFrame().getProjectView().selectProjectPane();
    myEditor = guiTest.ideFrame().getEditor();
    myEditor.open(PROVIDED_ACTIVITY);

    guiTest.ideFrame().getProjectView().assertFilesExist(
      "settings.gradle",
      "app",
      PROVIDED_ACTIVITY,
      PROVIDED_MANIFEST
    );

    invokeNewActivityMenu();
    assertTextFieldValues(DEFAULT_ACTIVITY_NAME, DEFAULT_LAYOUT_NAME);
    assertThat(getSavedKotlinSupport()).isFalse();
    assertThat(getSavedRenderSourceLanguage()).isEqualTo(Language.JAVA);
  }

  @Test
  public void createLauncherActivity() {
    myConfigActivity.selectLauncherActivity();
    myDialog.clickFinish();

    String text = guiTest.getProjectFileText(PROVIDED_MANIFEST);
    assertEquals(2, getOccurrenceCount(text, "android.intent.category.LAUNCHER"));
  }

  @Test
  public void createDefaultActivity() {
    myDialog.clickFinish();

    guiTest.ideFrame().waitForGradleProjectSyncToFinish(Wait.seconds(15));

    guiTest.ideFrame().getProjectView().assertFilesExist(
      "app/src/main/java/google/simpleapplication/MainActivity.java",
      "app/src/main/java/google/simpleapplication/FirstFragment.java",
      "app/src/main/java/google/simpleapplication/SecondFragment.java",
      "app/src/main/res/layout/activity_main.xml",
      "app/src/main/res/layout/fragment_first.xml",
      "app/src/main/res/layout/fragment_second.xml"
    );
  }

  @Test
  public void createActivityWithNonDefaultPackage() {
    myConfigActivity.enterTextFieldValue(ActivityTextField.PACKAGE_NAME, "google.test2");
    myDialog.clickFinish();

    String text = guiTest.getProjectFileText("app/src/main/java/google/test2/MainActivity.java");
    assertThat(text).startsWith("package google.test2;");
  }

  @Test
  public void createActivityWithKotlin() {
    myConfigActivity.setSourceLanguage("Kotlin");
    assertThat(getSavedRenderSourceLanguage()).isEqualTo(Language.KOTLIN);
    assertThat(getSavedKotlinSupport()).isFalse(); // Changing the Render source language should not affect the project default

    myDialog.clickFinish();
    guiTest.ideFrame().waitForGradleProjectSyncToFinish();

    myEditor
      .open("app/build.gradle")
      .moveBetween("apply plugin: 'kotlin-android'", "")
      .moveBetween("implementation \"org.jetbrains.kotlin:kotlin-stdlib:$kotlin_", "version")
      .enterText("my_")
      .open("build.gradle")
      .moveBetween("kotlin_", "version")
      .enterText("my_")
      .moveBetween("classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_", "version")
      .enterText("my_")
      .open("app/src/main/java/google/simpleapplication/MainActivity.kt")
      .moveBetween("override fun onCreate", "");

    // Add second Kotlin Activity and check it shouldn't add dependencies again (renamed $kotlin_version -> $kotlin_my_version)
    invokeNewActivityMenu();
    myConfigActivity.setSourceLanguage("Kotlin");
    myDialog.clickFinish();

    assertThat(guiTest.getProjectFileText("build.gradle")).doesNotContain("$kotlin_version");
    assertThat(guiTest.getProjectFileText("app/build.gradle")).doesNotContain("$kotlin_version");
  }

  @Test
  public void changeActivityName() {
    // Changing "Activity Name" causes "Title" and "Layout Name" to change
    myConfigActivity.enterTextFieldValue(ActivityTextField.NAME, "MainActivityTest");
    assertTextFieldValues("MainActivityTest", "activity_main_test");

    myDialog.clickFinish();

    guiTest.ideFrame().waitForGradleProjectSyncToFinish();

    guiTest.ideFrame().getProjectView().assertFilesExist(
      "app/src/main/java/google/simpleapplication/MainActivityTest.java",
      "app/src/main/res/layout/activity_main_test.xml"
    );
  }

  @Test
  public void changeLayoutName() {
    // Changing "Layout Name" causes "Activity Name" and "Title" to change
    myConfigActivity.enterTextFieldValue(ActivityTextField.LAYOUT, "activity_main_test1");
    assertTextFieldValues("MainTest1Activity", "activity_main_test1");

    myDialog.clickCancel();
  }

  @Test
  public void changeActivityThenLayoutName() {
    // Changing "Activity Name" causes "Title" and "Layout Name" to change, after that
    // "Activity Name" should be "locked", changing LAYOUT should not update any other field
    myConfigActivity.enterTextFieldValue(ActivityTextField.NAME, "MainActivityTest1");
    myConfigActivity.enterTextFieldValue(ActivityTextField.LAYOUT, "main_activity2");
    assertTextFieldValues("MainActivityTest1", "main_activity2");

    myDialog.clickCancel();
  }

  @Test
  public void changeActivityThenTitleName() {
    // Changing "Activity Name", then "Title", then "Activity Name" again. "Title" should not update since it's been manually modified.
    myConfigActivity.enterTextFieldValue(ActivityTextField.NAME, "MainActivityTest1");
    myConfigActivity.enterTextFieldValue(ActivityTextField.NAME, "MainActivityTest123");
    assertTextFieldValues("MainActivityTest123", "activity_main_test123");

    myDialog.clickCancel();
  }

  @Test
  public void projectViewPaneNotChanged() {
    // Verify that after creating a new activity, the current pane on projectView does not change, assumes initial pane is ProjectView
    myDialog.clickFinish();

    guiTest.ideFrame().waitForGradleProjectSyncToFinish();

    myEditor = guiTest.ideFrame().getEditor();
    myEditor.open(PROVIDED_ACTIVITY);

    assertEquals(ProjectViewPane.ID, guiTest.ideFrame().getProjectView().getCurrentViewId());

    // Verify that Android stays on Android
    verifyNewActivityProjectPane(true, true);

    // Now when new activity is cancelled
    verifyNewActivityProjectPane(false, false);
    verifyNewActivityProjectPane(true, false);
  }

  // Note: This should be called only when the last open file was a Java/Kotlin file
  private void invokeNewActivityMenu() {
    guiTest.ideFrame().invokeMenuPath("File", "New", "Activity", "Basic Activity");
    myDialog = NewActivityWizardFixture.find(guiTest.ideFrame());

    myConfigActivity = myDialog.getConfigureActivityStep();
  }

  private void assertTextFieldValues(@NotNull String activityName, @NotNull String layoutName) {
    assertThat(myConfigActivity.getTextFieldValue(ActivityTextField.NAME)).isEqualTo(activityName);
    assertThat(myConfigActivity.getTextFieldValue(ActivityTextField.LAYOUT)).isEqualTo(layoutName);
  }

  private void verifyNewActivityProjectPane(boolean startWithAndroidPane, boolean finish) {
    // Change to viewId
    if (startWithAndroidPane) {
      guiTest.ideFrame().getProjectView().selectAndroidPane();
    }
    else {
      guiTest.ideFrame().getProjectView().selectProjectPane();
    }
    myEditor = guiTest.ideFrame().getEditor();
    myEditor.open(PROVIDED_ACTIVITY);

    // Create a new activity
    guiTest.ideFrame().invokeMenuPath("File", "New", "Activity", "Basic Activity");
    myDialog = NewActivityWizardFixture.find(guiTest.ideFrame());
    myConfigActivity = myDialog.getConfigureActivityStep();
    if (finish) {
      myDialog.clickFinish();
      guiTest.ideFrame().waitForGradleProjectSyncToFinish();
      myEditor = guiTest.ideFrame().getEditor();
      myEditor.open(PROVIDED_ACTIVITY);

    }
    else {
      myDialog.clickCancel();
    }

    // Make sure it is still the same
    String viewId = startWithAndroidPane ? AndroidProjectViewPane.ID : ProjectViewPane.ID;
    assertEquals(viewId, guiTest.ideFrame().getProjectView().getCurrentViewId());
  }

  private static boolean getSavedKotlinSupport() {
    return PropertiesComponent.getInstance().isTrueValue("SAVED_PROJECT_KOTLIN_SUPPORT");
  }

  @NotNull
  private static Language getSavedRenderSourceLanguage() {
      return Language.fromName(PropertiesComponent.getInstance().getValue("SAVED_RENDER_LANGUAGE"), Language.JAVA);
  }
}
