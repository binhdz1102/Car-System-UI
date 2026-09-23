plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.core.platform"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.core.ktx)
}
