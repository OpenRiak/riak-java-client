configurations {
    all {
        resolutionStrategy {
            componentSelection {
                // Reject versions containing "rc", "alpha", "beta" etc
                all {
                    val rejected = candidate.version.contains("-rc", ignoreCase = true) ||
                            candidate.version.contains("alpha", ignoreCase = true) ||
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