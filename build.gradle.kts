import com.google.protobuf.gradle.*
import com.workday.blobitory.gradle.PrintVersionPlugin
import com.workday.blobitory.gradle.PrintVersionPluginExtension

plugins {
    // Configures how dependencies are managed, aka versions used etc
    `dependency-conventions`
    // Configures the common java settings (version, jars, manifest, locking etc)
    `java-common-conventions`
    // Configures the integration tests that can be run
    `integration-test-conventions`
    // For common publishing settings
    `publishing-conventions`
    // For generating the java classes from the proto files
    id("com.google.protobuf")
}

dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda")
    implementation("io.netty:netty-all")
    implementation("org.erlang.otp:jinterface")
    implementation("commons-codec:commons-codec")

    // because of uses of javax.xml.bind.DatatypeConverter
    implementation("javax.xml.bind:jaxb-api")

    //Test dependencies
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.powermock:powermock-api-mockito2")
    testImplementation("org.powermock:powermock-module-junit4")
    testImplementation("org.powermock:powermock-reflect")
    testImplementation("org.hamcrest:hamcrest-core")
    testImplementation("com.jayway.awaitility:awaitility")
}

/**
 * ---------------------------------------------
 *              Lock File Update Tasks
 * ---------------------------------------------
 */
val slackToken: String? by project
tasks.register<LockFileUpdates>("lockfileUpdates")
tasks.register<CheckoutLockFileUpdatePrTask>("checkoutLockFilePr")


/**
 * ---------------------------------------------
 *              Base Config
 * ---------------------------------------------
 */
group = "com.workday.riak"

base {
    // Sets the name of the jar
    archivesName.set("riak-client")
}



apply<PrintVersionPlugin>()
configure<PrintVersionPluginExtension> {
    version.set(project.version.toString())
}

/**
 * ---------------------------------------------
 *              Protobuf Generation
 * ---------------------------------------------
 */
// Make then use the internal configuration for dependancies
configurations.getByName("compileProtoPath").extendsFrom(configurations.getByName("internal"))
configurations.getByName("testCompileProtoPath").extendsFrom(configurations.getByName("internal"))

sourceSets {
    main {
        proto {
            // Configure it to point at the shared .proto files for the git submodule
            srcDir("riak_protobuf/src/")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.+"
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

/**
 * ---------------------------------------------
 *              Publishing
 * ---------------------------------------------
 */
publishing {
    publications {
        create<IvyPublication>("ivy") {
            module = "wd-riak-client"

            descriptor {
                author {
                    name.set("Document Storage Team")
                }
                description {
                    text.set("Java Client for interacting with a Riak Cluster")
                    homepage.set("https://bitbucket.internal.invalid/projects/DS/repos/workday-riak-client")
                }
            }

            versionMapping {
                usage(Usage.JAVA_API) {
                    fromResolutionResult()
                }
                usage(Usage.JAVA_RUNTIME) {
                    fromResolutionResult()
                }
            }

            from(components["java"])
        }
    }
}

