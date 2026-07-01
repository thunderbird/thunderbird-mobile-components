/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.testing

import de.infix.testBalloon.framework.core.TestFixture
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testPlatform
import kotlin.random.Random
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Creates a TestBalloon fixture for a temporary directory.
 *
 * The directory is deleted when tests pass or when running on CI. On local failures, the directory is retained and
 * printed to stdout for inspection.
 */
public fun TestSuiteScope.temporaryDirectoryFixture(
    prefix: String = "$testSuiteInScope-",
): TestFixture<Path> = testFixture {
    createTemporaryDirectory(prefix = prefix)
} closeWith { testsSucceeded ->
    if (shouldDeleteTemporaryDirectory(testsSucceeded, testPlatform.environment("CI"))) {
        SystemFileSystem.deleteRecursively(this)
    } else {
        println(temporaryDirectoryRetainedMessage(this))
    }
}

internal fun createTemporaryDirectory(prefix: String): Path {
    val root = Path("build", "tmp", "testBalloon")
    SystemFileSystem.createDirectories(root)
    val path = Path(root, "$prefix${Random.nextLong().toString(radix = 36)}")

    SystemFileSystem.createDirectories(path, mustCreate = true)
    return path
}

internal fun FileSystem.deleteRecursively(path: Path) {
    val metadata = metadataOrNull(path) ?: return

    if (metadata.isDirectory) {
        list(path).forEach { child -> deleteRecursively(child) }
    }

    delete(path, mustExist = false)
}

internal fun shouldDeleteTemporaryDirectory(testsSucceeded: Boolean, ciEnvironment: String?): Boolean {
    return testsSucceeded || ciEnvironment != null
}

internal fun temporaryDirectoryRetainedMessage(path: Path): String {
    return "Temporary directory: ${SystemFileSystem.resolve(path)}"
}
