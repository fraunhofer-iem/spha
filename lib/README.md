## SPHA Library

This module contains SPHA's core library. Its main purpose is to calculate a product's health score
based on a given `KpiHierarchy` and `RawValueKPIs`. Further, SPHA provides the possibility to transform
tool results into our internal `RawValueKPI` format. The transformation is handled by dedicated tool
adapters.

The [SPHA CLI Tool](../cli/) provides an executable command-line interface using this library.
The [SPHA UI](../ui/) provides a web-based visualization dashboard for analysis results.

A tool demo using our [GitHub Action](https://www.github.com/fraunhofer-iem/spha-action) can be
found [here](https://www.github.com/fraunhofer-iem/spha-demo).

## Modules

The library is divided into three modules:
- **model** - Data models and interfaces for KPIs, tool results, and hierarchies
- **adapter** - Tool adapters to transform various analysis tool outputs into SPHA format
- **core** - Calculation engine for computing health scores from KPI hierarchies

## Installation

SPHA is a 100% Kotlin project built with Gradle. You must have Kotlin installed on your
system. To use Gradle either install it locally or use the included Gradle wrapper.
We aim to always support the latest version of Kotlin and Gradle.

From the repository root, build the library using the wrapper: `./gradlew :lib:core:build :lib:model:build :lib:adapter:build`.

## Usage

To include one of the library modules in your project, use:
```kotlin
implementation("de.fraunhofer.iem:spha-model:VERSION")
implementation("de.fraunhofer.iem:spha-adapter:VERSION")
implementation("de.fraunhofer.iem:spha-core:VERSION")
```

## License

Copyright (C) Fraunhofer IEM.
Software Product Health Assistant (SPHA) and all its components are published under the [MIT license](../LICENSE.md).
