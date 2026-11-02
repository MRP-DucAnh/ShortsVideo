plugins { alias(libs.plugins.android.library) }

android {
    namespace = "com.shorts.flowdrama.core.data"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
}
