package net.thunderbird.gradle.plugin.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ValidatePublicationVersionTask : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val snapshotRequired: Property<Boolean>

    @TaskAction
    fun validate() {
        val versionString = version.get()
        if (snapshotRequired.get()) {
            requireSnapshotVersion(versionString)
        } else {
            requireStableVersion(versionString)
        }
    }

    private fun requireStableVersion(versionString: String) {
        if (versionString.endsWith(SNAPSHOT_SUFFIX)) {
            throw GradleException(
                "Stable releases require a non-SNAPSHOT version, but project '${projectPath.get()}' " +
                    "resolved '$versionString'. Create the component release tag before publishing a release.",
            )
        }
    }

    private fun requireSnapshotVersion(versionString: String) {
        if (!versionString.endsWith(SNAPSHOT_SUFFIX)) {
            throw GradleException(
                "Daily snapshots require a SNAPSHOT version, but project '${projectPath.get()}' " +
                    "resolved '$versionString'. Daily snapshots should be published from an untagged main commit.",
            )
        }
    }

    private companion object {
        private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
    }
}
