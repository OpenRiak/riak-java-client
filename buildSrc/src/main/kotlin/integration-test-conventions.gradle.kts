import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerExecContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage


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
val riakImage = "$baseImage:3.2.34-centos9"

/**
 * --------------------------------
 *          Docker Setup
 * --------------------------------
 */
 
//val pullImage by tasks.creating(DockerPullImage::class) {
//    image.set(riakImage)
//}

val createContainer by tasks.creating(DockerCreateContainer::class) {
    //dependsOn(pullImage)

    containerName.set(baseImage)
    targetImageId("30de46514b1f")

    hostConfig.portBindings.set(listOf("8087:8087", "8098:8098"))
    hostConfig.autoRemove.set(true)
}

val startContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createContainer)

    targetContainerId(createContainer.containerId)
}

val waitForRiak by tasks.creating(DockerExecContainer::class) {
    dependsOn(startContainer)

    targetContainerId(startContainer.containerId)
    commands.set(listOf(
            arrayOf("riak", "ping")
    ))
}

val configureRiak by tasks.creating(Exec::class) {
    dependsOn(waitForRiak)

    executable = "bash"
    commandLine("./riak_tools/riak-cluster-config", "docker exec $baseImage riak admin", 8098, false, false, "riak_tools/bucket-types")
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
    //finalizedBy(stopContainer)

    useJUnit()

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath


    filter {
        //include all integration tests
        includeTestsMatching("ITest*")
    }

    val properties: Map<String, String> = mutableMapOf(
            "com.basho.riak.yokozuna" to "false", // Not testing yokuzuna features
            "com.basho.riak.timeseries" to (baseImage == "riak-ts").toString()
    )
    // Set test properties to use
    systemProperties(properties)

    testLogging {
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
