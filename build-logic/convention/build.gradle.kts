plugins {
    `kotlin-dsl`
}

group = "com.android.systemui.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.hilt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.androidx.room.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "systemui.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "systemui.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "systemui.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "systemui.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidNavigationCompose") {
            id = "systemui.android.navigation.compose"
            implementationClass = "AndroidNavigationComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "systemui.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidKtlint") {
            id = "systemui.android.ktlint"
            implementationClass = "AndroidKtlintConventionPlugin"
        }
        register("androidDetekt") {
            id = "systemui.android.detekt"
            implementationClass = "AndroidDetektConventionPlugin"
        }
        register("androidJacoco") {
            id = "systemui.android.jacoco"
            implementationClass = "AndroidJacocoConventionPlugin"
        }
        register("androidRoom") {
            id = "systemui.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("rootKtlint") {
            id = "systemui.root.ktlint"
            implementationClass = "RootKtlintConventionPlugin"
        }
        register("rootDetekt") {
            id = "systemui.root.detekt"
            implementationClass = "RootDetektConventionPlugin"
        }
        register("rootJacoco") {
            id = "systemui.root.jacoco"
            implementationClass = "RootJacocoConventionPlugin"
        }
    }
}
