/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.quality.testing

import com.lemonappdev.konsist.api.Konsist
import kotlin.test.Test
import net.thunderbird.quality.util.assertNoForbiddenImports

class ValidateTestFramework {

    @Test
    fun `component and CLI tests should use TestBalloon`() {
        assertNoForbiddenImports(
            message = "Use TestBalloon testSuite and test instead of annotation-based or other test frameworks. " +
                "Build-logic and Konsist tests are exempt because they use JUnit.",
            replacementFor = { "de.infix.testBalloon.framework.core.testSuite" },
            scope = Konsist.scopeFromDirectories(listOf("components", "cli")),
        ) { importName ->
            forbiddenPackages.any { importName == it || importName.startsWith("$it.") }
        }
    }

    private companion object {
        val forbiddenPackages = setOf(
            "kotlin.test",
            "org.junit",
            "junit.framework",
            "org.testng",
            "io.kotest",
            "org.spekframework",
        )
    }
}
