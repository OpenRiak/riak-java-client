import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Support convention plugins written in Kotlin.
    // Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "8"
    }
}


dependencies {
    // Gradle packages a version of kotlin that is 1.6.+ so match that to avoid pulling in later versions (will need to update when gradle updates)
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.5.+"))

    /**
     * Needed for xray-scanning
     * Not entirely sure why needed to explicitly add xray-java, without it can't import com.workday.be.xray.Severity
     */
    implementation(plugin("wd-xray", "0.+"))
    implementation("com.workday.be.xray:xray-java:0.+")

    // Needed for handling the results from xray scans (aka jiras slack etc)
    implementation(plugin("xray-jira-automator", "0.+"))


    implementation("com.squareup.okhttp3:okhttp:4.+") {
        because("Needed for Http Requests in gradle scripts")
    }

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.+") {
        because("Needed json serializing/deserializing")
    }

    implementation("javax.activation:javax.activation-api:1.2.0") {
        because("Some plugins were built for java 8 so need to add this for compatablity ")
    }

    /**
     * Needed to manage semantic versioning
     */
    implementation(plugin("org.ajoberstar.reckon", "0.13.+"))

    implementation(plugin("org.scoverage", "7.+"))

    /**
     * Needed for version scanning
     */
    implementation(plugin("com.github.ben-manes.versions", "0.+"))

    /**
     * Artifactory/publishing plugins
     */
    implementation("com.workday.gradle.artifactory:wd-artifactory:3.+")

    /**
     * Needed to communicate with beer/console apis
     */
    implementation("com.workday.gradle.esb:gradle-esb-beer:4.+")

    /**
     * Slack integration
     * Stuck on 1.22.1 as 1.22.2 switches to kotlin version 1.7 (will need gradle updates)
     */
    implementation("com.slack.api:slack-api-client:1.22.1")
    implementation("com.slack.api:slack-api-model-kotlin-extension:1.22.1")
    implementation("com.slack.api:slack-api-client-kotlin-extension:1.22.1")

    /**
     * Git Integration
     */
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.+")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.+")

    /**
     * Needed for jar publishing
     */
    implementation(plugin("wd-publish-artifactory", "1.+"))
    implementation(plugin("nebula.ivy-publish", "5.+"))
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