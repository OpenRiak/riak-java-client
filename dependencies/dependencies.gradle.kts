/*
 * https://docs.gradle.org/current/userguide/platforms.html
 */
plugins {
    id ("java-platform")
}

/*
 * Where versions can be configured, can be ranges and can give reasons, can avoid particular versions, set min and max allowed etc
 *
 * https://docs.gradle.org/current/userguide/single_versions.html
 * https://docs.gradle.org/current/userguide/rich_versions.html
 */
dependencies {
    constraints {

        val log4j2 = "2.+"
        api("org.apache.logging.log4j:log4j-api") {
            version {
                strictly(log4j2)
            }
        }

        api("org.apache.logging.log4j:log4j-slf4j-impl") {
            version {
                strictly(log4j2)
            }
        }

        api("org.apache.logging.log4j:log4j-jul") {
            version {
                strictly(log4j2)
            }
        }

        api("org.apache.logging.log4j:log4j-core") {
            version {
                strictly(log4j2)
            }
        }

        api("org.slf4j:slf4j-api") {
            version {
                require("1.7.21")
            }
        }

        /**
         * ======================================================================================================
         * Third Party Dependencies
         * ======================================================================================================
         */

        api("com.google.code.gson:gson") {
            version {
                require("2.+")
            }
        }

        api("com.google.protobuf:protobuf-java") {
            version {
                require("2.6.1")
            }
        }

        api("org.hamcrest:hamcrest-core") {
            version {
                require("1.3")
            }
        }

        api("com.jayway.awaitility:awaitility") {
            version {
                require("1.6.1")
            }
        }

        val jacksonVersion = "2.8.0"
        api("com.fasterxml.jackson.core:jackson-databind") {
            version {
                require(jacksonVersion)
            }
        }

        api("com.fasterxml.jackson.datatype:jackson-datatype-joda") {
            version {
                require(jacksonVersion)
            }
        }

        api("io.netty:netty-all") {
            version {
                require("4.1.5.Final")
            }
        }

        api("org.erlang.otp:jinterface") {
            version {
                require("1.6.1")
            }
        }

        val powermockVersion = "1.6.5"
        api("org.powermock:powermock-api-mockito") {
            version {
                require(powermockVersion)
            }
        }

        api("org.powermock:powermock-module-junit4") {
            version {
                require(powermockVersion)
            }
        }

        api("org.jetbrains:annotations") {
            version {
                require("23.+")
            }
        }

        api("org.apache.santuario:xmlsec") {
            version {
                require("2.+")
                because(
                    "auth-common pulls in 1.5.6 which is quite old and is now vulnerable to CVE-2021-40690, note that this is forcing a " +
                            "major version upgrade on a dependancy of auth-common"
                )
            }
        }

        api("com.fasterxml.woodstox:woodstox-core") {
            version {
                require("5.+")
                because("org.apache.santuario:xmlsec pulls in an older woodstox-core (5.2.1) which is vulnerable")
            }
        }

        api("org.apache.commons:commons-text") {
            version {
                require("1.+")
            }
        }

        api("org.apache.commons:commons-compress") {
            version {
                require("1.+")
            }
        }

        api("com.fasterxml.jackson:jackson-bom") {
            version {
                require("2.+")
                because("jackson-databind 2.11.0 is vulnerable and is pulled in by java-jwt which is used by fugu-common (pulled through auth-common)")
            }
        }

        api("io.netty:netty-codec-http") {
            version {
                require("4.+")
                because("netty-codec-http-4.1.68.Final.jar is considered vulnerable")
            }
        }

        api("com.ibm.icu:icu4j") {
            version {
                require("+") // this isn't great but the library is just unicode parsing so safe enough..
                because("auth-common depends on oms-persistence-core which uses oms-utils that depends on icu4j 55.1 that is vulnerable")
            }
        }

        api("io.prometheus:simpleclient_bom") {
            version {
                require("0.+")
            }
        }

        api("software.amazon.awssdk:s3") {
            version {
                require("2.+")
            }
        }

        /**
         * ======================================================================================================
         * Crypto libraries
         * ======================================================================================================
         */

        val bouncyCastle = "1.70"
        api("org.bouncycastle:bcprov-jdk15on") {
            version {
                require(bouncyCastle)
            }
        }

        api("org.bouncycastle:bcpg-jdk15on") {
            version {
                require(bouncyCastle)
            }
        }

        api("org.bouncycastle:bcpkix-jdk15on") {
            version {
                require(bouncyCastle)
            }
        }

        // Fips Compliant
        api("org.bouncycastle:bc-fips") {
            version {
                require("1.0.+")
            }
        }

        api("org.bouncycastle:bctls-fips") {
            version {
                require("1.0.+")
            }
        }

        api("org.bouncycastle:bcpkix-fips") {
            version {
                require("1.0.+")
            }
        }

        api("org.bouncycastle:bcpg-fips") {
            version {
                require("1.0.+")
            }
        }

        /**
         * ======================================================================================================
         * Testing libraries
         * ======================================================================================================
         */

        val junitEngine = "1.7.1"
        api("org.junit.platform:junit-platform-engine") {
            version {
                strictly(junitEngine)
            }
        }

        api("org.junit.platform:junit-platform-launcher") {
            version {
                strictly(junitEngine)
            }
        }


        api("org.easymock:easymock") {
            version {
                require("4.+")
            }
        }

        api("com.workday:platsec-mocks") {
            version { require("+") }
        }
    }
}
