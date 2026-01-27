import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerExecContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import org.gradle.kotlin.dsl.support.serviceOf


/**
 * Config for a java library
 */
plugins {
    // For configuring a new test task
    `java-library`
    // For spinning up the riak docker container
    id("com.bmuschko.docker-remote-api")
}

/**
 * --------------------------------
 *          Configuration
 * --------------------------------
 */

val baseImage = "workday-riak-centos9"
val riakImage = "docker-dev-artifactory.internal.invalid/dssst/$baseImage:latest"

/**
 * --------------------------------
 *          Docker Setup
 * --------------------------------
 */
val pullImage by tasks.creating(DockerPullImage::class) {
    image.set(riakImage)
}

val createContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(pullImage)

    containerName.set(baseImage)
    targetImageId(pullImage.image)

    hostConfig.portBindings.set(listOf("8087:8087", "8098:8098"))
    hostConfig.autoRemove.set(true)

    // In case mounting is required:
    // hostConfig.binds.set(listOf(
    //     "/tmp/workday-riak-tests/riak1/lib:/var/lib/riak",
    //     "/tmp/workday-riak-tests/riak1/log:/var/log/riak"
    // ))
}



val startContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createContainer)

    targetContainerId(createContainer.containerId)
}

val waitForRiak by tasks.creating(DockerExecContainer::class) {
    dependsOn(startContainer)

    targetContainerId(startContainer.containerId)

    // We won't use the standard command for testing health
    // because we need to loop.
    // We use this just to satisfy the type requirements.
    commands.set(listOf(arrayOf("true")))
    doLast {
        var attempts = 0
        val maxAttempts = 20
        var success = false

        logger.lifecycle("Waiting for Riak to become reachable...")
        val execOps = project.serviceOf<ExecOperations>()
        while (attempts < maxAttempts && !success) {
            val result = execOps.exec {
                commandLine("docker", "exec", baseImage, "riak", "ping")
                isIgnoreExitValue = true
                standardOutput = System.out
            }

            if (result.exitValue == 0) {
                success = true
                logger.lifecycle("Riak is up!")
            } else {
                attempts++
                logger.lifecycle("Riak not ready yet (Attempt $attempts/$maxAttempts)... waiting 2s")
                Thread.sleep(2000)
            }
        }

        if (!success) {
            logger.error("Timeout waiting for Riak. Fetching logs...")
            execOps.exec { commandLine("docker", "logs", baseImage) }
            throw GradleException("Riak failed to start within timeout.")
        }
    }
}

// In order to run with devrels -- comment out Docker tasks above.

// val configureRiak by tasks.creating(Exec::class) {
//     executable = "bash"
//     commandLine(
//         "./riak-client-tools/riak-cluster-config",
//         "<path/to/devrel/bin/riak> admin",
//         10028,
//         false,
//         false,
//         "riak-client-tools/bucket-types"
//     )
// }

val configureRiak by tasks.creating(Exec::class) {
    dependsOn(waitForRiak)

    executable = "bash"
    commandLine(
        "./riak-client-tools/riak-cluster-config",
        "docker exec $baseImage riak admin",
        8098,
        false,
        false,
        "riak-client-tools/bucket-types"
    )
}

val stopContainer by tasks.creating(DockerStopContainer::class) {
    targetContainerId(createContainer.containerId)
}


/**
 * --------------------------------
 *          Integration Test Setup
 * --------------------------------
 */
val integrationTest = task<Test>("itest") {
    description = "Runs integration tests."
    group = "verification"

    shouldRunAfter(tasks.test)
    dependsOn(configureRiak)
    finalizedBy(stopContainer)

    useJUnit()

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath


    filter {
        //include all integration tests
        includeTestsMatching("ITest*")
    }

    val properties: Map<String, String> = mutableMapOf(
        "com.basho.riak.yokozuna" to "false", // Not testing yokuzuna features
        "com.basho.riak.timeseries" to (baseImage == "riak-ts").toString(),
        "com.basho.riak.2i" to (baseImage == "riak-ts").toString(), // backend must be 'leveldb' in riak config to us this
        "com.basho.riak.mapreduce" to (baseImage == "riak-ts").toString()
    )
    // Set test properties to use
    systemProperties(properties)

    testLogging {
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
