// Top-level build file. Plugins are declared here (not applied) so every module resolves them
// from a single classloader and version. AGP 9 compiles Kotlin itself ("built-in Kotlin") for
// Android modules, so org.jetbrains.kotlin.android is not used.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
