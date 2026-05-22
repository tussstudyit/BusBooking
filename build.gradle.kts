plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}

// Clean task
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
