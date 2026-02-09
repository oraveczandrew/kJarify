import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.10"
    java
}

group = "hu.oandras.kJarify"
version = "1.0.2"

repositories.apply {
    mavenCentral()
    google()
}

dependencies.apply {
    testImplementation(kotlin("test"))
    implementation("commons-cli:commons-cli:1.11.0")
    implementation("androidx.collection:collection:1.5.0")

    val coroutinesVersion = "1.10.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.2")
}

tasks.register<Jar>("fatJar") {
    archiveBaseName = "kJarify-fat"

    manifest.apply {
        attributes["Main-Class"] = "hu.oandras.kJarify.MainKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) { it } else zipTree(it)
        }
    )

    with(tasks["jar"] as CopySpec)
}

tasks.test {
    useJUnitPlatform()
}

kotlin.compilerOptions.apply {
    jvmTarget.set(JvmTarget.JVM_11)
}

java.apply {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin.apply {
    jvmToolchain(21)
}