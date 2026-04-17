import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2.6")
        bundledPlugin("org.jetbrains.kotlin")
    }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.zxuhan.j2k.runner"
        name = "j2k batch runner"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }
    buildSearchableOptions = false
    instrumentCode = false
}

tasks.runIde {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf(
            "-Djava.awt.headless=true",
            "-Didea.config.path=${layout.buildDirectory.dir("idea-sandbox/config").get().asFile.absolutePath}",
            "-Didea.system.path=${layout.buildDirectory.dir("idea-sandbox/system").get().asFile.absolutePath}",
            "-Didea.log.path=${layout.buildDirectory.dir("idea-sandbox/log").get().asFile.absolutePath}",
        )
    }
}
