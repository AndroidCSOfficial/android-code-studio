/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
*/
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tom.androidcodestudio.acsprovider"
    compileSdk = 36

}

dependencies {

    implementation(projects.core.common)
    
    // Coroutines
    implementation(libs.common.kotlin.coroutines.core)
    implementation(libs.common.kotlin.coroutines.android)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Gson for JSON parsing
    implementation(libs.gson)
    
    // SLF4J for logging
    implementation(libs.tooling.slf4j)
}
