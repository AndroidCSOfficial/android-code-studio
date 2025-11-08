/*
 * Enhanced version of BackgroundPreviewExtensions.kt
 * Now includes MaterialButton support alongside ImageView and background previews
 * Integrates VectorDrawableRenderer and MaterialDesign3Renderer with modular architecture
 */

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.inflater.internal.ViewImpl
import com.tom.rv2ide.projects.IWorkspace
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("BackgroundPreviewExtensions")

/** Extension function to add background preview to ViewImpl (ORIGINAL - with debug logging) */
fun ViewImpl.applyBackgroundPreview(
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File? = null,
) {
  // Find background attribute
  val backgroundAttr = this.attributes.find { it.name.equals("background", ignoreCase = true) }

  if (backgroundAttr != null) {
    // Try to get file from ViewImpl
    val viewFile =
        try {
          (this.file as? File)
        } catch (e: Exception) {
          null
        }

    val fileToUse = layoutFile ?: viewFile

    // Try vector renderer first for vector drawables
    val vectorRenderer = VectorDrawableRenderer()
    var applied =
        vectorRenderer.applyVectorPreview(
            this.view,
            backgroundAttr.value,
            context,
            fileToUse,
            isBackground = true,
        )

    // If vector failed, try regular background renderer
    if (!applied) {
      val renderer = BackgroundPreviewRenderer()
      applied = renderer.applyBackgroundPreview(this.view, backgroundAttr.value, context, fileToUse)
    }

    if (!applied) {
      // If all previews failed, fall back to normal attribute application
      try {
        this.applyAttribute(backgroundAttr)
      } catch (e: Exception) {
        // Ignore if normal application also fails
      }
    }
  }
}

/**
 * ENHANCED: Extension that handles background, ImageView src, MaterialButton, M3 components, and
 * vector drawables Now uses modular M3 renderer architecture
 */
fun ViewImpl.applyPreview(context: Context, workspace: IWorkspace?, layoutFile: File? = null) {
  val viewFile =
      try {
        (this.file as? File)
      } catch (e: Exception) {
        null
      }
  val fileToUse = layoutFile ?: viewFile

  // Initialize M3 renderer with workspace support
  val m3Renderer = MaterialDesign3Renderer(workspace)

  // Track which attributes we handle specially so we don't apply them twice
  val handledAttributes = mutableSetOf<String>()

  // Apply background preview if background attribute exists
  this.attributes
      .find { isBackgroundAttribute(it.name) }
      ?.let { attr ->
        handledAttributes.add(attr.name)

        // Try vector renderer first
        val vectorRenderer = VectorDrawableRenderer()
        var applied =
            vectorRenderer.applyVectorPreview(
                this.view,
                attr.value,
                context,
                fileToUse,
                isBackground = true,
            )

        // If vector failed, try regular background renderer
        if (!applied) {
          val backgroundRenderer = BackgroundPreviewRenderer()
          applied =
              backgroundRenderer.applyBackgroundPreview(this.view, attr.value, context, fileToUse)
        }

        // If all previews failed, fall back to normal attribute application
        if (!applied) {
          try {
            this.applyAttribute(attr)
          } catch (e: Exception) {
            // Ignore
          }
        }
      }

  // Apply ImageView src preview if this is an ImageView and src attribute exists
  if (this.view is ImageView) {
    val srcAttr = this.attributes.find { isSrcAttribute(it.name) }

    if (srcAttr != null) {
      handledAttributes.add(srcAttr.name)

      // Try vector renderer first for vector drawables
      val vectorRenderer = VectorDrawableRenderer()
      var applied =
          vectorRenderer.applyVectorPreview(
              this.view,
              srcAttr.value,
              context,
              fileToUse,
              isBackground = false,
          )

      // If vector failed, try regular image renderer
      if (!applied) {
        val imageRenderer = ImageViewSrcRenderer()
        applied =
            imageRenderer.applySrcPreview(this.view as ImageView, srcAttr.value, context, fileToUse)
      }

      // If both previews failed, fall back to normal attribute application
      if (!applied) {
        try {
          this.applyAttribute(srcAttr)
        } catch (e: Exception) {
          // Ignore
        }
      }
    }
  }

  // Apply M3 Material Design components preview using modular architecture
  if (isM3Component(this.view)) {
    var foundM3Attrs = 0

    // Handle M3 component attributes
    this.attributes.forEach { attr ->
      if (isM3Attribute(attr.name)) {
        foundM3Attrs++
        handledAttributes.add(attr.name)

        val applied =
            m3Renderer.applyM3Preview(this.view, attr.name, attr.value, context, fileToUse)

        // If M3 preview failed, fall back to normal attribute application
        if (!applied) {
          try {
            this.applyAttribute(attr)
          } catch (e: Exception) {
            // Ignore
          }
        }
      }
    }

    // Ensure M3 components have proper default styling even without explicit attributes
    if (foundM3Attrs == 0) {
      applyDefaultM3Styling(this.view, m3Renderer, context, fileToUse)
    }
  }

  // Apply all other attributes normally (excluding the ones we handled specially)
  this.attributes.forEach { attr ->
    if (attr.name !in handledAttributes) {
      try {
        this.applyAttribute(attr)
      } catch (e: Exception) {
        // Continue with other attributes even if one fails
      }
    }
  }
}

