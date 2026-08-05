plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.feature.launcher.presentation"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:launcher:domain"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.coroutines.android)
    implementation(libs.dagger.hilt.android)
    implementation(libs.timber)

    compileOnly(files(rootProject.file("../My-System-App/libs/platform/android.car.jar")))
}
