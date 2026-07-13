/**
 * Integration test configuration.
 *
 * The integration tests (`ITest*`) run against an already-running Riak node - for example
 * one started from a local devrel, or configured with the scripts under `riak-client-tools/`.
 * They are NOT part of `./gradlew build`. Start and configure Riak yourself before running
 * `./gradlew itest`.
 */
plugins {
    // For configuring a new test task
    `java-library`
}

// Optional toggles for backend-specific test groups (default off). Enable, for example,
// with -PriakTimeseries=true when running against a Riak TS node.
val riakTimeseries: String? by project
val riakTwoI: String? by project
val riakMapReduce: String? by project

/**
 * --------------------------------
 *          Integration Test Setup
 * --------------------------------
 */
val integrationTest = task<Test>("itest") {
    description = "Runs integration tests against an already-running Riak node."
    group = "verification"

    shouldRunAfter(tasks.test)

    useJUnit()

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    filter {
        //include all integration tests
        includeTestsMatching("ITest*")
    }

    val properties: Map<String, String> = mutableMapOf(
        "com.basho.riak.yokozuna" to "false", // Not testing yokozuna features
        "com.basho.riak.timeseries" to (riakTimeseries?.toBoolean() ?: false).toString(),
        "com.basho.riak.2i" to (riakTwoI?.toBoolean() ?: false).toString(), // backend must be 'leveldb' in riak config to use this
        "com.basho.riak.mapreduce" to (riakMapReduce?.toBoolean() ?: false).toString()
    )
    // Set test properties to use
    systemProperties(properties)

    testLogging {
        events("passed", "skipped", "failed")
    }
}
