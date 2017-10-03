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
package com.android.tools.idea.uibuilder.handlers.grid

import com.android.SdkConstants.GRID_LAYOUT_LIB_ARTIFACT
import com.android.tools.idea.common.scene.target.Target
import com.android.tools.idea.uibuilder.handlers.grid.targets.GridDragTarget
import com.google.common.collect.ImmutableList

/**
 * Handler for the `<android.support.v7.widget.GridLayout>` layout from AppCompat
 */
class GridLayoutV7Handler : GridLayoutHandler() {

  override fun getGradleCoordinateId(viewTag: String) = GRID_LAYOUT_LIB_ARTIFACT

  override fun createDragTarget(listBuilder: ImmutableList.Builder<Target>) = listBuilder.add(GridDragTarget(isSupportLibrary = true))
}