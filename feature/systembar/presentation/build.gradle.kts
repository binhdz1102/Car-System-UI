plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.feature.systembar.presentation"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:platform"))
    implementation(project(":feature:climate:domain"))
    implementation(project(":feature:climate:presentation"))
    implementation(project(":feature:notifications:domain"))
    implementation(project(":feature:notifications:presentation"))
    implementation(project(":feature:systembar:domain"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.material)
    implementation(libs.timber)
}
