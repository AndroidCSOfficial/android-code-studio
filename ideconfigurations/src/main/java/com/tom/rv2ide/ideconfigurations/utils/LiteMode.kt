/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.ideconfigurations.utils

import android.app.ActivityManager
import android.content.Context

/**
 * Flag + deteksi mode ringan untuk flavor `lite` (target RAM 2GB).
 *
 * Dipakai untuk mematikan path fitur berat saat runtime (animasi blur,
 * seasonal effects, charts, uidesigner preview, dst.) tanpa menghapus
 * modul dari `settings.gradle.kts` (varian `full` tetap build penuh).
 */
object LiteMode {

  /** Master switch kompilasi/dokumentasi: true = build lite aktif dipangkas. */
  const val ENABLED = true

  /** Batas memoryClass (MB) yang dianggap perangkat RAM rendah. */
  const val LOW_RAM_MEMORY_CLASS_MB = 128

  /**
   * True jika perangkat RAM rendah: tandai via [ActivityManager.isLowRamDevice]
   * atau `memoryClass` kecil. Aman dipanggil dari flavor mana pun.
   */
  @JvmStatic
  fun isLowRam(context: Context): Boolean {
    val manager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    return manager.isLowRamDevice || manager.memoryClass <= LOW_RAM_MEMORY_CLASS_MB
  }

  /** Memory class perangkat dalam MB, -1 jika tidak terdeteksi. */
  @JvmStatic
  fun memoryClassMb(context: Context): Int {
    val manager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return -1
    return manager.memoryClass
  }

  // ---- Stage-2: default lite (aman, tanpa hapus enum Full) ----
  /** JDK default lite: hanya 17 agar hemat download (~200MB+). Full tetap sediakan 17+21. */
  const val LITE_JDK_VERSION = "17"

  /** SDK default lite: hanya 35.0.1 (ALL arch). Full tetap 6 versi. */
  const val LITE_SDK_VERSION = "35.0.1"

  /** NDK default lite: "0" = Skip (Kotlin/Java murni tidak butuh NDK). */
  const val LITE_NDK_VERSION = "0"

  /** Git/OpenSSH default lite: off (hanya untuk version control/SSH, bukan build). */
  const val LITE_INSTALL_GIT = false
  const val LITE_INSTALL_OPENSSH = false

  /**
   * True jika onboarding/setup harus pakai default lite.
   * Aman: return false jika context null/tidak low-RAM -> perilaku Full asli.
   */
  @JvmStatic
  fun shouldUseLiteDefaults(context: Context?): Boolean {
    if (!ENABLED || context == null) return false
    return try {
      isLowRam(context)
    } catch (_: Throwable) {
      false
    }
  }
}
