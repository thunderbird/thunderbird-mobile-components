/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.tb.quality.spotless)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

// Konsist reads project sources directly, so rerun the tests when Kotlin sources change.
tasks.withType<Test>().configureEach {
    inputs.files(
        fileTree(isolated.rootProject.projectDirectory) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", ".gradle/**", ".kotlin/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("konsistSources")

    // Show the violation report rather than only the exception class.
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
    }
}
