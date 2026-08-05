plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.feature.notifications.presentation"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:notifications:domain"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.coroutines.android)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.material)
    implementation(libs.timber)
}
