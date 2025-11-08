/*
 *  This file is part of AndroidTC.
 *
 *  AndroidTC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidTC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with AndroidTC.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.androidcodestudio.project.manager.builder.module

import java.io.File

/**
 * Data class representing a ProGuard rule entry.
 *
 * @property rule The ProGuard rule content
 * @property comment Optional comment for the rule
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
data class ProguardRule(val rule: String, val comment: String? = null)

/** Interface for writing proguard-rules.pro files. */
interface MLProguardRules {

  /**
   * Generates the proguard-rules.pro file content from a raw string.
   *
   * @param content The raw content to write
   * @return The content as a String
   */
  fun generate(content: String): String

  /**
   * Generates the proguard-rules.pro file content from rule entries.
   *
   * @param rules List of ProGuard rule entries
   * @param headerComment Optional header comment for the file
   * @return The generated content as a String
   */
  fun generate(rules: List<ProguardRule>, headerComment: String? = null): String

  /**
   * Writes raw content to proguard-rules.pro file.
   *
   * @param outputDir The directory where the file will be created
   * @param content The raw content to write
   * @return The created File object
   */
  fun writeToFile(outputDir: File, content: String): File

  /**
   * Writes rule entries to proguard-rules.pro file.
   *
   * @param outputDir The directory where the file will be created
   * @param rules List of ProGuard rule entries
   * @param headerComment Optional header comment for the file
   * @return The created File object
   */
  fun writeToFile(outputDir: File, rules: List<ProguardRule>, headerComment: String? = null): File
}

/** Default implementation of MLProguardRules for generating proguard-rules.pro files. */
class ProguardRulesWriter : MLProguardRules {

  companion object {
    const val FILE_NAME = "proguard-rules.pro"
  }

  override fun generate(content: String): String {
    return content.trimIndent()
  }

  override fun generate(rules: List<ProguardRule>, headerComment: String?): String {
    val builder = StringBuilder()

    // Add header comment if provided
    if (headerComment != null) {
      builder.appendLine("# $headerComment")
      builder.appendLine()
    }

    // Add rules
    rules.forEach { rule ->
      // Add rule comment if provided
      if (rule.comment != null) {
        builder.appendLine("# ${rule.comment}")
      }
      builder.appendLine(rule.rule.trimIndent())
      builder.appendLine()
    }

    return builder.toString().trimEnd() + "\n"
  }

  override fun writeToFile(outputDir: File, content: String): File {
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val generatedContent = generate(content)
    val file = File(outputDir, FILE_NAME)
    file.writeText(generatedContent)

    return file
  }

  override fun writeToFile(
      outputDir: File,
      rules: List<ProguardRule>,
      headerComment: String?,
  ): File {
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val content = generate(rules, headerComment)
    val file = File(outputDir, FILE_NAME)
    file.writeText(content)

    return file
  }
}

/** Builder class for creating ProguardRule instances with a fluent API. */
class ProguardRuleBuilder {
  private var rule: String = ""
  private var comment: String? = null

  fun rule(rule: String) = apply { this.rule = rule }

  fun comment(comment: String?) = apply { this.comment = comment }

  fun build(): ProguardRule {
    require(rule.isNotBlank()) { "ProGuard rule cannot be blank" }
    return ProguardRule(rule, comment)
  }
}

/** DSL function for creating ProguardRule instances. */
fun proguardRule(block: ProguardRuleBuilder.() -> Unit): ProguardRule {
  return ProguardRuleBuilder().apply(block).build()
}

/** Helper object with predefined common ProGuard rules. */
object ProguardRulesPresets {

  /** Default Android ProGuard rules. */
  val DEFAULT_ANDROID =
      """
          # Add project specific ProGuard rules here.
          # You can control the set of applied configuration files using the
          # proguardFiles setting in build.gradle.
          #
          # For more details, see
          #   http://developer.android.com/guide/developing/tools/proguard.html
          
          # If your project uses WebView with JS, uncomment the following
          # and specify the fully qualified class name to the JavaScript interface
          # class:
          #-keepclassmembers class fqcn.of.javascript.interface.for.webview {
          #   public *;
          #}
          
          # Uncomment this to preserve the line number information for
          # debugging stack traces.
          #-keepattributes SourceFile,LineNumberTable
          
          # If you keep the line number information, uncomment this to
          # hide the original source file name.
          #-renamesourcefileattribute SourceFile
      """
          .trimIndent()

