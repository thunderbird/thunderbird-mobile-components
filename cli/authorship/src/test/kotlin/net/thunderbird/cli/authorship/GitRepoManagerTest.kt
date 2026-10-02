/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import net.thunderbird.components.core.testing.temporaryDirectoryFixture

val gitRepoManagerTest by testSuite("GitRepoManager") {
    temporaryDirectoryFixture().asParameterForEach {
        test("resolveRepository clones source repo into build/tmp and checks out requested commit") { tmpPath ->
            val upstreamDir = File(tmpPath.toString(), "upstream-repo")
            initGitRepo(upstreamDir)
            val file1 = File(upstreamDir, "init.txt")
            file1.writeText("initial")
            runGit(upstreamDir, "add", ".")
            runGit(upstreamDir, "commit", "-m", "init", "--author=Safe Dev <safe@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(upstreamDir)
            val commitSha = analyzer.getHeadCommit()

            val baseDir = File(tmpPath.toString(), "workdir")
            baseDir.mkdirs()
            val manager = GitRepoManager(workingDir = baseDir)
            val resolved = manager.resolveRepository(
                sourceRepoUrl = upstreamDir.absolutePath,
                sourceCommit = commitSha,
            )

            assertThat(resolved.exists()).isTrue()
            assertThat(File(resolved, "init.txt").exists()).isTrue()
            assertThat(File(resolved, "init.txt").readText()).isEqualTo("initial")
        }

        test("resolveRepository cleans stale tracked edits and untracked files before switching revisions") { tmpPath ->
            val upstreamDir = File(tmpPath.toString(), "upstream-repo")
            initGitRepo(upstreamDir)
            val file1 = File(upstreamDir, "file1.txt")
            file1.writeText("c1 content")
            runGit(upstreamDir, "add", ".")
            runGit(upstreamDir, "commit", "-m", "commit 1", "--author=Safe Dev <safe@thunderbird.net>")
            val analyzer = GitAuthorshipAnalyzer(upstreamDir)
            val commit1 = analyzer.getHeadCommit()

            val file2 = File(upstreamDir, "file2.txt")
            file2.writeText("c2 content")
            runGit(upstreamDir, "add", ".")
            runGit(upstreamDir, "commit", "-m", "commit 2", "--author=Safe Dev <safe@thunderbird.net>")
            val commit2 = analyzer.getHeadCommit()

            val baseDir = File(tmpPath.toString(), "workdir")
            baseDir.mkdirs()
            val manager = GitRepoManager(workingDir = baseDir)

            // Initial checkout at commit 1
            val resolved = manager.resolveRepository(
                sourceRepoUrl = upstreamDir.absolutePath,
                sourceCommit = commit1,
            )
            assertThat(File(resolved, "file1.txt").readText()).isEqualTo("c1 content")

            // Stale tracked edit and untracked file in cached clone that would collide with commit 2
            File(resolved, "file1.txt").writeText("dirty tracked change")
            File(resolved, "file2.txt").writeText("dirty conflicting untracked")

            // Switch revision to commit 2 - should clean and succeed without error
            val updated = manager.resolveRepository(
                sourceRepoUrl = upstreamDir.absolutePath,
                sourceCommit = commit2,
            )
            assertThat(updated.exists()).isTrue()
            assertThat(File(updated, "file1.txt").readText()).isEqualTo("c1 content")
            assertThat(File(updated, "file2.txt").readText()).isEqualTo("c2 content")
        }

        test("cleanup removes repo inside tmp directory") { tmpPath ->
            val baseDir = File(tmpPath.toString())
            val tmpDir = File(baseDir, "build/tmp/test-repo")
            tmpDir.mkdirs()
            val dummyFile = File(tmpDir, "test.txt")
            dummyFile.writeText("test")
            assertThat(dummyFile.exists()).isTrue()

            val manager = GitRepoManager(workingDir = baseDir)
            manager.cleanup(tmpDir)
            assertThat(tmpDir.exists()).isFalse()
        }
    }
}
