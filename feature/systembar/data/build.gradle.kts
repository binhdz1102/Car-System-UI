plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.feature.systembar.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:notifications:domain"))
    implementation(project(":feature:systembar:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.dagger.hilt.android)
    implementation(libs.timber)
}
