### Thunderbird Mobile Components – BOM

This module publishes a Bill of Materials (BOM) for Thunderbird Mobile Components. A BOM contains no code, it only 
defines version constraints for a curated set of artifacts so consumers can depend on them without specifying 
individual versions.

### How to populate the BOM

Add one `api("<groupId>:<artifactId>:<version>")` per component you want to add to the BOM inside the 
`dependencies.constraints` block. Use the exact versions you have published for those components.

Example:
```kotlin
dependencies {
    constraints {
        api("net.thunderbird:client-component:1.2.0")
        api("net.thunderbird:another-component:1.2.0")
    }
}
```

### Consuming the BOM

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation(platform("net.thunderbird:bom:<bomVersion>"))
    implementation("net.thunderbird:ews-client")        // version omitted
    implementation("net.thunderbird:another-component") // version omitted
}
```

Gradle (enforce strictly, optional):
```kotlin
dependencies {
    implementation(enforcedPlatform("net.thunderbird:bom:<bomVersion>"))
}
```

### Versioning

The BOM’s version is independent from individual components and is resolved from the `version.properties`.

### Publishing

**Workflow:**

1) Publish/update components at their intended versions.
2) Update BOM constraints to reflect those versions.
3) Publish a new BOM version that represents this coherent set.
4) Update consuming projects to use the new BOM version.
