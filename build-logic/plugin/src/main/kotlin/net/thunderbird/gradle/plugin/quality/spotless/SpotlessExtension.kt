/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.quality.spotless

val kotlinEditorConfigOverride = mapOf(
    "ktlint_code_style" to "intellij_idea",
    "ktlint_ignore_back_ticked_identifier" to "true",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "ktlint_standard_class-signature" to "disabled",
    "ktlint_standard_function-expression-body" to "disabled",
    "ktlint_standard_function-signature" to "disabled",
    "ktlint_standard_parameter-list-spacing" to "disabled",
    "ktlint_standard_property-naming" to "disabled",
)

val licenseHeaderMpl2 = """
    |/*
    | * This Source Code Form is subject to the terms of the Mozilla Public
    | * License, v. 2.0. If a copy of the MPL was not distributed with this
    | * file, You can obtain one at https://mozilla.org/MPL/2.0/.
    | */
""".trimMargin()
