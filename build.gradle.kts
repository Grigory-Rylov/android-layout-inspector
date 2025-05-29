plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    // Gradle IntelliJ Platform Plugin
    id("org.jetbrains.intellij.platform") version "2.6.0"
    // Migration helper plugin
    id("org.jetbrains.intellij.platform.migration") version "2.6.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.2.0"
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
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    // Plugin configuration
    pluginConfiguration {
        name.set(properties("pluginName"))
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
    }

    buildSearchableOptions {
        enabled = false
    }
}

dependencies {
    implementation(platform("io.projectreactor:reactor-bom:2024.0.0"))
    implementation("io.rsocket:rsocket-core:1.1.3")
    implementation("io.rsocket:rsocket-transport-netty:1.1.3")
    implementation("io.rsocket.broker:rsocket-broker-frames:0.3.0")

    implementation("org.jooq:joor-java-8:0.9.7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Protobuf dependencies
    implementation("com.google.protobuf:protobuf-java:3.25.1")
    
    // DDMLIB dependency
    implementation("com.android.tools.ddms:ddmlib:31.2.0")

    // IntelliJ Platform dependencies
    intellijPlatform {
        // Use Android Studio as the target platform
        create("AI", properties("platformVersion"))
        
        // Add required plugins
        plugins(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    testImplementation("junit:junit:4.13.2")
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
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        changeNotes.set(
            """
            Support Android Studio Ladybug.<br>
            Improved tree renderer.<br>
            Fixed uiautomator dump.<br>
          """
        )
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    register<Test>("runIdeForUiTests") {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    runIde {
        // To debug with preview use path: "/Applications/Android Studio Preview.app/Contents"
        ideDir.set(file("/Applications/Android Studio.app/Contents"))
    }
}
