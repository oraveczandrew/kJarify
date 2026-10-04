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

// Profiler: production parity run (assertions OFF, like `java -jar`).
// Usage: ./gradlew profiler
tasks.register<Test>("profiler") {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    enableAssertions = false
    filter { includeTestsMatching("hu.oandras.kJarify.ProfilerTest") }
    // JFR dump for analysis: ./gradlew profiler -Pjfr
    if (project.hasProperty("jfr")) {
        jvmArgs(
            "-XX:+FlightRecorder",
            "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=build/profiler.jfr,settings=profile",
        )
    }
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
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