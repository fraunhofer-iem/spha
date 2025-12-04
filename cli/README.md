![SPHA Logo](docs/img/Software_Project_Health_Assistant_Secondary-Logo.png)

## About

SPHA is a fully automated tool suite that assesses and communicates all aspects
of software product quality. It does so by combining data about your projects
from sources like ticketing systems, and static analysis tools. For more details
see [software-product.health](https://www.software-product.health).

## SPHA-CLI Tool

This project contains SPHA's command line tool, a wrapper around our
[core library](https://www.github.com/fraunhofer-iem/spha). The CLI-tool can `transform` tool results
into `RawValueKpis`, `calculate` a given `KpiHierarchy` based on `RawValueKpis`, and
generate a human-readable report.  
A tool demo using our [GitHub Action](https://www.github.com/fraunhofer-iem/spha-action) can be
found [here](https://www.github.com/fraunhofer-iem/spha-demo).

## Installation

### Using Docker

The easiest way to get started with SPHA is by using the included docker image. To
build it locally run `docker build -t TAG_NAME .`. Afterward, you can run the container
with `docker run TAG_NAME`. The default command will print the `--help` statement that
further explains how to use SPHA.  
This repository contains a prebuild version of SPHA's docker image. To use it just run
`docker pull ghcr.io/fraunhofer-iem/spha-cli:latest` and then run `docker run fraunhofer-iem/spha-cli`.

### Build native

SPHA is a 100% Kotlin project build with Gradle. You must have Kotlin installed on your
system. To use Gradle either install it locally our use the included Gradle wrapper.
We aim to always support the latest version of Kotlin and Gradle.

To build the project using the wrapper run `./gradlew build`.  
`./gradlew run` executes SPHA. We use CLIKT as a command line framework, see their
[documentation](https://ajalt.github.io/clikt/quickstart/#developing-command-line-applications-with-gradle) on how to
interact with CLIKT based tools. By default, it will print the `--help` output that further explains
how to use SPHA.

## Usage

After successfully building SPHA you can generally use one of the following three commands:
* `transform` - transforms a given tool result into our internal `RawValueKpi` format
* `calculate` - calculates a given `KpiHierarchy` by combining it with `RawValueKpi`
* `report` - generates a human-readable report from a given `KpiResultHierarchy`

For the most up-to-date documentation of each command run `--help`.

## Contribute

You are welcome to contribute to SPHA. Please make sure you adhere to our
[contributing](CONTRIBUTING.md) guidelines.  
First time contributors are asked to accept our
[contributor license agreement (CLA)](CLA.md).
For questions about the CLA please contact us at _SPHA(at)iem.fraunhofer.de_ or create an issue.

## License

Copyright (C) Fraunhofer IEM.  
Software Product Health Assistant (SPHA) and all its components are published under the MIT license.

<picture>
<source media="(prefers-color-scheme: dark)" srcset="./docs/img/IEM_Logo_White.png">
<img alt="Logo IEM" src="./docs/img/IEM_Logo_Dark.png">
</picture>
 
