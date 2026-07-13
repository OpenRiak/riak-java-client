/*
 * https://docs.gradle.org/current/userguide/platforms.html
 */
plugins {
    id("java-platform")
}

/*
 * Where versions can be configured, can be ranges and can give reasons, can avoid particular versions, set min and max allowed etc
 *
 * https://docs.gradle.org/current/userguide/single_versions.html
 * https://docs.gradle.org/current/userguide/rich_versions.html
 */
dependencies {
    constraints {
        /**
         * ======================================================================================================
         * Third Party Dependencies
         * ======================================================================================================
         */
        api("com.google.protobuf:protobuf-java") {
            version {
                require("3.+")
            }
        }

        api("com.fasterxml.jackson:jackson-bom") {
            version {
                require("2.+")
            }
        }

        api("io.netty:netty-bom") {
            version {
                require("4.+")
            }
        }


        api("org.erlang.otp:jinterface") {
            version {
                require("1.+")
            }
        }

        api("commons-codec:commons-codec") {
            version {
                require("1.+")
            }
        }

        api("javax.xml.bind:jaxb-api") {
            version {
                require("2.3.+")
            }
        }

        api("org.slf4j:slf4j-api") {
            version {
                require("1.+")
            }
        }

        /**
         * ======================================================================================================
         * Testing libraries
         * ======================================================================================================
         */

        api("org.mockito:mockito-core") {
            version {
                require("3.+")
            }
        }

        val powerMock = "2.+"
        api("org.powermock:powermock-api-mockito2") {
            version {
                require(powerMock)
            }
        }

        api("org.powermock:powermock-module-junit4") {
            version {
                require(powerMock)
            }
        }

        api("org.powermock:powermock-reflect") {
            version {
                require(powerMock)
            }
        }

        api("org.hamcrest:hamcrest-core") {
            version {
                // Pin to the Maven Central release; "1.+" can otherwise resolve to the
                // Atlassian-only "1.4-atlassian-1" variant that is not on Maven Central.
                require("1.3")
            }
        }

        api("org.javassist:javassist") {
            version {
                require("3.+")
            }
        }

        api("com.jayway.awaitility:awaitility") {
            version {
                require("1.+")
            }
        }

        api("junit:junit") {
            version {
                strictly("4.+")
            }
        }

        api("org.junit.vintage:junit-vintage-engine") {
            version {
                strictly("+")
            }
        }
    }
}
