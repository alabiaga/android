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
package com.android.tools.profilers.network;

import com.android.tools.adtui.Choreographer;
import com.android.tools.profilers.AnimatedTimeline;
import com.android.tools.profilers.StageView;
import com.android.tools.profilers.StudioMonitorStage;
import com.android.tools.profilers.StudioProfilers;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class NetworkProfilerStageView extends StageView {
  private final NetworkProfilerStage myStage;
  private final JPanel myConnectionDetails;
  private final NetworkRequestsView myRequestsView;
  private final Splitter mySplitter;

  public NetworkProfilerStageView(NetworkProfilerStage stage) {
    super(stage);
    myStage = stage;
    myConnectionDetails = new JPanel(new BorderLayout());

    myRequestsView = new NetworkRequestsView(getTimeline().getViewRange(), stage.getRequestsModel(), data -> {
      System.out.println(data.getId());
    });
    stage.aspect.addDependency()
      .setExecutor(ApplicationManager.getApplication()::invokeLater)
      .onChange(NetworkProfilerAspect.REQUEST_DETAILS, this::updateRequestDetailsView)
      .onChange(NetworkProfilerAspect.REQUESTS, this::updateRequestsView);

    JPanel l2Panel = new JPanel(); // TODO: L2 view should populate here

    Splitter l2l3splitter = new Splitter(true);
    l2l3splitter.setFirstComponent(l2Panel);
    l2l3splitter.setSecondComponent(myRequestsView);

    mySplitter = new Splitter(false);
    getComponent().add(mySplitter, BorderLayout.CENTER);
    mySplitter.setFirstComponent(l2l3splitter);
    mySplitter.setSecondComponent(myConnectionDetails);

    getChoreographer().register(myRequestsView);

    updateRequestsView();
    updateRequestDetailsView();
  }

  private void updateRequestDetailsView() {
    // TODO: Get the data from the model and fill it/update it.
    myConnectionDetails.removeAll();
    myConnectionDetails.setBackground(JBColor.background());
    myConnectionDetails.add(new JLabel("Connection ID: " + myStage.getConnectionId()), BorderLayout.CENTER);
    myConnectionDetails.setVisible(myStage.isConnectionDataEnabled());
    myConnectionDetails.revalidate();
  }

  private void updateRequestsView() {
  }

  @Override
  public JComponent getToolbar() {
    // TODO Replace with real network profiler toolbar elements. The following buttons are debug only.
    JPanel toolbar = new JPanel();
    JButton button = new JButton("<-");
    button.addActionListener(action -> {
      StudioProfilers profilers = getStage().getStudioProfilers();
      StudioMonitorStage monitor = new StudioMonitorStage(profilers);
      profilers.setStage(monitor);
    });
    toolbar.add(button);
    button = new JButton("Open details pane");
    button.addActionListener(action -> myStage.setEnableConnectionData(!myStage.isConnectionDataEnabled()));
    toolbar.add(button);

    return toolbar;
  }
}
