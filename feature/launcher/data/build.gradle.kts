plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.feature.launcher.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:launcher:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.dagger.hilt.android)
    implementation(libs.timber)
}
