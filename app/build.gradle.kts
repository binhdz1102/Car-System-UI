import org.gradle.api.tasks.Exec
import java.util.Properties

plugins {
    id("systemui.android.application")
    id("systemui.android.hilt")
}

// These standalone projects are intentionally distributable without a .git
// directory. Keep the field available without making Gradle depend on git.
val gitCommitHash = "standalone"

val platformKeystoreProperties =
    Properties().apply {
        rootProject.file("keystore.properties").inputStream().use(::load)
    }

android {
    namespace = "com.android.systemui"

    defaultConfig {
        applicationId = "com.android.systemui"
        // Higher than the Baklava image's versionCode (37), so adb install -r
        // can test this development replacement without -d.
        versionCode = 1000
        versionName = "custom-dev"
        buildConfigField("String", "GIT_COMMIT_HASH", "\"$gitCommitHash\"")
    }

    signingConfigs {
        create("platform") {
            storeFile =
                rootProject.file(
                    platformKeystoreProperties.getProperty("storeFile"),
                )
            storePassword = platformKeystoreProperties.getProperty("storePassword")
            keyAlias = platformKeystoreProperties.getProperty("keyAlias")
            keyPassword = platformKeystoreProperties.getProperty("keyPassword")
            storeType = platformKeystoreProperties.getProperty("storeType")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("platform")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("platform")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

// CarSystemUI is a persistent system package. Android's normal "Run" action
// (adb install -r) is intentionally not used for it; run this task on an AVD
// started with -writable-system instead.
tasks.register<Exec>("replaceSystemApp") {
    group = "deployment"
    description = "Pushes the signed CarSystemUI APK into /system_ext on a writable AVD"
    dependsOn("assembleDebug")

    doFirst {
        commandLine(
            "bash",
            rootProject.file("replace_system_app.sh").absolutePath,
            layout.buildDirectory
                .file("outputs/apk/debug/app-debug.apk")
                .get()
                .asFile
                .absolutePath,
        )
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:platform"))
    implementation(project(":feature:climate:data"))
    implementation(project(":feature:climate:domain"))
    implementation(project(":feature:climate:presentation"))
    implementation(project(":feature:launcher:data"))
    implementation(project(":feature:launcher:domain"))
    implementation(project(":feature:launcher:presentation"))
    implementation(project(":feature:notifications:data"))
    implementation(project(":feature:notifications:domain"))
    implementation(project(":feature:notifications:presentation"))
    implementation(project(":feature:systembar:data"))
    implementation(project(":feature:systembar:domain"))
    implementation(project(":feature:systembar:presentation"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
