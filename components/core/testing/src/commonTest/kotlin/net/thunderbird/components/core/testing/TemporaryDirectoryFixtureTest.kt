/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

val temporaryDirectoryFixtureTest by testSuite("TemporaryDirectoryFixture") {

    temporaryDirectoryFixture(prefix = "temporary-directory-fixture-test-").asParameterForEach {
        test("creates a temporary directory") { temporaryDirectory ->
            assertThat(SystemFileSystem.metadataOrNull(temporaryDirectory)).isNotNull().given { metadata ->
                assertThat(metadata.isDirectory).isTrue()
            }
        }
    }

    test("deletes a temporary directory recursively") {
        val temporaryDirectory = createTemporaryDirectory(prefix = "temporary-directory-recursive-delete-test-")
        val nestedDirectory = Path(temporaryDirectory, "nested")
        val nestedFile = Path(nestedDirectory, "file.txt")

        SystemFileSystem.createDirectories(nestedDirectory)
        SystemFileSystem.sink(nestedFile).close()
        SystemFileSystem.deleteRecursively(temporaryDirectory)

        assertThat(SystemFileSystem.metadataOrNull(temporaryDirectory)).isNull()
    }

    test("deleteRecursively ignores a missing path") {
        val temporaryDirectory = createTemporaryDirectory(prefix = "temporary-directory-missing-delete-test-")
        val missingPath = Path(temporaryDirectory, "missing")

        try {
            SystemFileSystem.deleteRecursively(missingPath)

            assertThat(SystemFileSystem.metadataOrNull(missingPath)).isNull()
        } finally {
            SystemFileSystem.deleteRecursively(temporaryDirectory)
        }
    }

    test("should delete temporary directories after successful tests") {
        assertThat(shouldDeleteTemporaryDirectory(testsSucceeded = true, ciEnvironment = null)).isTrue()
    }

    test("should delete temporary directories on CI after failed tests") {
        assertThat(shouldDeleteTemporaryDirectory(testsSucceeded = false, ciEnvironment = "true")).isTrue()
    }

    test("should keep temporary directories locally after failed tests") {
        assertThat(shouldDeleteTemporaryDirectory(testsSucceeded = false, ciEnvironment = null)).isEqualTo(false)
    }

    test("formats the retained temporary directory message") {
        val temporaryDirectory = createTemporaryDirectory(prefix = "temporary-directory-retained-message-test-")

        try {
            assertThat(temporaryDirectoryRetainedMessage(temporaryDirectory))
                .isEqualTo("Temporary directory: ${SystemFileSystem.resolve(temporaryDirectory)}")
        } finally {
            SystemFileSystem.deleteRecursively(temporaryDirectory)
        }
    }
}