  /** Keep all classes (no obfuscation). */
  val KEEP_ALL =
      """
          # Keep all classes
          -keep class ** { *; }
          -keepclassmembers class ** { *; }
      """
          .trimIndent()

  /** Standard rules for Retrofit. */
  val RETROFIT =
      """
          # Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
          # EnclosingMethod is required to use InnerClasses.
          -keepattributes Signature, InnerClasses, EnclosingMethod
          
          # Retrofit does reflection on method and parameter annotations.
          -keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
          
          # Keep annotation default values (e.g., retrofit2.http.Field.encoded).
          -keepattributes AnnotationDefault
          
          # Retain service method parameters when optimizing.
          -keepclassmembers,allowshrinking,allowobfuscation interface * {
              @retrofit2.http.* <methods>;
          }
          
          # Ignore annotation used for build tooling.
          -dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
          
          # Ignore JSR 305 annotations for embedding nullability information.
          -dontwarn javax.annotation.**
          
          # Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
          -dontwarn kotlin.Unit
          
          # Top-level functions that can only be used by Kotlin.
          -dontwarn retrofit2.KotlinExtensions
          -dontwarn retrofit2.KotlinExtensions$*
          
          # With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
          # and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
          -if interface * { @retrofit2.http.* <methods>; }
          -keep,allowobfuscation interface <1>
          
          # Keep inherited services.
          -if interface * { @retrofit2.http.* <methods>; }
          -keep,allowobfuscation interface * extends <1>
          
          # Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
          -keep,allowobfuscation,allowshrinking interface retrofit2.Call
          -keep,allowobfuscation,allowshrinking class retrofit2.Response
          
          # With R8 full mode generic signatures are stripped for classes that are not
          # kept. Suspend functions are wrapped in continuations where the type argument
          # is used.
          -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
      """
          .trimIndent()

  /** Standard rules for Gson. */
  val GSON =
      """
          # Gson uses generic type information stored in a class file when working with fields. Proguard
          # removes such information by default, so configure it to keep all of it.
          -keepattributes Signature
          
          # For using GSON @Expose annotation
          -keepattributes *Annotation*
          
          # Gson specific classes
          -dontwarn sun.misc.**
          
          # Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
          # JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
          -keep class * extends com.google.gson.TypeAdapter
          -keep class * implements com.google.gson.TypeAdapterFactory
          -keep class * implements com.google.gson.JsonSerializer
          -keep class * implements com.google.gson.JsonDeserializer
          
          # Prevent R8 from leaving Data object members always null
          -keepclassmembers,allowobfuscation class * {
            @com.google.gson.annotations.SerializedName <fields>;
          }
          
          # Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
          -keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
          -keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
      """
          .trimIndent()

  /** Standard rules for OkHttp. */
  val OKHTTP =
      """
          # JSR 305 annotations are for embedding nullability information.
          -dontwarn javax.annotation.**
          
          # A resource is loaded with a relative path so the package of this class must be preserved.
          -keeppackagenames okhttp3.internal.publicsuffix.*
          -adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz
          
          # Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
          -dontwarn org.codehaus.mojo.animal_sniffer.*
          
          # OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
          -dontwarn okhttp3.internal.platform.**
          -dontwarn org.conscrypt.**
          -dontwarn org.bouncycastle.**
          -dontwarn org.openjsse.**
      """
          .trimIndent()

  /** Rules to keep source file and line numbers for debugging. */
  val DEBUG_INFO =
      """
          # Keep source file and line numbers for better stack traces
          -keepattributes SourceFile,LineNumberTable
          -renamesourcefileattribute SourceFile
      """
          .trimIndent()
}
