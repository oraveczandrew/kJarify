import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.10"
    java
}

group = "hu.oandras.kJarify"
version = "1.0"

repositories.apply {
    mavenCentral()
    google()
}

dependencies.apply {
    testImplementation(kotlin("test"))
    implementation("commons-cli:commons-cli:1.9.0")
    implementation("androidx.collection:collection:1.4.5")

    val coroutinesVersion = "1.10.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
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
    jvmTarget.set(JvmTarget.JVM_1_8)
}

java.apply {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin.apply {
    jvmToolchain(21)
}