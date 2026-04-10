import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    // https://github.com/JetBrains/intellij-platform-gradle-plugin
    id("org.jetbrains.intellij.platform") version "2.6.0"
    // https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.2.1"
}

fun properties(key: String) = project.findProperty(key).toString()

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure Java compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Configure project's dependencies
repositories {
    mavenCentral()
    google()
    intellijPlatform { defaultRepositories() }
}

intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        group = properties("pluginGroup")
        changeNotes.set("Updated android studio compatibility to 2024.3.1.7")
        ideaVersion.sinceBuild.set(project.property("sinceBuild").toString())
        ideaVersion.untilBuild.set(provider { null })
    }
    buildSearchableOptions.set(false)
    instrumentCode = true
}

dependencies {
    intellijPlatform {
        bundledPlugin("org.jetbrains.android")
        if (project.hasProperty("localASVersion")) {
            local(property("localASVersion").toString())
        } else {
            androidStudio(property("platformVersion").toString())
        }
    }
    implementation(platform("io.projectreactor:reactor-bom:2024.0.0"))
    implementation("io.projectreactor.netty:reactor-netty-http:1.1.13")
    implementation("io.projectreactor.netty:reactor-netty-core:1.1.13")
    implementation("io.rsocket:rsocket-core:1.1.3")
    implementation("io.rsocket:rsocket-transport-netty:1.1.3")
    implementation("io.rsocket.broker:rsocket-broker-frames:0.3.0")
    implementation("org.jooq:joor-java-8:0.9.7")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okio:okio:3.4.0")
    testImplementation("junit:junit:4.13.1")
}

tasks {
    runIde {
        jvmArgs = listOf(
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false"
        )
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
