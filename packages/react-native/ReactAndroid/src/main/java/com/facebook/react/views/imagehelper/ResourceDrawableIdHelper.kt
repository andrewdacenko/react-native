/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.imagehelper

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import javax.annotation.concurrent.ThreadSafe

/** Helper class for obtaining information about local images. */
@ThreadSafe
public class ResourceDrawableIdHelper private constructor() {
  private val resourceDrawableIdMap: MutableMap<String, Int> = HashMap()

  @Synchronized
  public fun clear() {
    resourceDrawableIdMap.clear()
  }

  public fun getResourceDrawableId(context: Context, name: String?): Int {
    if (name.isNullOrEmpty()) {
      return 0
    }
    val normalizedName = name.lowercase().replace("-", "_")

    // name could be a resource id.
    try {
      return normalizedName.toInt()
    } catch (e: NumberFormatException) {
      // Do nothing.
    }

    synchronized(this) {
      if (resourceDrawableIdMap.containsKey(normalizedName)) {
        return resourceDrawableIdMap.get(normalizedName)!!
      }
      return context.resources.getIdentifier(normalizedName, "drawable", context.packageName).also {
        resourceDrawableIdMap[normalizedName] = it
      }
    }
  }

  public fun getResourceDrawable(context: Context, name: String?): Drawable? {
    val resId = getResourceDrawableId(context, name)
    return if (resId > 0) ResourcesCompat.getDrawable(context.resources, resId, null) else null
  }

  public fun getResourceDrawableUri(context: Context, name: String?): Uri {
    val resId = getResourceDrawableId(context, name)
    return if (resId > 0) {
      Uri.Builder().scheme(LOCAL_RESOURCE_SCHEME).path(resId.toString()).build()
    } else {
      Uri.EMPTY
    }
  }

  public companion object {
    private const val LOCAL_RESOURCE_SCHEME = "res"
    private val resourceDrawableIdHelper: ResourceDrawableIdHelper = ResourceDrawableIdHelper()
    @JvmStatic
    public val instance: ResourceDrawableIdHelper
      get() = resourceDrawableIdHelper
  }
}
