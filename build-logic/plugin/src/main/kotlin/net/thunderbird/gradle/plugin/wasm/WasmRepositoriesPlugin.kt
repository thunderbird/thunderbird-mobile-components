package net.thunderbird.gradle.plugin.wasm

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin

/**
 * Routes the Wasm Node.js distribution download through the repository declared in `settings.gradle.kts`.
 *
 * This build uses [org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS], but Kotlin's Wasm
 * Node tooling would otherwise add a project-level repository for the Node distribution. Unsetting the default
 * download base URL makes Kotlin resolve Node through the settings repository instead.
 *
 * Applied to the root project, this configures the root Node.js spec that drives the shared Node download. The
 * per-project Node setup tasks need the same treatment, applied via
 * [useSettingsRepositoryForWasmNodeJsDistribution].
 */
class WasmRepositoriesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "The Wasm repositories plugin must be applied to the root project."
        }

        target.plugins.apply(WasmNodeJsRootPlugin::class.java)
        target.plugins.withType(WasmNodeJsRootPlugin::class.java).configureEach {
            target.configureWasmNodeJsSpec()
        }
    }
}

/**
 * Configures this project's Wasm Node.js spec to resolve the Node distribution through the settings repository.
 *
 * See [WasmRepositoriesPlugin] for the rationale. Call this from a Kotlin Multiplatform module that enables the
 * `wasmJs` target so its per-project `kotlinWasmNodeJsSetup` task does not add a project-level repository.
 */
fun Project.useSettingsRepositoryForWasmNodeJsDistribution() {
    plugins.withType(WasmNodeJsPlugin::class.java).configureEach {
        configureWasmNodeJsSpec()
    }
}

private fun Project.configureWasmNodeJsSpec() {
    extensions.configure("kotlinWasmNodeJsSpec", useSettingsRepositoryForWasmNodeJsDistribution)
}

private val useSettingsRepositoryForWasmNodeJsDistribution = Action<WasmNodeJsEnvSpec> {
    downloadBaseUrl.unset()
}
