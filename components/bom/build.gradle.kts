plugins {
    `java-platform`
    id("net.thunderbird.gradle.plugin.publishing")
    id("net.thunderbird.gradle.plugin.versioning")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        // Add constraints for published components here, e.g.:
        // api("net.thunderbird:some-component:${project.version}")
    }
}
