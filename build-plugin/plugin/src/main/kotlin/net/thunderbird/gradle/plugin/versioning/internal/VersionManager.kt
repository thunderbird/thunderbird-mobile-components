package net.thunderbird.gradle.plugin.versioning.internal

import java.io.File
import java.util.Properties
import net.thunderbird.gradle.plugin.versioning.internal.Version
import net.thunderbird.gradle.plugin.versioning.internal.VersionPropertiesMapper

/**
 * Internal utility to centralize version file discovery, parsing, and persistence.
 * Keeps task implementations small and avoids code duplication.
 *
 * @param base The starting directory to search for version files.
 * @param root The repository root directory to limit the search.
 */
internal class VersionManager(
    val base: File,
    val root: File,
) {

    private val mapper = VersionPropertiesMapper()
    private var source: File? = null

    init {
        source = locateNearestVersionFile(base, root)
    }

    fun get(): Version {
        if (source != null) {
            return readVersion(source!!)
        }
        throw IllegalStateException(
            "[versioning] No version.properties found between ${base.path} and ${root.path}. " +
                "Create one with MAJOR, MINOR, PATCH and optional SNAPSHOT (defaults to true).",
        )
    }

    fun update(version: Version) {
        val current = source
        if (current != null) {
            storeVersion(current, version)
        } else {
            throw IllegalStateException("No version.properties file found to update.")
        }
    }

    private fun locateNearestVersionFile(start: File, repoRoot: File): File? {
        var dir: File? = start
        while (dir != null) {
            val candidate = File(dir, FILE_NAME)
            if (candidate.exists()) {
                // Log the located version.properties file for visibility
                println("[versioning] Using version file: ${candidate.path}")
                return candidate
            }
            if (dir == repoRoot) break
            dir = dir.parentFile
        }
        println(
            "[versioning] No version.properties found for ${start.path} and nearest parents up to ${repoRoot.path}.",
        )
        return null
    }

    private fun readVersion(file: File): Version {
        val properties = readProperties(file)
        val parsed = mapper.to(properties)
        if (parsed != null) return parsed
        throw IllegalStateException(
            "[versioning] Invalid version.properties at ${file.path}. " +
                "Expected keys: MAJOR, MINOR, PATCH and optional SNAPSHOT (defaults to true).",
        )
    }

    private fun storeVersion(file: File, version: Version) {
        val properties = mapper.from(version)
        writeProperties(file, properties)
    }

    private fun readProperties(file: File): Properties = Properties().also { p ->
        file.inputStream().use(p::load)
    }

    private fun writeProperties(target: File, properties: Properties) {
        val writer = PropertiesWriter()
        writer.write(target, properties)
    }

    private companion object {
        private const val FILE_NAME = "version.properties"
    }
}
