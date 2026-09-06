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
}
