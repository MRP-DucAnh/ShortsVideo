import com.android.build.api.artifact.SingleArtifact
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.shorts.flowdrama"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.shorts.flowdrama"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
    // Language is selected in-app, so every installation needs all translations.
    bundle {
        language { enableSplit = false }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:player"))
    implementation(project(":core:ads"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:reels"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:playback"))
    implementation(project(":feature:library"))
    implementation(project(":feature:settings"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.serialization.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ValueSource is rechecked when reusing the configuration cache, including across midnight.
abstract class BuildDateSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
        .format(DateTimeFormatter.BASIC_ISO_DATE)
}

@CacheableTask
abstract class NameBundle : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputBundle: RegularFileProperty

    @get:OutputFile
    abstract val outputBundle: RegularFileProperty

    @TaskAction
    fun copyBundle() {
        val target = outputBundle.get().asFile
        target.parentFile.mkdirs()
        inputBundle.get().asFile.copyTo(target, overwrite = true)
    }
}

val artifactBuildDate = providers.of(BuildDateSource::class) {}.get()
androidComponents {
    onVariants(selector().all()) { variant ->
        val mainOutput = variant.outputs.first()
        val artifactName = "FlowDrama-v${mainOutput.versionName.get()}-b${mainOutput.versionCode.get()}-${variant.name}-$artifactBuildDate"
        variant.outputs.forEach { output ->
            val split = output.filters.joinToString("-") { it.identifier }
            output.outputFileName.set("$artifactName${if (split.isEmpty()) "" else "-$split"}.apk")
        }
        val nameBundle = tasks.register<NameBundle>("name${variant.name.replaceFirstChar { it.uppercaseChar() }}Bundle")
        variant.artifacts.use(nameBundle)
            .wiredWithFiles(NameBundle::inputBundle, NameBundle::outputBundle)
            .withName("$artifactName.aab")
            .toTransform(SingleArtifact.BUNDLE)
    }
}
