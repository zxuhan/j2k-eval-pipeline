// Clikt CLI, kotlin-compiler-embeddable, JUnit 5 + Kotest arrive in commit 4.
// Stub keeps the build graph wireable.

plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.zxuhan.j2k.eval.MainKt")
}
