plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

group = "cz.parizmat.gitcraft"
version = "0.1.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Compose Desktop
    implementation("org.jetbrains.compose.desktop:desktop:1.12.0")
    runtimeOnly("org.jetbrains.compose.desktop:desktop-jvm-windows-x64:1.12.0")

    // Compose Resources
    implementation("org.jetbrains.compose.components:components-resources:1.12.0")

    // Material 3
    implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")

    // Icons
    implementation("io.github.lyxnx.compose.ui:tabler-icons-desktop:3.40.0")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.7.3")

    // Functional programming
    implementation("io.arrow-kt:arrow-core:2.1.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-compose:4.1.1")

    // Tests
    testImplementation(kotlin("test"))
}

compose.resources {
    packageOfResClass = "cz.parizmat.gitcraft.resources"
    generateResClass = always
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "cz.parizmat.gitcraft.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}