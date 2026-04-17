// Dependencies of OkHttp 3.14.9's :okhttp module, transcribed from
//   https://github.com/square/okhttp/blob/parent-3.14.9/okhttp/build.gradle
//   https://github.com/square/okhttp/blob/parent-3.14.9/build.gradle (versions)
// This helper project exists only to dump the resolved jars into build/classpath.txt
// so our evaluator's CompileAnalyzer can hand them to kotlinc.

plugins {
    java
}

repositories {
    mavenCentral()
    google() // for com.google.android:android
}

dependencies {
    implementation("com.squareup.okio:okio:1.17.2")
    // compileOnly in OkHttp — still needed on classpath for references to resolve.
    implementation("com.google.android:android:4.1.1.4")
    implementation("org.bouncycastle:bcprov-jdk15on:1.64")
    implementation("org.bouncycastle:bctls-jdk15on:1.64")
    implementation("org.conscrypt:conscrypt-openjdk-uber:2.2.1")
    implementation("org.openjsse:openjsse:1.1.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.codehaus.mojo:animal-sniffer-annotations:1.18")
    implementation("org.jetbrains:annotations:17.0.0")
}

tasks.register("dumpClasspath") {
    val out = rootDir.resolve("classpath.txt")
    outputs.file(out)
    doLast {
        val entries = configurations.named("runtimeClasspath").get().files
            .map { it.absolutePath }
            .toSortedSet()
        out.writeText(entries.joinToString("\n") + "\n")
        logger.lifecycle("wrote ${entries.size} entries to $out")
    }
}
