import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Support convention plugins written in Kotlin.
    // Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

// The toolchain used by gradle is 11 as plugins are built with that jvm in mind
// Note we can still build a jar that targets 8 while using the 11 JDK
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    // Gradle packages a version of kotlin that is 1.7.+ so match that to avoid pulling in later versions (will need to update when gradle updates)
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.7.+"))

    // For performing http requests
    implementation("com.squareup.okhttp3:okhttp:4.+") {
        because("Needed for Http Requests in gradle scripts")
    }

    // For parsing responses
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.+") {
        because("Needed json serializing/deserializing")
    }

    // For protobuf generation from .proto files
    implementation(plugin("com.google.protobuf", version = "0.+"))

    implementation("com.bmuschko:gradle-docker-plugin:9.+")
}

dependencyLocking {
    lockAllConfigurations()
}

/**
 * Helper that handles converting from plugin syntax i.e. id("plugin-name") to an implementation dependency which
 * typically looks like "plugin-name:plugin-name.gradle.plugin:version"
 */
fun plugin(plugin: String, version: String): String = "$plugin:${plugin}.gradle.plugin:$version"


tasks.register("resolveAndLockAll") {
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
        configurations.filter {
            // Add any custom filtering on the configurations to be resolved
            it.isCanBeResolved && !it.name.startsWith("incrementalScalaAnalysis")
        }.forEach { it.resolve() }
    }
}