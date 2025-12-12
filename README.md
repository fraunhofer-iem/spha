![SPHA Logo](docs/img/Software_Project_Health_Assistant_Secondary-Logo.png)

## About

SPHA is a fully automated tool suite that assesses and communicates all aspects
of software product quality. It does so by combining data about your projects
from sources like ticketing systems, and static analysis tools. For more details
see [software-product.health](https://www.software-product.health).

## Repository Structure

This monorepo contains all SPHA components:

### Library (`lib/`)

The core library is divided into three modules that are individually published:
- **model** - Data models and interfaces for KPIs and tool results
- **adapter** - Tool adapters to transform various analysis tool outputs into SPHA format
- **core** - Calculation engine for computing health scores from KPI hierarchies

### CLI (`cli/`)

A command-line tool that wraps the core library, providing commands to:
- `transform` - Convert tool results into SPHA's internal format
- `calculate` - Compute health scores from KPI hierarchies
- `report` - Generate human-readable reports

### UI (`ui/`)

A Vue.js web application for visualizing SPHA analysis results, featuring:
- Interactive health score dashboard
- KPI hierarchy visualization
- Tool integration overview

## Getting Started

For detailed instructions on each component, please refer to their respective README files linked above.

A tool demo using our [GitHub Action](https://www.github.com/fraunhofer-iem/spha-action) can be
found [here](https://www.github.com/fraunhofer-iem/spha-demo).

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
