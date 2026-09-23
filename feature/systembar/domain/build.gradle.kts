plugins {
    id("systemui.android.library")
}

android {
    namespace = "com.android.systemui.feature.systembar.domain"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:climate:domain"))
    implementation(project(":feature:notifications:domain"))
    implementation(libs.dagger.hilt.android)
    implementation(libs.coroutines.core)
}
