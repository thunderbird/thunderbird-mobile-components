# Thunderbird Core Components

Core components provide small, foundational building blocks that can be reused by higher-level
components and applications.

## Components

| Component |        Module        |             Maven Coordinate              |                                Description                                 |
|-----------|----------------------|-------------------------------------------|----------------------------------------------------------------------------|
| [Logging](logging/README.md) | `:components:core:logging` | `net.thunderbird.components.core.logging` | Logging API with composite, console, and file logging. |
| [Outcome](outcome/README.md) | `:components:core:outcome` | `net.thunderbird.components.core:outcome` | Small result type with a flexible failure type, unlike Kotlin `Result`. |
