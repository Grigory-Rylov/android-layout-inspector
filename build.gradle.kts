import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    // https://github.com/JetBrains/intellij-platform-gradle-plugin
    id("org.jetbrains.intellij.platform") version "2.10.5"
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

// Android Studio's App Inspection classes (com.android.tools.idea.appinspection.*) live in
// <android-studio>/plugins/android/lib/android.jar. They are visible to YALI at runtime through
// the org.jetbrains.android plugin dependency, but the module is not exported to the plugin
// compile classpath, so typed imports would not resolve without the extra compileOnly entry below.
// The intellijPlatform AS artifact is unpacked by Gradle into
// ~/.gradle/caches/<gradle-version>/transforms/<hash>/transformed/android-studio-<platformVersion>*/;
// we locate that already-unpacked distribution and point compileOnly at its android.jar.
// The lookup is defensive: it is deferred to classpath-materialization time and yields an empty
// file collection when the AS distribution cannot be found, so the build itself never crashes here.
fun findAndroidStudioAndroidJar(): File? {
    val version = properties("platformVersion")
    val transformsDir = File(gradle.gradleUserHomeDir, "caches")
        .listFiles { f -> f.isDirectory && f.name.matches(Regex("\\d[\\d.]*")) }
        .orEmpty()
        .map { File(it, "transforms") }
        .filter { it.isDirectory }
    val candidates = transformsDir.flatMap { dir ->
        dir.listFiles { f -> f.isDirectory }.orEmpty().flatMap { hashDir ->
            File(hashDir, "transformed").listFiles { f ->
                f.isDirectory && f.name.startsWith("android-studio")
            }.orEmpty().map { asRoot -> File(asRoot, "plugins/android/lib/android.jar") }
        }
    }.filter { it.isFile }
    return candidates.firstOrNull { it.absolutePath.contains("android-studio-$version") }
        ?: candidates.firstOrNull()
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
    implementation("com.google.protobuf:protobuf-java:3.25.5")
    // See findAndroidStudioAndroidJar() above: compile-time access to AS App Inspection classes.
    compileOnly(files(provider { listOfNotNull(findAndroidStudioAndroidJar()) }))
    testImplementation("junit:junit:4.13.1")
}

tasks {
    runIde {
        jvmArgs = listOf(
            "-Dide.mac.message.dialogs.as.sheets=false",
            "-Djb.privacy.policy.text=<!--999.999-->",
            "-Djb.consents.confirmation.enabled=false"
        )
        // Compose self-test harness: `./gradlew runIde -Pyali.selftest=/tmp/yali-selftest.json`.
        // -D on the Gradle CLI does not propagate to the launched IDE JVM, so forward explicitly
        // (RunIdeTask extends JavaExec, so jvmArgs is available on this receiver).
        (project.findProperty("yali.selftest") as String?)?.let {
            jvmArgs("-Dyali.compose.selftest=$it")
            // ProjectActivity only runs when a project is open; a fresh sandbox stops at the
            // Welcome frame, so self-test mode opens a throwaway (empty) directory instead.
            jvmArgs("-Didea.trust.all.projects=true")
            val probeDir = layout.buildDirectory.dir("yali-selftest-project").get().asFile
            doFirst { probeDir.mkdirs() }
            args = listOf(probeDir.absolutePath)
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
