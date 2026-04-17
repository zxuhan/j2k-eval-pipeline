// IntelliJ Platform plugin wiring arrives in commit 2 (NJ2K via j2kConverterExtension).
// This stub is just enough for `./gradlew projects` and `./gradlew build` to pass.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}