/**
 * ENHANCED: Apply preview for a specific attribute (for real-time editing) Uses modular M3 renderer
 * architecture with workspace support
 */
fun ViewImpl.applyAttributePreview(
    attributeName: String,
    attributeValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File? = null,
): Boolean {
  val viewFile =
      try {
        (this.file as? File)
      } catch (e: Exception) {
        null
      }
  val fileToUse = layoutFile ?: viewFile

  // Initialize M3 renderer with workspace support
  val m3Renderer = MaterialDesign3Renderer(workspace)

  return when {
    isBackgroundAttribute(attributeName) -> {
      // Try vector renderer first
      val vectorRenderer = VectorDrawableRenderer()
      var result =
          vectorRenderer.applyVectorPreview(
              this.view,
              attributeValue,
              context,
              fileToUse,
              isBackground = true,
          )

      // If vector failed, try regular background renderer
      if (!result) {
        val backgroundRenderer = BackgroundPreviewRenderer()
        result =
            backgroundRenderer.applyBackgroundPreview(this.view, attributeValue, context, fileToUse)
      }

      // If all previews failed, try normal attribute application
      if (!result) {
        try {
          val tempAttr = this.attributes.find { it.name.equals(attributeName, ignoreCase = true) }
          if (tempAttr != null) {
            tempAttr.value = attributeValue
            this.applyAttribute(tempAttr)
            return true
          }
        } catch (e: Exception) {
          // Ignore
        }
      }
      result
    }
    isSrcAttribute(attributeName) && this.view is ImageView -> {
      // Try vector renderer first
      val vectorRenderer = VectorDrawableRenderer()
      var result =
          vectorRenderer.applyVectorPreview(
              this.view,
              attributeValue,
              context,
              fileToUse,
              isBackground = false,
          )

      // If vector failed, try regular image renderer
      if (!result) {
        val imageRenderer = ImageViewSrcRenderer()
        result =
            imageRenderer.applySrcPreview(
                this.view as ImageView,
                attributeValue,
                context,
                fileToUse,
            )
      }

      // If all previews failed, try normal attribute application
      if (!result) {
        try {
          val tempAttr = this.attributes.find { it.name.equals(attributeName, ignoreCase = true) }
          if (tempAttr != null) {
            tempAttr.value = attributeValue
            this.applyAttribute(tempAttr)
            return true
          }
        } catch (e: Exception) {
          // Ignore
        }
      }
      result
    }
    isM3Attribute(attributeName) && isM3Component(this.view) -> {
      val result =
          m3Renderer.applyM3Preview(this.view, attributeName, attributeValue, context, fileToUse)

      // If M3 preview failed, try normal attribute application
      if (!result) {
        try {
          val tempAttr = this.attributes.find { it.name.equals(attributeName, ignoreCase = true) }
          if (tempAttr != null) {
            tempAttr.value = attributeValue
            this.applyAttribute(tempAttr)
            return true
          }
        } catch (e: Exception) {
          // Ignore
        }
      }

      // Ensure M3 components maintain visibility even after attribute changes
      if (result && isM3Component(this.view)) {
        ensureM3ComponentVisibility(this.view, m3Renderer, context, fileToUse)
      }

      result
    }
    else -> {
      // For other attributes, apply them normally
      try {
        val tempAttr = this.attributes.find { it.name.equals(attributeName, ignoreCase = true) }
        if (tempAttr != null) {
          tempAttr.value = attributeValue
          this.applyAttribute(tempAttr)
          return true
        }
      } catch (e: Exception) {
        // Ignore
      }
      false
    }
  }
}

/** Helper functions to identify attribute types */
private fun isBackgroundAttribute(attributeName: String): Boolean {
  return attributeName.lowercase() in listOf("background", "android:background")
}

private fun isSrcAttribute(attributeName: String): Boolean {
  return attributeName.lowercase() in
      listOf("src", "android:src", "srccompat", "android:srccompat", "app:srccompat")
}

