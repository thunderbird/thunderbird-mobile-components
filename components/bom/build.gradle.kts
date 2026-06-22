plugins {
    id("net.thunderbird.gradle.plugin.bom")
}

dependencies {
    constraints {
        api(projects.components.core.outcome)
    }
}
