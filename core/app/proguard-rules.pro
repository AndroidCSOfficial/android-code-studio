-ignorewarnings

-dontwarn **
-dontnote **
-dontobfuscate

-keep class javax.** { *; }
-keep class jdkx.** { *; }

# keep javac classes
-keep class openjdk.** { *; }

# Android builder model interfaces
-keep class com.android.** { *; }

# Tooling API classes
-keep class com.tom.rv2ide.tooling.** { *; }

# Builder model implementations
-keep class com.tom.rv2ide.builder.model.** { *; }

# Eclipse
-keep class org.eclipse.** { *; }

# JAXP
-keep class jaxp.** { *; }
-keep class org.w3c.** { *; }
-keep class org.xml.** { *; }

# Services
-keep @com.google.auto.service.AutoService class ** {
}
-keepclassmembers class ** {
    @com.google.auto.service.AutoService <methods>;
}

# EventBus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Accessed reflectively
-keep class io.github.rosemoe.sora.widget.component.EditorAutoCompletion {
    io.github.rosemoe.sora.widget.component.EditorCompletionAdapter adapter;
    int currentSelection;
}
-keep class com.tom.rv2ide.projects.util.StringSearch {
    packageName(java.nio.file.Path);
}
-keep class * implements org.antlr.v4.runtime.Lexer {
    <init>(...);
}
-keep class * extends com.tom.rv2ide.lsp.java.providers.completion.IJavaCompletionProvider {
    <init>(...);
}
-keep class com.tom.rv2ide.editor.api.IEditor { *; }
-keep class * extends com.tom.rv2ide.inflater.IViewAdapter { *; }
-keep class * extends com.tom.rv2ide.inflater.drawable.IDrawableParser {
    <init>(...);
    android.graphics.drawable.Drawable parse();
    android.graphics.drawable.Drawable parseDrawable();
}
-keep class com.tom.rv2ide.utils.DialogUtils {  public <methods>; }

# APK Metadata
-keep class com.tom.rv2ide.models.ApkMetadata { *; }
-keep class com.tom.rv2ide.models.ArtifactType { *; }
-keep class com.tom.rv2ide.models.MetadataElement { *; }

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# Used in preferences
-keep enum org.eclipse.lemminx.dom.builder.EmptyElements { *; }
-keep enum com.tom.rv2ide.xml.permissions.Permission { *; }

# Lots of native methods in tree-sitter
# There are some fields as well that are accessed from native field
-keepclasseswithmembers class ** {
    native <methods>;
}

-keep class com.tom.rv2ide.treesitter.** { *; }

# Retrofit 2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp3
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Stat uploader
-keep class com.tom.rv2ide.stats.** { *; }

# Gson
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

## Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

## Themes
-keep enum com.tom.rv2ide.ui.themes.IDETheme {
  *;
}

## Contributor models - deserialized with GSON
-keep class * implements com.tom.rv2ide.contributors.Contributor {
  *;
}

# Suppress wissing class warnings
## These are used in annotation processing process in the Java Compiler
-dontwarn sun.reflect.annotation.AnnotationParser
-dontwarn sun.reflect.annotation.AnnotationType
-dontwarn sun.reflect.annotation.EnumConstantNotPresentExceptionProxy
-dontwarn sun.reflect.annotation.ExceptionProxy

## Used in Logback. We do not need this though.
-dontwarn jakarta.servlet.ServletContainerInitializer

## These are used in JGit
## TODO(itsaky): Verify if it is safe to ignore these warnings
-dontwarn java.lang.ProcessHandle
-dontwarn java.lang.management.ManagementFactory
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid