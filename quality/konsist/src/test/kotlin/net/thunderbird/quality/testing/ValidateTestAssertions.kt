/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.quality.testing

import kotlin.test.Test
import net.thunderbird.quality.util.assertNoForbiddenImports

class ValidateTestAssertions {

    @Test
    fun `no class should use Truth assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead.",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("com.google.common.truth") }
    }

    @Test
    fun `no class should use Hamcrest assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead.",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("org.hamcrest") }
    }

    @Test
    fun `no class should use AssertJ assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead.",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("org.assertj") }
    }

    @Test
    fun `no class should use JUnit assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead of org.junit.Assert and junit.framework.",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("org.junit.Assert") || it.startsWith("junit.framework") }
    }

    @Test
    fun `no class should use Kotlin test assertions`() {
        assertNoForbiddenImports(
            message = "Use assertk instead of kotlin.test assertions. " +
                "Only ${allowedKotlinTestImports.joinToString()} may be imported from kotlin.test.",
            replacementFor = ::assertkReplacementFor,
        ) { it.startsWith("kotlin.test.") && it !in allowedKotlinTestImports }
    }

    private companion object {
        val allowedKotlinTestImports = setOf(
            "kotlin.test.Test",
            "kotlin.test.BeforeTest",
            "kotlin.test.AfterTest",
            "kotlin.test.Ignore",
        )

        val assertkReplacements = mapOf(
            "assertEquals" to "assertThat(actual).isEqualTo(expected)",
            "assertNotEquals" to "assertThat(actual).isNotEqualTo(expected)",
            "assertArrayEquals" to "assertThat(actual).isEqualTo(expected)",
            "assertContains" to "assertThat(actual).contains(expected)",
            "assertTrue" to "assertThat(x).isTrue()",
            "assertFalse" to "assertThat(x).isFalse()",
            "assertNull" to "assertThat(x).isNull()",
            "assertNotNull" to "assertThat(x).isNotNull()",
            "assertSame" to "assertThat(actual).isSameInstanceAs(expected)",
            "assertIs" to "assertThat(x).isInstanceOf<T>()",
            "assertFailsWith" to "assertFailure { }.isInstanceOf<T>()",
            "assertThrows" to "assertFailure { }.isInstanceOf<T>()",
            "assertFails" to "assertFailure { }",
            "fail" to "assertk.fail(message)",
        )

        fun assertkReplacementFor(importName: String): String =
            assertkReplacements[importName.substringAfterLast('.')] ?: "the assertk equivalent"
    }
}
