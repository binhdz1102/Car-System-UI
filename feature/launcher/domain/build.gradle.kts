plugins {
    id("systemui.android.library")
}

android {
    namespace = "com.android.systemui.feature.launcher.domain"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.dagger.hilt.android)
    implementation(libs.coroutines.core)
}
