/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tools.profilers.cpu.capturedetails;

import static com.android.tools.profilers.cpu.capturedetails.CaptureNodeHRenderer.toUnmatchColor;

import com.android.tools.adtui.common.DataVisualizationColors;
import com.android.tools.adtui.common.EnumColors;
import com.android.tools.profilers.ProfilerColors;
import com.android.tools.profilers.cpu.CpuProfilerStage;
import com.android.tools.profilers.cpu.nodemodel.AtraceNodeModel;
import com.android.tools.profilers.cpu.nodemodel.CaptureNodeModel;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the colors (fill and border) of the rectangles used to represent {@link AtraceNodeModel} nodes in a
 * {@link com.android.tools.adtui.chart.hchart.HTreeChart}.
 */
class AtraceNodeModelHChartColors {

  private static EnumColors<CpuProfilerStage.ThreadState> threadColors = ProfilerColors.THREAD_STATES.build();

  private static void validateModel(@NotNull CaptureNodeModel model) {
    if (!(model instanceof AtraceNodeModel)) {
      throw new IllegalStateException("Model must be an instance of AtraceNodeModel.");
    }
  }

  /**
   * For cpu idle time (time the node is scheduled wall clock, but not thread time), we match thread colors.
   */
  static Color getIdleCpuColor(@NotNull CaptureNodeModel model,
                               CaptureDetails.Type chartType,
                               boolean isUnmatched,
                               boolean isFocused,
                               boolean isDeselected) {
    Color color;
    if (chartType == CaptureDetails.Type.CALL_CHART) {
      int index = model.getFullName().hashCode();
      if (isDeselected) {
        color = DataVisualizationColors.INSTANCE.getColor(DataVisualizationColors.BACKGROUND_DATA_COLOR, 1, isFocused);
      }
      else if (JBColor.isBright()) {
        color = ColorUtil.darker(DataVisualizationColors.INSTANCE.getColor(index, isFocused), 5);
      }
      else {
        color = ColorUtil.brighter(DataVisualizationColors.INSTANCE.getColor(index, isFocused), 5);
      }
    }
    else {
      // Atrace captures do not know where calls come from so we always use APP.
      color = isFocused ? ProfilerColors.CPU_FLAMECHART_APP_HOVER_IDLE : ProfilerColors.CPU_FLAMECHART_APP_IDLE;
    }
    return isUnmatched ? toUnmatchColor(color) : color;
  }

  /**
   * We use the usage captured color. This gives the UI a consistent look
   * across CPU, Kernel, Threads, and trace nodes.
   */
  static Color getFillColor(@NotNull CaptureNodeModel model,
                            CaptureDetails.Type chartType,
                            boolean isUnmatched,
                            boolean isFocused,
                            boolean isDeselected) {
    validateModel(model);
    Color color;
    if (chartType == CaptureDetails.Type.CALL_CHART) {
      int index = model.getFullName().hashCode();
      color = isDeselected ?
              DataVisualizationColors.INSTANCE.getColor(DataVisualizationColors.BACKGROUND_DATA_COLOR, isFocused) :
              DataVisualizationColors.INSTANCE.getColor(index, isFocused);
    }
    else {
      // Atrace captures do not know where calls come from so we always use APP.
      color = isFocused ? ProfilerColors.CPU_FLAMECHART_APP_HOVER : ProfilerColors.CPU_FLAMECHART_APP;
    }
    return isUnmatched ? toUnmatchColor(color) : color;
  }
}
