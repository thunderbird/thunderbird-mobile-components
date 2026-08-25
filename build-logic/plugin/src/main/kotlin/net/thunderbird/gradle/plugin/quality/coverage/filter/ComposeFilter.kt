/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.quality.coverage.filter

import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

internal fun KoverReportFiltersConfig.composeFilter() {
    excludes {
        // Exclude Compose Multiplatform generated resource packages and runtime resource wrappers
        // so that auto-generated resource accessors don't affect coverage numbers.
        classes(
            // Compose Resources
            "*.Res",
            "*.ActualResourceCollectorsKt",
        )

        annotatedBy(
            "androidx.compose.ui.tooling.preview.Preview",
            "androidx.compose.ui.tooling.preview.PreviewLightDark",
            "app.k9mail.core.ui.compose.common.annotation.PreviewDevices",
            "app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground",
            "app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape",
        )
    }
}
