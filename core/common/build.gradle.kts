plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.core.common"
}

dependencies {
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