/** Check if a view is an M3 Material Design component */
private fun isM3Component(view: android.view.View): Boolean {
  return view is MaterialButton ||
      view is MaterialCardView ||
      view is MaterialTextView ||
      view is FloatingActionButton ||
      view is Chip ||
      view is TextInputEditText ||
      view is TextInputLayout
}

/** Check if an attribute is an M3 attribute */
private fun isM3Attribute(attributeName: String): Boolean {
  val lowerName = attributeName.lowercase().replace("app:", "").replace("android:", "")

  return lowerName in
      listOf(
          // MaterialButton M3 attributes
          "icon",
          "iconsize",
          "icontint",
          "icongravity",
          "iconpadding",
          "cornerradius",
          "strokewidth",
          "strokecolor",
          "backgroundtint",
          "ripplecolor",
          "elevation",
          "surfacecolor",
          "textcolor",
          "textsize",
          "textstyle",
          "shapeappearance",
          "shapeappearanceoverlay",
          "checked",
          "enabled",

          // MaterialCardView M3 attributes
          "cardbackgroundcolor",
          "cardelevation",
          "radius",

          // MaterialTextView M3 attributes
          "textcolor",
          "textsize",
          "textstyle",

          // FloatingActionButton M3 attributes
          "src",
          "backgroundtint",
          "ripplecolor",
          "elevation",

          // Chip M3 attributes
          "chipicon",
          "chipbackgroundcolor",
          "chipstrokecolor",
          "chipstrokewidth",

          // TextInputLayout M3 attributes
          "hint",
          "helpertext",
          "errortext",
          "counterenabled",
          "countermaxlength",
          "hinttextcolor",
          "boxbackgroundcolor",
          "boxstrokecolor",
          "boxstrokewidth",
          "boxstrokewidthfocused",
          "boxcornerradius",
          "boxcornerradiustopleft",
          "boxcornerradiustopright",
          "boxcornerradiusbottomleft",
          "boxcornerradiusbottomright",
          "boxbackgroundmode",
          "starticon",
          "starticontint",
          "startcontentdescription",
          "endicon",
          "endicontint",
          "endiconmode",
          "endcontentdescription",
          "prefixtext",
          "suffixtext",
          "prefixtextcolor",
          "suffixtextcolor",
          "hintanimationenabled",
          "hintenabled",
          "error",
          "errorenabled",
          "helpertextenabled",
          "placeholdertext",
          "placeholdertextcolor",

          // TextInputEditText M3 attributes
          "text",
          "hint",
          "textcolor",
          "texthintcolor",
          "textsize",
          "textstyle",
          "gravity",
          "inputtype",
          "maxlength",
          "maxlines",
          "minlines",
          "lines",
          "singleline",
          "editable",
          "cursorvisible",
          "selectallonfocus",
          "imeactionlabel",
          "imeoptions",
          "drawablestart",
          "drawableend",
          "drawabletop",
          "drawablebottom",
          "drawabletint",
          "drawablepadding",

          // M3 color attributes
          "colorprimary",
          "coloronprimary",
          "colorprimarycontainer",
          "coloronprimarycontainer",
          "colorsecondary",
          "coloronsecondary",
          "colorsecondarycontainer",
          "coloronsecondarycontainer",
          "colortertiary",
          "colorontertiary",
          "colortertiarycontainer",
          "colorontertiarycontainer",
          "colorerror",
          "coloronerror",
          "colorerrorcontainer",
          "coloronerrorcontainer",
          "colorsurface",
          "coloronsurface",
          "colorsurfacevariant",
          "coloronsurfacevariant",
          "colorsurfacetint",
          "colorsurfacecontainer",
          "colorsurfacecontainerhigh",
          "colorsurfacecontainerhighest",
          "coloroutline",
          "coloroutlinevariant",
          "colorinversesurface",
          "colorinverseonsurface",
          "colorinverseprimary",
      )
}

/**
 * Apply default M3 styling to ensure M3 components are visible and properly styled This prevents M3
 * components from appearing invisible after re-entering preview
 */
