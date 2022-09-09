import org.ajoberstar.reckon.gradle.ReckonExtension
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    // Needed for some dependency stuff (i.e. resolving jar dependencies with java-platform etc)
    id("dependency-conventions")

    id("org.ajoberstar.reckon")

    id("nebula.ivy-publish")

    // Apply the java-library plugin for API and implementation separation.
    // https://docs.gradle.org/current/userguide/java_library_plugin.html
    `java-library`
}

val artifactory_user: String? by project
val artifactory_password: String? by project

repositories {
    maven("https://artifactory.internal.invalid/artifactory/virtexperimental") {
        credentials {
            username = artifactory_user
            password = artifactory_password
        }
    }
}

/**
 * Configuring Reckon (used for semantic versioning)
 * To push the latest tag use:  ./gradlew reckonTagPush -Preckon.stage=final
 * final means the version will be in the form 0.2.0 not 0.2.0-alpha.0.1+20210304T132206Z
 */
configure<ReckonExtension> {
    scopeFromProp()
    stageFromProp("alpha", "beta", "final")
}

/**
 * Simple task to log the reckoned version
 */
tasks.register("version") {
    doLast {
        logger.lifecycle("Project version is: ${project.version}")
    }
}

allprojects {
    apply {
        plugin("idea")
    }
}

dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda")
    implementation("io.netty:netty-all")
    implementation("org.erlang.otp:jinterface")

    //Test dependencies

    testImplementation("org.mockito:mockito-core")
    testImplementation("org.powermock:powermock-api-mockito")
    testImplementation("org.powermock:powermock-module-junit4")
    testImplementation("org.hamcrest:hamcrest-core")
    testImplementation("com.jayway.awaitility:awaitility")
}

/**
 * ------------------------------------------------------------
 *                        Artifact Info
 * ------------------------------------------------------------
 */

val cleanedVersion = project.version.toString().replace("+", "-")

tasks.jar.configure {
    manifest {
        attributes("Built-By" to  System.getProperty("user.name"),
                "Build-Timestamp" to (SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())),
                "Gradle-Version" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})",
                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                "Implementation-Vendor" to "Workday",
                "Implementation-Version" to version
        )
    }
}

afterEvaluate {
    publishing {
        publications {
            create<IvyPublication>("jar") {
                from(components["java"])
            }
        }
    }
}


gradle.buildFinished {
    logger.lifecycle("\n------------------------------------------------------------\n")
    logger.lifecycle("VERSION: $cleanedVersion")
    logger.lifecycle("\n------------------------------------------------------------\n")
}
