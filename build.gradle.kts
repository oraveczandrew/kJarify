import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
    java
    jacoco
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

// JitPack passes -Pgroup/-Pversion; keep local defaults when flags are absent.
group = providers.gradleProperty("group").getOrElse("hu.oandras.kJarify")
version = providers.gradleProperty("version").getOrElse("1.1.0")

repositories.apply {
    mavenCentral()
    google()
}

dependencies.apply {
    testImplementation(kotlin("test"))
    implementation("commons-cli:commons-cli:1.11.0")
    implementation("androidx.collection:collection:1.6.0")

    val coroutinesVersion = "1.11.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.4")
}

tasks.register<Jar>("fatJar") {
    description = "Assembles a self-contained fat jar with Main-Class hu.oandras.kJarify.MainKt."
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

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Profiler: production parity run (assertions OFF, like `java -jar`).
// Usage: ./gradlew profiler
tasks.register<Test>("profiler") {
    description = "Runs ProfilerTest with production parity (assertions OFF, no JaCoCo)."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    enableAssertions = false
    // Coverage instrumentation would distort profiler numbers
    the<JacocoTaskExtension>().isEnabled = false
    // test2 needs GBs of retained classes per run; -Pheap=8g to override
    maxHeapSize = (project.findProperty("heap") as String?) ?: "12g"
    filter { includeTestsMatching("hu.oandras.kJarify.ProfilerTest") }
    // passthrough: ./gradlew profiler -Pinput=test2-base.apk -Pruns=2
    // (Gradle -P props don't reach the test JVM as -D automatically)
    for (key in listOf("input", "runs", "warmup", "threads")) {
        val value: String? = project.findProperty("profiler.$key") as String?
            ?: project.findProperty(key) as String?
        if (value != null) {
            systemProperty("profiler.$key", value)
        }
    }
    // GC choice: ./gradlew profiler -Pgc=parallel (default: G1)
    when ((project.findProperty("gc") as String?)?.lowercase()) {
        "parallel" -> jvmArgs("-XX:+UseParallelGC")
        "z" -> jvmArgs("-XX:+UseZGC")
        "serial" -> jvmArgs("-XX:+UseSerialGC")
    }
    if (project.hasProperty("jfr")) {
        jvmArgs(
            "-XX:+FlightRecorder",
            "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=build/profiler.jfr,settings=profile",
        )
    }
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
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