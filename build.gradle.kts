import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    // Gradle IntelliJ Platform Plugin
    id("org.jetbrains.intellij.platform") version "2.6.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // Protobuf support
    id("com.google.protobuf") version "0.9.4"
}

fun properties(key: String) = project.findProperty(key).toString()

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure Java compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure project's dependencies
repositories {
    mavenCentral()
    google()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        androidStudio(properties("platformVersion"))
        bundledPlugin("org.jetbrains.android")
    }
    // https://mvnrepository.com/artifact/com.android.tools.ddms/ddmlib
    implementation("com.android.tools.ddms:ddmlib:31.10.1")
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
    testImplementation("junit:junit:4.12")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {
                    option("lite")
                }
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("proto")
        }
    }
}

tasks {
    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")
        changeNotes = """
            Support Android Studio Ladybug.<br>
            Improved tree renderer.<br>
            Fixed uiautomator dump.<br>
        """
    }

    runIde {
        jvmArgs = listOf(
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false"
        )
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
