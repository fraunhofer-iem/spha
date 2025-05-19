![SPHA Logo](docs/img/Software_Project_Health_Assistant_Secondary-Logo.png)

## About

SPHA is a fully automated tool suite that assesses and communicates all aspects
of software product quality. It does so by combining data about your projects
from sources like ticketing systems, and static analysis tools. For more details
see [software-product.health](https://www.software-product.health).

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

## Contribute

You are welcome to contribute to SPHA. Please make sure you adhere to our
[contributing](CONTRIBUTING.md) guidelines.
First time contributors are asked to accept our [contributor license agreement (CLA)](CLA.md)
by creating a pull-request and following the instructions added as a comment.
For questions about the CLA please contact us at _SPHA(at)iem.fraunhofer.de_ or create an issue.

## License

Copyright (C) Fraunhofer IEM.
Software Product Health Assistant (SPHA) and all its components are published under the MIT license.

<picture>
<source media="(prefers-color-scheme: dark)" srcset="./docs/img/IEM_Logo_White.png">
<img alt="Logo IEM" src="./docs/img/IEM_Logo_Dark.png">
</picture>
