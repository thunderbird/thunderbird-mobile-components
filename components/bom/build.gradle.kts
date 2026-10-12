plugins {
    id("net.thunderbird.gradle.plugin.bom")
}

description = "Bill of materials that aligns the versions of Thunderbird mobile components."

dependencies {
    constraints {
        api(projects.components.core.outcome)
        api(projects.components.core.logging.core)
        api(projects.components.core.logging.file)
        api(projects.components.core.logging.testing)
        api(projects.components.core.testing)
    }
}
