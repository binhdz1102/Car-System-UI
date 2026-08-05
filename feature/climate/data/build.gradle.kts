import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("systemui.android.library")
    id("systemui.android.hilt")
}

android {
    namespace = "com.android.systemui.feature.climate.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:climate:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.dagger.hilt.android)
    implementation(libs.timber)

    // android.car is a platform API and is intentionally compileOnly. The matching
    // android.car.jar is produced/staged by My-System-App in this workspace.
    compileOnly(files(rootProject.file("../My-System-App/libs/platform/android.car.jar")))

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}

afterEvaluate {
    tasks.withType<JavaCompile>().configureEach {
        val carJar = rootProject.file("../My-System-App/libs/platform/android.car.jar")
        options.bootstrapClasspath = files(carJar)
        classpath = files(carJar) + classpath
    }
}
