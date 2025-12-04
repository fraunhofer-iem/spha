# Software Product Health Assistant

This library helps users working with KPI hierarchies. It provides functionality to define
and validate KPI hierarchies, transform tool results into KPIs, and calculate a given KPI
hierarchy based on tool results.

Implementation details, as well as usage instructions for the library's API is described in the code as JavaDoc.
This document is meant to give a general overview of the library's functionality and intended structure.

## Tool Adapter

A tool adapter transforms the given tool results into *RawValueKPIs*. This functionality is
defined by the `interface KpiAdapter<T>`. We support two kinds of adapters: *tools* and *kpis*.
*Tool* adapters take a tool's result as an input and transform it into, possibly multiple, KPIs.
A *tool* adapter might use one or multiple *kpi* adapters to do this transformation.<p>
The reasoning behind this design is that one tool might produce multiple different KPIs.
E.g., the OSS review toolkit (ORT) produces information about vulnerabilities, licenses,
and more. Thus, the ORT *tool* adapter takes ORT results as an input and uses multiple *kpi*
adapters to generate vulnerability KPIs (using the *cve* adapter) and license KPIs.

## KPI Hierarchy

The KPI hierarchy can be viewed as a domain specific language (DSL), which defines calculation rules and semantic
connections between different KPIs in a hierarchical structure.
It is implemented as a graph in which each node defines a `KpiId`, which is used to identify
which KPI belongs to the node, as well as a list of children. Further, the node stores a `StrategyId`,
which relates to a `CalculationStrategy`. The `CalculationStrategy` defines how each node processes the data stored in
its children. The `KpiId` has no influence on the semantic processing of the KPI hierarchy. For more information
regarding the calculation, see [Kpi Calculation](#kpi-calculation).

Additionally, we define a `KpiResultHierarchy`, which stores the same information as the regular `KpiHierarchy` as well
as a `KpiCalculationResult`.

The root node of every KPI hierarchy represents the project's overall health score.

## Validation

The `KpiValidator` is used to check the semantics of a given `KpiHierarchy`. It compares the requirements
of each node's `Calculation Strategy` with the given structure. E.g., a `RatioCalculationStrategy` requires two children
to be calculated. This is checked by the validator.
The validator supports a *strict* and *relaxed* mode. In *strict* mode validation fails if there is an ambiguity in the
`KpiHierarchy`. *Relaxed* mode allows `KpiHierarchies` which are defined in a way that we can perform calculations on
it, but which might result in ambiguous results.

## KPI Calculation

The KPI calculation is performed on a given `KpiHierarchy` and a list of `RawValueKpi` objects. The `RawValueKpi`
objects can be generated using the [Adapters](#tool-adapter). The calculation is performed in two steps.

1. We connect the `RawValueKpi` objects to the `KpiHierarchy`. This is done by comparing the `KpiId` of the hierarchy
   node and the `RawValueKpi`. Whenever there is a match the value of the KPI is attached to the node.
2. We calculate the `KpiCalculationResult` results by applying the `CalculationStrategy` on each node. The strategies
   are applied depth-first order. This is important, as we must start our calculation at the leaf nodes and work our
   way up the tree to the root.

## Usage Examples

### Creating a KPI Hierarchy

```kotlin
// Create a simple KPI hierarchy with weighted average strategy at the root
val root = KpiNode(
    typeId = KpiType.ROOT.name,
    strategy = KpiStrategyId.WEIGHTED_AVERAGE_STRATEGY,
    edges = listOf(
        KpiEdge(
            target = KpiNode(
                typeId = KpiType.SECURITY.name,
                strategy = KpiStrategyId.MAXIMUM_STRATEGY,
                edges = listOf(
                    KpiEdge(
                        target = KpiNode(
                            typeId = KpiType.CODE_VULNERABILITY_SCORE.name,
                            strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                            edges = emptyList()
                        ),
                        weight = 1.0
                    )
                )
            ),
            weight = 0.5
        ),
        KpiEdge(
            target = KpiNode(
                typeId = KpiType.SAST_USAGE.name,
                strategy = KpiStrategyId.RAW_VALUE_STRATEGY,
                edges = emptyList()
            ),
            weight = 0.5
        )
    )
)

// Create a KPI hierarchy from the root node
val hierarchy = KpiHierarchy.create(root)
```

### Transforming Tool Results to KPIs

```kotlin
// Example of using the CveAdapter to transform vulnerability data to KPIs
val vulnerabilities = listOf(
    VulnerabilityDto(
        cveIdentifier = "CVE-2023-1234",
        packageName = "example-package",
        severity = 7.5
    )
)

// Transform to code vulnerability KPIs
val codeVulnerabilityResults = CveAdapter.transformCodeVulnerabilityToKpi(vulnerabilities)

// Extract the raw value KPIs from successful results
val rawValueKpis = codeVulnerabilityResults
    .filterIsInstance<AdapterResult.Success.Kpi<VulnerabilityDto>>()
    .map { it.rawValueKpi }
```

### Calculating KPIs

```kotlin
// Calculate KPIs using the hierarchy and raw values
val resultHierarchy = KpiCalculator.calculateKpis(
    hierarchy = hierarchy,
    rawValueKpis = rawValueKpis,
    strict = false
)

// Access the overall health score
val overallScore = when (val result = resultHierarchy.rootNode.result) {
    is KpiCalculationResult.Success -> result.score
    is KpiCalculationResult.Incomplete -> result.score
    else -> 0
}

println("Overall health score: $overallScore")
```
