/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.runtime

import com.facebook.jni.HybridData
import com.facebook.jni.annotations.DoNotStrip
import com.facebook.soloader.SoLoader

public class JSCInstance constructor() : JSRuntimeFactory(initHybrid()) {
  private companion object {
    init {
      SoLoader.loadLibrary("jscinstance")
    }

    @DoNotStrip protected external fun initHybrid(): HybridData
  }
}
