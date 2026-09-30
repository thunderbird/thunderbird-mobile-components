/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
plugins {
    alias(libs.plugins.tb.cli)
}

application {
    mainClass.set("net.thunderbird.cli.importcode.MainKt")
    applicationName = "import-code"
}

dependencies {
    implementation(project(":cli:authorship"))
    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.components.core.testing)
    testImplementation(libs.testBalloon.framework.core)
    testImplementation(libs.kotlinx.io.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.assertk)
}
