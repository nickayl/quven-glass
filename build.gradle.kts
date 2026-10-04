plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// A run that outlives this is hung, not slow.
subprojects {
    tasks.withType<Test>().configureEach {
        timeout.set(java.time.Duration.ofSeconds(90))
    }
}
