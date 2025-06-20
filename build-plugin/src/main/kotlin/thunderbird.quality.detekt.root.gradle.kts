import io.gitlab.arturbosch.detekt.Detekt

/**
 * Root project configuration for Detekt.
 *
 * It registers a task `detektAll` to run Detekt on the whole project
 */
plugins {
    id("io.gitlab.arturbosch.detekt")
}

tasks.register("detektAll") {
    group = "verification"
    description = "Runs detekt on the whole project"

    allprojects {
        this@register.dependsOn(tasks.withType<Detekt>())
    }
}
