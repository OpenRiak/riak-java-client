 /**
 * Conventions for anything the needs to use dependencies
 * Ensures that the constraints set by the platforms are used (i.e. the 'dependencies' module and any platform boms)
 */

plugins {
    // Need this plugin for "implementation" + dependency management
    java
    // Scan every module independently
    id("dependency-scanning")
}

java {
    consistentResolution {
        // Avoids compile and runtime using different versions
        // https://docs.gradle.org/6.8.3/userguide/resolution_strategy_tuning.html#resolution_consistency
        useRuntimeClasspathVersions()
    }
}

configurations {
    all {
        resolutionStrategy {
            componentSelection {
                // Reject versions containing "rc", "alpha", "beta" etc
                all {
                    val rejected = candidate.version.contains("-rc", ignoreCase = true) ||
                            candidate.version.contains("-alpha", ignoreCase = true) ||
                            candidate.version.contains("-beta", ignoreCase = true) ||
                            candidate.version.contains("snapshot", ignoreCase = true)

                    if (rejected) {
                        logger.info("Rejecting version ${candidate.version} of ${candidate.displayName}")
                        reject("Rejecting version ${candidate.version} of ${candidate.displayName}")
                    }
                }
            }
        }
    }
}

dependencies {
    // Use version constraints from dependencies module.
    implementation(platform(project(":dependencies")))
}

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