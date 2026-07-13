import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Support convention plugins written in Kotlin.
    // Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

// Resolve plugins and dependencies from public repositories so the build works
// off any internal network.
repositories {
    gradlePluginPortal()
    mavenCentral()
}

// The toolchain used by gradle is 11 as plugins are built with that jvm in mind
// Note we can still build a jar that targets 8 while using the 11 JDK
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}


dependencies {
    // Gradle packages a version of kotlin that is 2.2.+ so match that to avoid pulling in later versions (will need to update when gradle updates)
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:2.2.+"))

    // For protobuf generation from .proto files
    implementation(plugin("com.google.protobuf", version = "0.+"))
}

/**
 * Helper that handles converting from plugin syntax i.e. id("plugin-name") to an implementation dependency which
 * typically looks like "plugin-name:plugin-name.gradle.plugin:version"
 */
fun plugin(plugin: String, version: String): String = "$plugin:${plugin}.gradle.plugin:$version"
