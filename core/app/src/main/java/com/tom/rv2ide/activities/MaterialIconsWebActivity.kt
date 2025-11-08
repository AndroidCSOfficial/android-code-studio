package com.tom.rv2ide.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import com.tom.rv2ide.R
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity

class MaterialIconsWebActivity : EdgeToEdgeIDEActivity() {

  private lateinit var webView: WebView

  @SuppressLint("SetJavaScriptEnabled")
  override fun bindLayout(): View {
    val container = layoutInflater.inflate(R.layout.activity_material_icons_web, null)
    val toolbar = container.findViewById<MaterialToolbar>(R.id.toolbar)
    val webContainer = container.findViewById<android.widget.FrameLayout>(R.id.web_container)

    webView = WebView(this)
    webView.layoutParams =
        android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
        )
    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
    webView.settings.allowFileAccess = true
    webView.settings.allowContentAccess = true
    webView.webViewClient = WebViewClient()
    webView.webChromeClient = WebChromeClient()
    webView.setBackgroundColor(Color.WHITE)
    webView.addJavascriptInterface(WebBridge(), "AndroidBridge")

    webContainer.addView(webView)
    toolbar.setNavigationOnClickListener { finish() }

    loadIconsHtml()

    return container
  }

  override fun onBackPressed() {
    super.onBackPressed()
  }

  private fun loadIconsHtml() {
    val html =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: 'Roboto', Arial, sans-serif;
                    background: #fafafa;
                    padding: 16px;
                }
                .search-box {
                    position: sticky;
                    top: 0;
                    background: white;
                    padding: 12px;
                    margin-bottom: 16px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    z-index: 100;
                }
                .search-box input {
                    width: 100%;
                    padding: 12px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    font-size: 16px;
                }
                .search-box input:focus {
                    outline: none;
                    border-color: #6200EE;
                }
                .icons-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
                    gap: 16px;
                    padding-bottom: 20px;
                }
                .icon-card {
                    background: white;
                    border-radius: 8px;
                    padding: 16px;
                    text-align: center;
                    cursor: pointer;
                    transition: all 0.2s;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    min-height: 120px;
                }
                .icon-card:active {
                    transform: scale(0.95);
                    background: #f5f5f5;
                }
                .icon-card svg {
                    width: 48px;
                    height: 48px;
                    fill: #6200EE;
                    margin-bottom: 8px;
                }
                .icon-name {
                    font-size: 11px;
                    color: #333;
                    word-break: break-word;
                    margin-top: 4px;
                }
                .loading {
                    text-align: center;
                    padding: 40px;
                    color: #666;
                    font-size: 16px;
                }
                .error {
                    text-align: center;
                    padding: 40px;
                    color: #d32f2f;
                }
                .stats {
                    text-align: center;
                    color: #666;
                    font-size: 14px;
                    margin-top: 8px;
                }
            </style>
        </head>
        <body>
            <div class="search-box">
                <input type="text" id="searchInput" placeholder="Search icons..." />
                <div class="stats" id="stats"></div>
            </div>
            <div class="loading" id="loading">Loading icons...</div>
            <div class="icons-grid" id="iconsGrid"></div>
            
            <script>
                const categories = [
                    'action', 'alert', 'av', 'communication', 'content', 'device', 'editor',
                    'file', 'hardware', 'image', 'maps', 'navigation', 'notification', 'places',
                    'social', 'toggle'
                ];
                
                const CACHE_KEY = 'material_icons_cache';
                const CACHE_VERSION = '1.0';
                const iconSvgMap = {}; // Store raw SVG strings separately
                let allIconNames = [];
                
                // Load from cache
                function loadFromCache() {
                    try {
                        const cached = localStorage.getItem(CACHE_KEY);
                        if (cached) {
                            const data = JSON.parse(cached);
                            if (data.version === CACHE_VERSION) {
                                return data;
                            }
                        }
                    } catch (e) {
                        console.error('Cache load error:', e);
                    }
                    return null;
                }
                
                // Save to cache
                function saveToCache(iconNames, icons) {
                    try {
                        const data = {
                            version: CACHE_VERSION,
                            timestamp: Date.now(),
                            iconNames: iconNames,
                            icons: icons
                        };
                        localStorage.setItem(CACHE_KEY, JSON.stringify(data));
                    } catch (e) {
                        console.error('Cache save error:', e);
                    }
                }
                
                // Fetch all icon names from the repository
                async function discoverAllIcons() {
                    const discoveredIcons = new Set();
                    
                    for (const category of categories) {
                        try {
                            const url = `https://api.github.com/repos/google/material-design-icons/contents/src/${'$'}{category}`;
                            const response = await fetch(url);
                            
                            if (response.ok) {
                                const data = await response.json();
                                data.forEach(item => {
                                    if (item.type === 'dir') {
                                        discoveredIcons.add(item.name);
                                    }
                                });
                            }
                        } catch (e) {
                            console.error(`Failed to load category: ${'$'}{category}`, e);
                        }
                    }
                    
                    return Array.from(discoveredIcons).sort();
                }
                
                async function loadIcon(iconName) {
                    for (const category of categories) {
                        try {
                            const url = `https://raw.githubusercontent.com/google/material-design-icons/master/src/${'$'}{category}/${'$'}{iconName}/materialicons/24px.svg`;
                            const response = await fetch(url);
                            if (response.ok) {
                                const svgText = await response.text();
                                if (svgText.includes('<path') || svgText.includes('<circle') || svgText.includes('<rect')) {
                                    return svgText;
                                }
                            }
                        } catch (e) {
                            continue;
                        }
                    }
                    return null;
                }
                
                function renderIcon(iconName, svgText) {
                    const grid = document.getElementById('iconsGrid');
                    
                    // Store the raw SVG text in our map
                    iconSvgMap[iconName] = svgText;
                    
                    const card = document.createElement('div');
                    card.className = 'icon-card';
                    card.dataset.name = iconName;
                    
                    // Insert the SVG for display
                    card.innerHTML = svgText + '<div class="icon-name">' + iconName.replace(/_/g, ' ') + '</div>';
                    
                    // Use click handler that retrieves from the map, not from DOM
                    card.onclick = function() {
                        const originalSvg = iconSvgMap[iconName];
                        if (originalSvg) {
                            console.log('Sending SVG for: ' + iconName);
                            console.log('SVG length: ' + originalSvg.length);
                            AndroidBridge.onIconClicked(iconName, originalSvg);
                        } else {
                            console.error('SVG not found in map for: ' + iconName);
                        }
                    };
                    
                    grid.appendChild(card);
                }
                
                async function loadAllIcons() {
                    const grid = document.getElementById('iconsGrid');
                    const loading = document.getElementById('loading');
                    const stats = document.getElementById('stats');
                    
                    // Try to load from cache first
                    const cached = loadFromCache();
                    
                    if (cached && cached.iconNames && cached.icons) {
                        loading.textContent = 'Loading from cache...';
                        allIconNames = cached.iconNames;
                        
                        let loadedCount = 0;
                        for (const iconName of allIconNames) {
                            if (cached.icons[iconName]) {
                                renderIcon(iconName, cached.icons[iconName]);
                                loadedCount++;
                            }
                        }
                        
                        loading.style.display = 'none';
                        stats.textContent = `${'$'}{loadedCount} icons loaded from cache`;
                        return;
                    }
                    
                    // No cache, fetch from network
                    loading.textContent = 'Discovering icons from repository...';
                    allIconNames = await discoverAllIcons();
                    
                    if (allIconNames.length === 0) {
                        loading.style.display = 'block';
                        loading.className = 'error';
                        loading.textContent = 'Failed to discover icons. Check internet connection.';
                        return;
                    }
                    
                    stats.textContent = `Found ${'$'}{allIconNames.length} icons`;
                    loading.textContent = `Loading icons... 0/${'$'}{allIconNames.length}`;
                    
                    let loadedCount = 0;
                    const iconsToCache = {};
                    
                    for (const iconName of allIconNames) {
                        const svg = await loadIcon(iconName);
                        if (svg) {
                            renderIcon(iconName, svg);
                            iconsToCache[iconName] = svg;
                            loadedCount++;
                            
                            loading.textContent = `Loading icons... ${'$'}{loadedCount}/${'$'}{allIconNames.length}`;
                            stats.textContent = `Loaded ${'$'}{loadedCount} of ${'$'}{allIconNames.length} icons`;
                        }
                    }
                    
                    loading.style.display = 'none';
                    stats.textContent = `${'$'}{loadedCount} icons loaded`;
                    
                    // Save to cache
                    saveToCache(allIconNames, iconsToCache);
                    
                    if (loadedCount === 0) {
                        loading.style.display = 'block';
                        loading.className = 'error';
                        loading.textContent = 'Failed to load icons. Check internet connection.';
                    }
                }
                
                // Search functionality
                document.getElementById('searchInput').addEventListener('input', function(e) {
                    const searchTerm = e.target.value.toLowerCase();
                    const cards = document.querySelectorAll('.icon-card');
                    let visibleCount = 0;
                    
                    cards.forEach(card => {
                        const iconName = card.dataset.name;
                        if (iconName.includes(searchTerm)) {
                            card.style.display = 'flex';
                            visibleCount++;
                        } else {
                            card.style.display = 'none';
                        }
                    });
                    
                    const stats = document.getElementById('stats');
                    if (searchTerm) {
                        stats.textContent = `${'$'}{visibleCount} icons match "${'$'}{searchTerm}"`;
                    } else {
                        stats.textContent = `${'$'}{cards.length} icons loaded`;
                    }
                });
                
                // Load icons on page load
                loadAllIcons();
            </script>
        </body>
        </html>
            """
            .trimIndent()

    webView.loadDataWithBaseURL("https://example.com", html, "text/html", "UTF-8", null)
  }

  inner class WebBridge {
    @JavascriptInterface
    fun onIconClicked(iconName: String, svgContent: String) {
      runOnUiThread {
        android.util.Log.d("MaterialIcons", "Received icon: $iconName")
        android.util.Log.d("MaterialIcons", "SVG content length: ${svgContent.length}")
        android.util.Log.d("MaterialIcons", "SVG content preview: ${svgContent.take(200)}")

        Toast.makeText(this@MaterialIconsWebActivity, "Converting: $iconName", Toast.LENGTH_SHORT)
            .show()

        val vectorXml = SvgToVectorConverter.convert(svgContent)
        if (vectorXml != null) {
          android.util.Log.d("MaterialIcons", "Generated Vector XML:\n$vectorXml")

          val data = Intent()
          data.putExtra("vectorXml", vectorXml)
          setResult(android.app.Activity.RESULT_OK, data)
          finish()
        } else {
          Toast.makeText(
                  this@MaterialIconsWebActivity,
                  "Failed to convert icon",
                  Toast.LENGTH_SHORT,
              )
              .show()
        }
      }
    }
  }
}

private object SvgToVectorConverter {
  fun convert(svg: String): String? {
    return try {
      val cleanSvg = svg.trim()

      android.util.Log.d("SvgConverter", "Input SVG:\n$cleanSvg")

      // Extract viewBox
      val viewBoxRegex = Regex("viewBox\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
      val viewBoxMatch = viewBoxRegex.find(cleanSvg)
      val viewBox = viewBoxMatch?.groupValues?.get(1) ?: "0 0 24 24"
      val parts = viewBox.trim().split(Regex("\\s+"))
      val vpW = parts.getOrNull(2)?.toFloatOrNull() ?: 24f
      val vpH = parts.getOrNull(3)?.toFloatOrNull() ?: 24f

      android.util.Log.d("SvgConverter", "ViewBox: $viewBox -> W:$vpW H:$vpH")

      // Find all path elements - match both self-closing and regular tags
      val pathRegex =
          Regex(
              "<path\\s+([^>]*?)(?:/>|></path>)",
              setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
          )
      val pathMatches = pathRegex.findAll(cleanSvg)
      val pathMatchesList = pathMatches.toList()

      android.util.Log.d("SvgConverter", "Found ${pathMatchesList.size} path elements")

      if (pathMatchesList.isEmpty()) {
        android.util.Log.e("SvgConverter", "No paths found in SVG")
        return null
      }

      val sb = StringBuilder()
      sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
      sb.append("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
      sb.append("    android:width=\"24dp\"\n")
      sb.append("    android:height=\"24dp\"\n")
      sb.append("    android:viewportWidth=\"$vpW\"\n")
      sb.append("    android:viewportHeight=\"$vpH\">\n")

      var validPaths = 0

      // Process each path
      pathMatchesList.forEachIndexed { index, match ->
        val pathAttrs = match.groupValues[1]
        android.util.Log.d("SvgConverter", "Path $index attributes: $pathAttrs")

        // Extract d attribute (path data)
        val dRegex = Regex("d\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val dMatch = dRegex.find(pathAttrs)
        val pathData = dMatch?.groupValues?.get(1)

        if (pathData.isNullOrEmpty()) {
          android.util.Log.w("SvgConverter", "Path $index has no d attribute")
          return@forEachIndexed
        }

        android.util.Log.d("SvgConverter", "Path $index data: ${pathData.take(50)}...")

        // Extract fill color
        val fillRegex = Regex("fill\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val fillMatch = fillRegex.find(pathAttrs)
        val fillValue = fillMatch?.groupValues?.get(1)

        android.util.Log.d("SvgConverter", "Path $index fill: $fillValue")

        val fillColor =
            when {
              fillValue == null -> "#FF000000" // Default to black
              fillValue.equals("none", ignoreCase = true) -> null
              fillValue.startsWith("#") -> {
                when (fillValue.length) {
                  4 -> "#FF" + fillValue.substring(1).map { "$it$it" }.joinToString("")
                  7 -> "#FF" + fillValue.substring(1)
                  9 -> fillValue
                  else -> "#FF000000"
                }
              }
              else -> "#FF000000"
            }

        // Extract stroke
        val strokeRegex = Regex("stroke\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val strokeMatch = strokeRegex.find(pathAttrs)
        val strokeValue = strokeMatch?.groupValues?.get(1)

        val strokeWidthRegex =
            Regex("stroke-width\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val strokeWidthMatch = strokeWidthRegex.find(pathAttrs)
        val strokeWidth = strokeWidthMatch?.groupValues?.get(1)

        // Build the path element
        sb.append("    <path\n")

        if (fillColor != null) {
          sb.append("        android:fillColor=\"$fillColor\"\n")
        }

        if (strokeValue != null && !strokeValue.equals("none", ignoreCase = true)) {
          val strokeColor =
              when {
                strokeValue.startsWith("#") -> {
                  when (strokeValue.length) {
                    4 -> "#FF" + strokeValue.substring(1).map { "$it$it" }.joinToString("")
                    7 -> "#FF" + strokeValue.substring(1)
                    9 -> strokeValue
                    else -> "#FF000000"
                  }
                }
                else -> "#FF000000"
              }
          sb.append("        android:strokeColor=\"$strokeColor\"\n")

          if (strokeWidth != null) {
            sb.append("        android:strokeWidth=\"$strokeWidth\"\n")
          }
        }

        sb.append("        android:pathData=\"$pathData\" />\n")
        validPaths++
      }

      sb.append("</vector>")

      android.util.Log.d("SvgConverter", "Successfully converted $validPaths paths")

      val result = sb.toString()
      result
    } catch (e: Exception) {
      android.util.Log.e("SvgConverter", "Conversion failed", e)
      e.printStackTrace()
      null
    }
  }
}
