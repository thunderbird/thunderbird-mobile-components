package net.thunderbird.gradle.plugin.versioning.internal

import java.util.Properties
import net.thunderbird.gradle.plugin.versioning.internal.Version

/**
 * Mapper to convert between [Version] and [java.util.Properties].
 */
internal class VersionPropertiesMapper {

    fun to(properties: Properties): Version? {
        if (properties.isEmpty) return null
        val major = properties["MAJOR"]?.toString()?.toIntOrNull() ?: return null
        val minor = properties["MINOR"]?.toString()?.toIntOrNull() ?: return null
        val patch = properties["PATCH"]?.toString()?.toIntOrNull() ?: return null
        val snapshot = properties["SNAPSHOT"]?.toString()?.equals("true", ignoreCase = true) ?: true
        return Version(major, minor, patch, snapshot)
    }

    fun from(version: Version): Properties {
        val properties = Properties()
        properties["MAJOR"] = version.major.toString()
        properties["MINOR"] = version.minor.toString()
        properties["PATCH"] = version.patch.toString()
        properties["SNAPSHOT"] = version.snapshot.toString()
        return properties
    }
}
