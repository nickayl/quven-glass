import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

group = providers.gradleProperty("quvenGlass.group").get()
version = providers.gradleProperty("quvenGlass.version").get()

android {
    namespace = "tv.quven.glass"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation.core)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "glass"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
            pom {
                name = "Quven Glass"
                description = "A Liquid Glass material for Jetpack Compose."
                licenses {
                    license {
                        name = "MIT No Attribution"
                        url = "https://spdx.org/licenses/MIT-0.html"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "quven"
            url = uri(rootProject.file(providers.gradleProperty("quvenGlass.repository").get()))
        }
    }
}
