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
package com.android.tools.idea.editors.layoutInspector

import com.android.ddmlib.Client
import com.android.layoutinspector.model.ClientWindow
import com.android.layoutinspector.model.ViewNode
import java.awt.image.BufferedImage

data class LayoutInspectorModel @JvmOverloads constructor(val root: ViewNode,
                                val bufferedImage: BufferedImage,
                                val client: Client? = null,
                                val window: ClientWindow? = null) {
  val isConnected: Boolean
    get() = client != null && window != null
}

