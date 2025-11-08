/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.tom.rv2ide.R
import com.tom.rv2ide.adapters.ColorPresetAdapter
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity
import com.tom.rv2ide.databinding.ActivityAssetStudioBinding
import com.tom.rv2ide.databinding.DialogColorPickerBinding
import com.tom.rv2ide.projects.IProjectManager
import java.io.File
import java.io.FileOutputStream

/**
 * Activity for the Asset Studio - Image Icon Generator. Generates app icons from images with
 * customizable backgrounds and shapes.
 *
 * @author Tom
 */
class AssetStudioActivity : EdgeToEdgeIDEActivity() {

  private var _binding: ActivityAssetStudioBinding? = null
  private val binding: ActivityAssetStudioBinding
    get() = checkNotNull(_binding)

  private var selectedImage: Bitmap? = null
  private var selectedXmlDrawable: Drawable? = null
  private var currentBackgroundColor: Int = Color.parseColor("#FF6200EE")
  private var currentIconShape: IconShape = IconShape.CIRCLE
  private var currentForegroundScale: Float = 0.9f
  private var currentRoundedCorners: Float = 0f
  private var isXmlMode: Boolean = false

  private val materialIconsLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val data = result.data
          val vectorXml = data?.getStringExtra("vectorXml")
          if (!vectorXml.isNullOrEmpty()) {
            selectedXmlDrawable = loadVectorDrawableFromXml(vectorXml)
            selectedImage = null
            isXmlMode = true
            updatePreview()
            if (selectedXmlDrawable != null) {
              Toast.makeText(this, "Icon imported from Material Icons", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(this, "Failed to load vector from selection", Toast.LENGTH_SHORT)
                  .show()
            }
          }
        }
      }

  private val imagePickerLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val data: Intent? = result.data
          val imageUri: Uri? = data?.data
          imageUri?.let { uri ->
            try {
              val inputStream = contentResolver.openInputStream(uri)
              selectedImage = BitmapFactory.decodeStream(inputStream)
              selectedXmlDrawable = null
              isXmlMode = false
              updatePreview()
            } catch (e: Exception) {
              Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
          }
        }
      }

  private val xmlPickerLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val data: Intent? = result.data
          val xmlUri: Uri? = data?.data
          xmlUri?.let { uri ->
            try {
              val inputStream = contentResolver.openInputStream(uri)
              val xmlContent = inputStream?.bufferedReader()?.use { it.readText() }
              if (xmlContent != null) {
                Toast.makeText(this, "XML content length: ${xmlContent.length}", Toast.LENGTH_SHORT)
                    .show()
                selectedXmlDrawable = loadVectorDrawableFromXml(xmlContent)
                selectedImage = null
                isXmlMode = true
                updatePreview()
                if (selectedXmlDrawable != null) {
                  Toast.makeText(this, "XML loaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(
                          this,
                          "Failed to create VectorDrawable from XML",
                          Toast.LENGTH_SHORT,
                      )
                      .show()
                }
              } else {
                Toast.makeText(this, "Failed to read XML content", Toast.LENGTH_SHORT).show()
              }
            } catch (e: Exception) {
              Toast.makeText(this, "Error loading XML: ${e.message}", Toast.LENGTH_SHORT).show()
            }
          }
        }
      }

  private fun setupUI() {
    binding.apply {
      // Set up toolbar
      toolbar.setNavigationOnClickListener { finish() }

      // Set up image selection
      selectImageButton.setOnClickListener { selectImage() }

      // Set up XML selection
      selectXmlButton.setOnClickListener { selectXmlFile() }

      // Set up icon shape selection using ChipGroup
      shapeChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
        when (checkedIds.firstOrNull()) {
          R.id.circle_shape_button -> updateIconShape(IconShape.CIRCLE)
          R.id.square_shape_button -> updateIconShape(IconShape.SQUARE)
          R.id.rounded_square_shape_button -> updateIconShape(IconShape.ROUNDED_SQUARE)
        }
      }

      // Set up color picker
      colorPickerButton.setOnClickListener { showColorPickerDialog() }

      customColorButton.setOnClickListener { showCustomColorDialog() }

      // Set up color presets
      setupColorPresets()

      // Set up sliders
      foregroundScaleSlider.addOnChangeListener { _, value, _ ->
        updateForegroundScale(value / 100f)
      }

      roundedCornersSlider.addOnChangeListener { _, value, _ -> updateRoundedCorners(value / 100f) }

      // Set up generation controls
      generateButton.setOnClickListener { generateImageAssets() }

      previewButton.setOnClickListener { showPreviewDialog() }
    }
  }

  private fun selectImage() {
    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    imagePickerLauncher.launch(intent)
  }

  private fun selectXmlFile() {
    val intent =
        Intent(Intent.ACTION_GET_CONTENT).apply {
          type = "text/xml"
          addCategory(Intent.CATEGORY_OPENABLE)
        }
    xmlPickerLauncher.launch(intent)
  }

  // Replace the loadVectorDrawableFromXml function with this improved version:

  private fun loadVectorDrawableFromXml(xmlContent: String): Drawable? {
    return try {
      // Create a temporary file in the cache directory
      val tempFile = File.createTempFile("temp_vector", ".xml", cacheDir)
      tempFile.writeText(xmlContent)

      // Create a temporary resource directory structure
      val tempResDir = File(cacheDir, "temp_res")
      val tempDrawableDir = File(tempResDir, "drawable")
      tempDrawableDir.mkdirs()

      val tempVectorFile = File(tempDrawableDir, "temp_vector.xml")
      tempVectorFile.writeText(xmlContent)

      try {
        // Method 1: Try to inflate using the system inflater
        val parser = resources.assets.openXmlResourceParser("drawable/temp_vector.xml")
        val drawable = Drawable.createFromXml(resources, parser)
        parser.close()

        if (drawable != null) {
          drawable.setBounds(0, 0, 512, 512)
          // Don't set tint here - preserve original colors
          tempFile.delete()
          tempResDir.deleteRecursively()
          return drawable
        }
      } catch (e: Exception) {
        // Method 1 failed, try Method 2
      }

      try {
        // Method 2: Parse XML manually and create VectorDrawable programmatically
        val drawable = parseVectorDrawableXml(xmlContent)
        if (drawable != null) {
          tempFile.delete()
          tempResDir.deleteRecursively()
          return drawable
        }
      } catch (e: Exception) {
        // Method 2 failed, try Method 3
      }

      try {
        // Method 3: Create bitmap from XML content using XmlPullParser
        val drawable = createVectorDrawableFromXmlContent(xmlContent)
        tempFile.delete()
        tempResDir.deleteRecursively()
        return drawable
      } catch (e: Exception) {
        // All methods failed, clean up and return placeholder
        tempFile.delete()
        tempResDir.deleteRecursively()
        return createPlaceholderDrawable()
      }
    } catch (e: Exception) {
      return createPlaceholderDrawable()
    }
  }

  private fun parseVectorDrawableXml(xmlContent: String): VectorDrawable? {
    return try {
      val parser = android.util.Xml.newPullParser()
      parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
      parser.setInput(xmlContent.byteInputStream(), "UTF-8")

      // Find the vector element
      var eventType = parser.eventType
      while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "vector") {
          // Found vector element, try to create VectorDrawable
          val drawable = VectorDrawable()

          // Set basic properties
          drawable.setBounds(0, 0, 512, 512)

          // Try to inflate from the parser position
          try {
            drawable.inflate(resources, parser, android.util.Xml.asAttributeSet(parser))
            return drawable
          } catch (e: Exception) {
            // Inflation failed, continue to next method
            break
          }
        }
        eventType = parser.next()
      }

      null
    } catch (e: Exception) {
      null
    }
  }

  private fun createVectorDrawableFromXmlContent(xmlContent: String): Drawable? {
    return try {
      // Create a bitmap and draw the vector content onto it
      val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Parse the XML to extract basic vector information
      val parser = android.util.Xml.newPullParser()
      parser.setInput(xmlContent.byteInputStream(), "UTF-8")

      var width = 512f
      var height = 512f
      var viewportWidth = 24f
      var viewportHeight = 24f
      val paths = mutableListOf<VectorPath>()

      var eventType = parser.eventType
      while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          org.xmlpull.v1.XmlPullParser.START_TAG -> {
            when (parser.name) {
              "vector" -> {
                // Parse vector attributes
                for (i in 0 until parser.attributeCount) {
                  when (parser.getAttributeName(i)) {
                    "width" -> width = parseDimension(parser.getAttributeValue(i))
                    "height" -> height = parseDimension(parser.getAttributeValue(i))
                    "viewportWidth" ->
                        viewportWidth = parser.getAttributeValue(i).toFloatOrNull() ?: 24f
                    "viewportHeight" ->
                        viewportHeight = parser.getAttributeValue(i).toFloatOrNull() ?: 24f
                  }
                }
              }
              "path" -> {
                // Parse path attributes
                var pathData = ""
                var fillColor = Color.BLACK
                var strokeColor = Color.TRANSPARENT
                var strokeWidth = 0f

                for (i in 0 until parser.attributeCount) {
                  when (parser.getAttributeName(i)) {
                    "pathData" -> pathData = parser.getAttributeValue(i)
                    "fillColor" -> fillColor = parseColor(parser.getAttributeValue(i))
                    "strokeColor" -> strokeColor = parseColor(parser.getAttributeValue(i))
                    "strokeWidth" -> strokeWidth = parser.getAttributeValue(i).toFloatOrNull() ?: 0f
                  }
                }

                if (pathData.isNotEmpty()) {
                  paths.add(VectorPath(pathData, fillColor, strokeColor, strokeWidth))
                }
              }
            }
          }
        }
        eventType = parser.next()
      }

      // Draw the vector paths onto the bitmap
      canvas.drawColor(Color.TRANSPARENT)

      val scaleX = 512f / viewportWidth
      val scaleY = 512f / viewportHeight
      val scale = minOf(scaleX, scaleY)

      canvas.save()
      canvas.scale(scale, scale)
      canvas.translate((512f / scale - viewportWidth) / 2f, (512f / scale - viewportHeight) / 2f)

      paths.forEach { vectorPath ->
        try {
          val path = android.graphics.Path()
          val pathParser =
              androidx.core.graphics.PathParser.createPathFromPathData(vectorPath.pathData)
          path.set(pathParser)

          // Draw fill
          if (vectorPath.fillColor != Color.TRANSPARENT) {
            val fillPaint =
                Paint().apply {
                  color = vectorPath.fillColor
                  style = Paint.Style.FILL
                  isAntiAlias = true
                }
            canvas.drawPath(path, fillPaint)
          }

          // Draw stroke
          if (vectorPath.strokeColor != Color.TRANSPARENT && vectorPath.strokeWidth > 0) {
            val strokePaint =
                Paint().apply {
                  color = vectorPath.strokeColor
                  style = Paint.Style.STROKE
                  strokeWidth = vectorPath.strokeWidth
                  isAntiAlias = true
                }
            canvas.drawPath(path, strokePaint)
          }
        } catch (e: Exception) {
          // Skip this path if parsing fails
        }
      }

      canvas.restore()

      android.graphics.drawable.BitmapDrawable(resources, bitmap)
    } catch (e: Exception) {
      null
    }
  }

  private fun parseDimension(value: String): Float {
    return when {
      value.endsWith("dp") ->
          value.dropLast(2).toFloatOrNull()?.times(resources.displayMetrics.density) ?: 512f
      value.endsWith("px") -> value.dropLast(2).toFloatOrNull() ?: 512f
      value.endsWith("sp") ->
          value.dropLast(2).toFloatOrNull()?.times(resources.displayMetrics.scaledDensity) ?: 512f
      else -> value.toFloatOrNull() ?: 512f
    }
  }

  private fun parseColor(colorString: String): Int {
    return try {
      when {
        colorString.startsWith("#") -> Color.parseColor(colorString)
        colorString.startsWith("@") -> {
          // This is a resource reference, use a default color
          Color.BLACK
        }
        else -> Color.parseColor("#$colorString")
      }
    } catch (e: Exception) {
      Color.BLACK
    }
  }

  private data class VectorPath(
      val pathData: String,
      val fillColor: Int,
      val strokeColor: Int,
      val strokeWidth: Float,
  )

  // Also update the createPlaceholderDrawable function to be more informative:
  private fun createPlaceholderDrawable(): Drawable {
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint =
        Paint().apply {
          isAntiAlias = true
          textAlign = Paint.Align.CENTER
        }

    // Background
    paint.color = Color.parseColor("#F5F5F5")
    paint.style = Paint.Style.FILL
    canvas.drawRect(0f, 0f, 512f, 512f, paint)

    // Border
    paint.color = Color.parseColor("#CCCCCC")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawRect(2f, 2f, 510f, 510f, paint)

    // Icon
    paint.color = Color.parseColor("#666666")
    paint.style = Paint.Style.FILL
    paint.textSize = 48f
    canvas.drawText("XML", 256f, 240f, paint)

    // Subtitle
    paint.textSize = 24f
    paint.color = Color.parseColor("#999999")
    canvas.drawText("Vector Drawable", 256f, 280f, paint)

    return android.graphics.drawable.BitmapDrawable(resources, bitmap)
  }

  private fun updatePreview() {
    if (isXmlMode && selectedXmlDrawable != null) {
      // For XML drawables, display directly as VectorDrawable
      binding.previewImageView.setImageDrawable(selectedXmlDrawable)
    } else {
      selectedImage?.let { bitmap ->
        val transformedBitmap = applyImageTransformations(bitmap)
        binding.previewImageView.setImageBitmap(transformedBitmap)
      }
    }
  }

  private fun createBitmapFromDrawable(drawable: Drawable): Bitmap? {
    return try {
      val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Clear the canvas with transparent background
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

      // Set bounds and draw the drawable
      drawable.setBounds(0, 0, 512, 512)
      drawable.draw(canvas)

      bitmap
    } catch (e: Exception) {
      // Create a fallback bitmap
      val fallbackBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(fallbackBitmap)
      val paint =
          Paint().apply {
            color = Color.GRAY
            style = Paint.Style.FILL
            textSize = 48f
            textAlign = Paint.Align.CENTER
          }
      canvas.drawColor(Color.LTGRAY)
      canvas.drawText("XML", 256f, 256f, paint)
      fallbackBitmap
    }
  }

  private fun applyImageTransformations(originalBitmap: Bitmap): Bitmap {
    val size = 512 // High resolution for better quality
    val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // Draw background with proper shape
    val backgroundPaint =
        Paint().apply {
          color = currentBackgroundColor
          isAntiAlias = true
        }

    when (currentIconShape) {
      IconShape.CIRCLE -> {
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint)
      }
      IconShape.SQUARE -> {
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), backgroundPaint)
      }
      IconShape.ROUNDED_SQUARE -> {
        val cornerRadius = currentRoundedCorners * size
        canvas.drawRoundRect(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            cornerRadius,
            cornerRadius,
            backgroundPaint,
        )
      }
    }

    // Draw foreground image with scaling
    val foregroundSize = (size * currentForegroundScale).toInt()
    val scaledBitmap =
        Bitmap.createScaledBitmap(originalBitmap, foregroundSize, foregroundSize, true)
    val left = (size - foregroundSize) / 2f
    val top = (size - foregroundSize) / 2f

    // Apply shape mask to foreground
    val maskBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(maskBitmap)
    val maskPaint =
        Paint().apply {
          color = Color.BLACK
          isAntiAlias = true
        }

    when (currentIconShape) {
      IconShape.CIRCLE -> {
        maskCanvas.drawCircle(size / 2f, size / 2f, size / 2f, maskPaint)
      }
      IconShape.SQUARE -> {
        maskCanvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), maskPaint)
      }
      IconShape.ROUNDED_SQUARE -> {
        val cornerRadius = currentRoundedCorners * size
        maskCanvas.drawRoundRect(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            cornerRadius,
            cornerRadius,
            maskPaint,
        )
      }
    }

    // Apply mask to foreground
    val maskedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val maskedCanvas = Canvas(maskedBitmap)
    val maskedPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }

    maskedCanvas.drawBitmap(scaledBitmap, left, top, null)
    maskedCanvas.drawBitmap(maskBitmap, 0f, 0f, maskedPaint)

    // Draw the masked foreground onto the background
    canvas.drawBitmap(maskedBitmap, 0f, 0f, null)

    return result
  }

  private fun updateIconShape(shape: IconShape) {
    currentIconShape = shape
    binding.apply {
      circleShapeButton.isSelected = shape == IconShape.CIRCLE
      squareShapeButton.isSelected = shape == IconShape.SQUARE
      roundedSquareShapeButton.isSelected = shape == IconShape.ROUNDED_SQUARE
    }
    updatePreview()
  }

  private fun updateBackgroundColor(color: Int) {
    currentBackgroundColor = color
    binding.backgroundColorPreview.setBackgroundColor(color)
    updatePreview()
  }

  private fun updateForegroundScale(scale: Float) {
    currentForegroundScale = scale
    binding.foregroundScaleValue.text = "${(scale * 100).toInt()}%"
    updatePreview()
  }

  private fun updateRoundedCorners(radius: Float) {
    currentRoundedCorners = radius
    binding.roundedCornersValue.text = "${(radius * 100).toInt()}%"
    updatePreview()
  }

  private fun showColorPickerDialog() {
    val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
    val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

    // Set up current color
    dialogBinding.currentColorPreview.setBackgroundColor(currentBackgroundColor)
    dialogBinding.currentColorHex.text = String.format("#%08X", currentBackgroundColor)
    dialogBinding.hexColorInput.setText(String.format("#%08X", currentBackgroundColor))

    // Set up color presets
    val colorPresets = ColorPresetAdapter.getDefaultColors()
    val adapter =
        ColorPresetAdapter(colorPresets) { color ->
          currentBackgroundColor = color
          dialogBinding.currentColorPreview.setBackgroundColor(color)
          dialogBinding.currentColorHex.text = String.format("#%08X", color)
          dialogBinding.hexColorInput.setText(String.format("#%08X", color))
        }

    dialogBinding.colorPresetsRecycler.layoutManager =
        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    dialogBinding.colorPresetsRecycler.adapter = adapter

    // Set up hex input
    dialogBinding.hexColorInput.addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

          override fun afterTextChanged(s: Editable?) {
            try {
              val color = Color.parseColor(s.toString())
              currentBackgroundColor = color
              dialogBinding.currentColorPreview.setBackgroundColor(color)
              dialogBinding.currentColorHex.text = String.format("#%08X", color)
            } catch (e: Exception) {
              // Invalid color format, ignore
            }
          }
        }
    )

    // Set up buttons
    dialogBinding.cancelColorButton.setOnClickListener { dialog.dismiss() }

    dialogBinding.applyColorButton.setOnClickListener {
      updateBackgroundColor(currentBackgroundColor)
      dialog.dismiss()
    }

    dialog.show()
  }

  private fun showCustomColorDialog() {
    val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
    val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

    // Set up current color
    dialogBinding.currentColorPreview.setBackgroundColor(currentBackgroundColor)
    dialogBinding.currentColorHex.text = String.format("#%08X", currentBackgroundColor)
    dialogBinding.hexColorInput.setText(String.format("#%08X", currentBackgroundColor))

    // Set up color presets
    val colorPresets = ColorPresetAdapter.getDefaultColors()
    val adapter =
        ColorPresetAdapter(colorPresets) { color ->
          currentBackgroundColor = color
          dialogBinding.currentColorPreview.setBackgroundColor(color)
          dialogBinding.currentColorHex.text = String.format("#%08X", color)
          dialogBinding.hexColorInput.setText(String.format("#%08X", color))
        }

    dialogBinding.colorPresetsRecycler.layoutManager =
        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    dialogBinding.colorPresetsRecycler.adapter = adapter

    // Set up hex input
    dialogBinding.hexColorInput.addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

          override fun afterTextChanged(s: Editable?) {
            try {
              val color = Color.parseColor(s.toString())
              currentBackgroundColor = color
              dialogBinding.currentColorPreview.setBackgroundColor(color)
              dialogBinding.currentColorHex.text = String.format("#%08X", color)
            } catch (e: Exception) {
              // Invalid color format, ignore
            }
          }
        }
    )

    // Set up buttons
    dialogBinding.cancelColorButton.setOnClickListener { dialog.dismiss() }

    dialogBinding.applyColorButton.setOnClickListener {
      updateBackgroundColor(currentBackgroundColor)
      dialog.dismiss()
    }

    dialog.show()
  }

  private fun setupColorPresets() {
    val colorPresets = ColorPresetAdapter.getDefaultColors()
    val adapter = ColorPresetAdapter(colorPresets) { color -> updateBackgroundColor(color) }

    binding.colorPresetsContainer.removeAllViews()

    // Add color preset buttons directly to the container
    colorPresets.forEach { color ->
      val button =
          com.google.android.material.button.MaterialButton(this).apply {
            layoutParams =
                android.widget.LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.color_preset_size),
                        resources.getDimensionPixelSize(R.dimen.color_preset_size),
                    )
                    .apply { setMargins(4, 0, 4, 0) }
            setBackgroundColor(color)
            setOnClickListener { updateBackgroundColor(color) }
          }
      binding.colorPresetsContainer.addView(button)
    }
  }

  private fun createFinalIcon(originalBitmap: Bitmap, size: Int): Bitmap {
    val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // Draw background
    val backgroundPaint =
        Paint().apply {
          color = currentBackgroundColor
          isAntiAlias = true
        }

    when (currentIconShape) {
      IconShape.CIRCLE -> {
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint)
      }
      IconShape.SQUARE -> {
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), backgroundPaint)
      }
      IconShape.ROUNDED_SQUARE -> {
        val cornerRadius = currentRoundedCorners * size
        canvas.drawRoundRect(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            cornerRadius,
            cornerRadius,
            backgroundPaint,
        )
      }
    }

    // Draw foreground image with scaling
    val foregroundSize = (size * currentForegroundScale).toInt()
    val scaledBitmap =
        Bitmap.createScaledBitmap(originalBitmap, foregroundSize, foregroundSize, true)
    val left = (size - foregroundSize) / 2f
    val top = (size - foregroundSize) / 2f

    // Apply shape mask to foreground
    val maskBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(maskBitmap)
    val maskPaint =
        Paint().apply {
          color = Color.BLACK
          isAntiAlias = true
        }

    when (currentIconShape) {
      IconShape.CIRCLE -> {
        maskCanvas.drawCircle(size / 2f, size / 2f, size / 2f, maskPaint)
      }
      IconShape.SQUARE -> {
        maskCanvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), maskPaint)
      }
      IconShape.ROUNDED_SQUARE -> {
        val cornerRadius = currentRoundedCorners * size
        maskCanvas.drawRoundRect(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            cornerRadius,
            cornerRadius,
            maskPaint,
        )
      }
    }

    // Apply mask to foreground
    val maskedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val maskedCanvas = Canvas(maskedBitmap)
    val maskedPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }

    maskedCanvas.drawBitmap(scaledBitmap, left, top, null)
    maskedCanvas.drawBitmap(maskBitmap, 0f, 0f, maskedPaint)

    // Draw the masked foreground onto the background
    canvas.drawBitmap(maskedBitmap, 0f, 0f, null)

    return result
  }

  private fun showPreviewDialog() {
    if (isXmlMode && selectedXmlDrawable != null) {
      // For XML drawables, show directly
      binding.previewImageView.setImageDrawable(selectedXmlDrawable)
      Toast.makeText(this, "XML Preview updated", Toast.LENGTH_SHORT).show()
    } else {
      selectedImage?.let { bitmap ->
        val transformedBitmap = applyImageTransformations(bitmap)
        binding.previewImageView.setImageBitmap(transformedBitmap)
        Toast.makeText(this, "Image Preview updated", Toast.LENGTH_SHORT).show()
      }
          ?: run {
            Toast.makeText(this, "Please select an image or XML file first", Toast.LENGTH_SHORT)
                .show()
          }
    }
  }

  override fun bindLayout(): android.view.View {
    _binding = ActivityAssetStudioBinding.inflate(layoutInflater)
    setupUI()
    handleInitialAction()
    return binding.root
  }

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
  }

  enum class IconShape {
    CIRCLE,
    SQUARE,
    ROUNDED_SQUARE,
  }

  private fun handleInitialAction() {
    val action = intent?.getStringExtra("action") ?: return
    if (action == "material_icons") {
      showMaterialIconsImportDialog()
    }
  }

  private fun showMaterialIconsImportDialog() {
    val options = arrayOf("Browse in app", "Paste Vector XML")
    AlertDialog.Builder(this)
        .setTitle("Material Icons")
        .setItems(options) { dialog, which ->
          when (which) {
            0 -> materialIconsLauncher.launch(Intent(this, MaterialIconsWebActivity::class.java))
            1 -> {
              showPasteXmlDialog()
            }
          }
          dialog.dismiss()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun showPasteXmlDialog() {
    val input =
        android.widget.EditText(this).apply {
          setText("")
          hint = "Paste <vector> XML here"
          setPadding(32, 24, 32, 24)
          minLines = 6
          maxLines = 16
          isSingleLine = false
          setHorizontallyScrolling(false)
          layoutParams =
              android.widget.LinearLayout.LayoutParams(
                  android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                  android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
              )
        }
    AlertDialog.Builder(this)
        .setTitle("Import Vector XML")
        .setView(input)
        .setPositiveButton("Import") { d, _ ->
          val xml = input.text?.toString()?.trim()
          if (!xml.isNullOrEmpty()) {
            selectedXmlDrawable = loadVectorDrawableFromXml(xml)
            selectedImage = null
            isXmlMode = true
            updatePreview()
            if (selectedXmlDrawable != null) {
              Toast.makeText(this, "Vector imported", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(this, "Failed to parse vector XML", Toast.LENGTH_SHORT).show()
            }
          }
          d.dismiss()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun promptSaveLocationAndGenerate(sourceBitmap: Bitmap, iconName: String) {
    val locations = arrayOf("drawable", "mipmap")
    var selected = 0
    AlertDialog.Builder(this)
        .setTitle("Save to")
        .setSingleChoiceItems(locations, selected) { _, which -> selected = which }
        .setPositiveButton("Generate") { dialog, _ ->
          val useMipmap = (selected == 1)
          generateToLocation(sourceBitmap, iconName, useMipmap)
          dialog.dismiss()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun generateToLocation(sourceBitmap: Bitmap, iconName: String, useMipmap: Boolean) {
    try {
      val projectDir = com.tom.rv2ide.projects.IProjectManager.getInstance().projectDirPath
      if (projectDir == null) {
        Toast.makeText(this, "No project opened", Toast.LENGTH_SHORT).show()
        return
      }
      val resDir = java.io.File(projectDir, "app/src/main/res")
      val base = if (useMipmap) "mipmap" else "drawable"
      val baseDir = java.io.File(resDir, base)
      baseDir.mkdirs()

      val densities =
          listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)

      var generatedCount = 0

      // Main base asset
      val mainFinalIcon = createFinalIcon(sourceBitmap, 96)
      val mainIconFile = java.io.File(baseDir, "$iconName.png")
      java.io.FileOutputStream(mainIconFile).use { out ->
        mainFinalIcon.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
      }
      generatedCount++

      // Density-specific assets
      densities.forEach { (density, size) ->
        val densityDir = java.io.File(resDir, "$base-$density")
        densityDir.mkdirs()
        val finalIcon = createFinalIcon(sourceBitmap, size)
        val iconFile = java.io.File(densityDir, "$iconName.png")
        java.io.FileOutputStream(iconFile).use { out ->
          finalIcon.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        generatedCount++
      }

      Toast.makeText(this, "Generated $generatedCount icons in res/$base-*", Toast.LENGTH_LONG)
          .show()
    } catch (e: Exception) {
      Toast.makeText(this, "Error generating icons: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  // Updated to prompt for save location
  private fun generateImageAssets() {
    val iconName = binding.iconNameInput.text.toString().ifEmpty { "ic_my_icon" }
    val sourceBitmap =
        if (isXmlMode && selectedXmlDrawable != null) {
          createBitmapFromDrawable(selectedXmlDrawable!!)
        } else {
          selectedImage
        }
    if (sourceBitmap == null) {
      Toast.makeText(this, "Please select an image or XML file first", Toast.LENGTH_SHORT).show()
      return
    }
    promptSaveLocationAndGenerate(sourceBitmap, iconName)
  }
}
