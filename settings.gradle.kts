import org.ajoberstar.reckon.gradle.ReckonExtension

/*
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/6.8/userguide/multi_project_builds.html
 */

rootProject.name = "workday-riak-client"

include("dependencies")

// Makes every sub-project have its `build.gradle` named after its name
for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "$name.gradle.kts"
        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}

/**
 * Set up Reckon plugin to manage the project.version used
 */
buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    // Apply semantic versioning: https://github.com/ajoberstar/reckon
    id("org.ajoberstar.reckon.settings") version ("0.+")
}

/**
 * Configuring Reckon (used for semantic versioning)
 * To push the latest tag use:  ./gradlew reckonTagPush -Preckon.stage=final
 * final means the version will be in the form 0.2.0 not 0.2.0-alpha.0.1+20210304T132206Z
 */
configure<ReckonExtension> {
    stages("alpha", "beta", "final")

    setDefaultInferredScope("minor")

    // Set the stage, aka are we building the final version, or is this a alpha/beta build
    setStageCalc(calcStageFromProp())

    // Scope is for "major", "minor", or "patch" bumps etc
    // for example, patch build plan uses: -Preckon.stage=final -Preckon.scope=patch
    setScopeCalc(calcScopeFromProp())
}
