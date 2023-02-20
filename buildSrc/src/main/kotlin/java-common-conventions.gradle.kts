import java.text.SimpleDateFormat
import java.util.*

/**
 * Config for a java library
 */
plugins {
    `java-library`
}


/** -----------------------------------------------
 *                  Java Configuration
 *  -----------------------------------------------
 */
java {
    // Explicitly set the target and source compatability to 8
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))

    // Include a sources jar
    withSourcesJar()

    consistentResolution {
        // Avoids compile and runtime using different versions
        // https://docs.gradle.org/6.8.3/userguide/resolution_strategy_tuning.html#resolution_consistency
        useRuntimeClasspathVersions()
    }

    // Configure the manifest of the jar
    manifest {
        attributes(
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to (SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())),
                "Gradle-Version" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})",
                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                "Implementation-Vendor" to "Workday",
                "Implementation-Version" to project.version
        )
    }
}

/** -----------------------------------------------
 *                  Test Configuration
 *  -----------------------------------------------
 */
// Unit tests
tasks.test {
    useJUnit()

    testLogging {
        events("passed", "skipped", "failed")
    }

    filter {
        //exclude all integration tests
        excludeTestsMatching("ITest*")
    }
}

/** -----------------------------------------------
 *                  Dependency Configuration
 *  -----------------------------------------------
 */
/**
 * This misdirection is to avoid the boms we use to control versions ending up in the ivy pom
 * see more: https://github.com/gradle/gradle/issues/10861#issuecomment-576562961
 */
val internal by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = false
}

configurations {
    compileClasspath {
        extendsFrom(internal)
        resolutionStrategy.activateDependencyLocking()
    }
    runtimeClasspath {
        extendsFrom(internal)
        resolutionStrategy.activateDependencyLocking()
    }
    testCompileClasspath {
        extendsFrom(internal)
        resolutionStrategy.activateDependencyLocking()
    }
    testRuntimeClasspath {
        extendsFrom(internal)
        resolutionStrategy.activateDependencyLocking()
    }
    annotationProcessor {
        extendsFrom(internal)
        resolutionStrategy.activateDependencyLocking()
    }
    default {
        extendsFrom(internal)
        resolutionStrategy.activateDependencyLocking()
    }
}

dependencies {
    // Use version constraints from dependencies module.
    internal(platform(project(":dependencies")))

    // Use the provided jackson bom to keep versions in sync
    internal(platform("com.fasterxml.jackson:jackson-bom"))

    // Use the provided netty bom to keep versions in sync
    internal(platform("io.netty:netty-bom"))

    testImplementation("junit:junit")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}