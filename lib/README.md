## SPHA Library

This project contains SPHA's core library. Its main purpose is to calculate a products health score
based on a given `KpiHierarchy` and `RawValueKPIs`. Further, SPHA provides the possibility to transform
tool results into our internal `RawValueKPI` format. The transformation is handled by dedicated tool
adapters.
With the [SPHA CLI Tool](https://www.github.com/fraunhofer-iem/spha-cli) we have created an executable tool using this
library and showcasing its possibilities.
A tool demo using our [GitHub Action](https://www.github.com/fraunhofer-iem/spha-action) can be
found [here](https://www.github.com/fraunhofer-iem/spha-demo).

## Installation

SPHA is a 100% Kotlin project build with Gradle. You must have Kotlin installed on your
system. To use Gradle either install it locally our use the included Gradle wrapper.
We aim to always support the latest version of Kotlin and Gradle.

To build the project using the wrapper run `./gradlew build`.

## Usage

SPHA is divided into three modules `core`, `adapter`, and `model` that are individually published.
To include one of the components use `implementation("de.fraunhofer.iem:spha-XXX:VERSION")`.

## License

Copyright (C) Fraunhofer IEM.
Software Product Health Assistant (SPHA) and all its components are published under the MIT license.