private fun applyDefaultM3Styling(
    view: android.view.View,
    m3Renderer: MaterialDesign3Renderer,
    context: Context,
    layoutFile: File?,
) {
  try {
    when (view) {
      is MaterialButton -> {
        // Apply default M3 button styling
        m3Renderer.applyM3Preview(view, "backgroundtint", "?attr/colorPrimary", context, layoutFile)
        m3Renderer.applyM3Preview(view, "textcolor", "?attr/colorOnPrimary", context, layoutFile)
        m3Renderer.applyM3Preview(view, "cornerradius", "20dp", context, layoutFile)
        m3Renderer.applyM3Preview(view, "elevation", "2dp", context, layoutFile)
      }
      is MaterialCardView -> {
        // Apply default M3 card styling
        m3Renderer.applyM3Preview(
            view,
            "cardbackgroundcolor",
            "?attr/colorSurface",
            context,
            layoutFile,
        )
        m3Renderer.applyM3Preview(view, "cardelevation", "1dp", context, layoutFile)
        m3Renderer.applyM3Preview(view, "radius", "12dp", context, layoutFile)
      }
      is MaterialTextView -> {
        // Apply default M3 text styling
        m3Renderer.applyM3Preview(view, "textcolor", "?attr/colorOnSurface", context, layoutFile)
        m3Renderer.applyM3Preview(view, "textsize", "14sp", context, layoutFile)
      }
      is FloatingActionButton -> {
        // Apply default M3 FAB styling
        m3Renderer.applyM3Preview(
            view,
            "backgroundtint",
            "?attr/colorPrimaryContainer",
            context,
            layoutFile,
        )
        m3Renderer.applyM3Preview(view, "elevation", "6dp", context, layoutFile)
      }
      is Chip -> {
        // Apply default M3 chip styling
        m3Renderer.applyM3Preview(
            view,
            "chipbackgroundcolor",
            "?attr/colorSurfaceVariant",
            context,
            layoutFile,
        )
        m3Renderer.applyM3Preview(
            view,
            "chipstrokecolor",
            "?attr/colorOutline",
            context,
            layoutFile,
        )
      }
      is TextInputLayout -> {
        // Apply default M3 text input styling
        m3Renderer.applyM3Preview(
            view,
            "hinttextcolor",
            "?attr/colorOnSurfaceVariant",
            context,
            layoutFile,
        )
        m3Renderer.applyM3Preview(
            view,
            "boxbackgroundcolor",
            "?attr/colorSurface",
            context,
            layoutFile,
        )
      }
      is TextInputEditText -> {
        // Apply default M3 edit text styling
        m3Renderer.applyM3Preview(view, "textcolor", "?attr/colorOnSurface", context, layoutFile)
        m3Renderer.applyM3Preview(view, "textsize", "16sp", context, layoutFile)
      }
    }
  } catch (e: Exception) {
    log.debug("Failed to apply default M3 styling: {}", e.message)
  }
}

/**
 * Ensure M3 components maintain visibility after attribute changes This prevents M3 components from
 * becoming invisible during real-time editing
 */
private fun ensureM3ComponentVisibility(
    view: android.view.View,
    m3Renderer: MaterialDesign3Renderer,
    context: Context,
    layoutFile: File?,
) {
  try {
    when (view) {
      is MaterialButton -> {
        // Ensure button has visible background and text
        if (view.background == null || view.text.isNullOrEmpty()) {
          m3Renderer.applyM3Preview(
              view,
              "backgroundtint",
              "?attr/colorPrimary",
              context,
              layoutFile,
          )
          if (view.text.isNullOrEmpty()) {
            view.text = "Button"
          }
        }
      }
      is MaterialCardView -> {
        // Ensure card has visible background
        if (view.cardBackgroundColor == null) {
          m3Renderer.applyM3Preview(
              view,
              "cardbackgroundcolor",
              "?attr/colorSurface",
              context,
              layoutFile,
          )
        }
      }
      is MaterialTextView -> {
        // Ensure text is visible
        if (view.currentTextColor == 0) {
          m3Renderer.applyM3Preview(view, "textcolor", "?attr/colorOnSurface", context, layoutFile)
        }
      }
      is FloatingActionButton -> {
        // Ensure FAB has visible background
        if (view.backgroundTintList == null) {
          m3Renderer.applyM3Preview(
              view,
              "backgroundtint",
              "?attr/colorPrimaryContainer",
              context,
              layoutFile,
          )
        }
      }
      is Chip -> {
        // Ensure chip has visible background
        if (view.chipBackgroundColor == null) {
          m3Renderer.applyM3Preview(
              view,
              "chipbackgroundcolor",
              "?attr/colorSurfaceVariant",
              context,
              layoutFile,
          )
        }
      }
      is TextInputLayout -> {
        // Ensure text input has visible styling
        if (view.boxBackgroundColor == 0) {
          m3Renderer.applyM3Preview(
              view,
              "boxbackgroundcolor",
              "?attr/colorSurface",
              context,
              layoutFile,
          )
        }
      }
      is TextInputEditText -> {
        // Ensure edit text is visible
        if (view.currentTextColor == 0) {
          m3Renderer.applyM3Preview(view, "textcolor", "?attr/colorOnSurface", context, layoutFile)
        }
      }
    }
  } catch (e: Exception) {
    log.debug("Failed to ensure M3 component visibility: {}", e.message)
  }
}
