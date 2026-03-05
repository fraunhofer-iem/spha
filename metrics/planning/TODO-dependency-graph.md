# Dependency Graph Feature Plan

## Step-By-Step Todo (With Progress)

1. Baseline check (Status: Completed)
   - Locate current metric dependency usage and confirm available fields (`depends_on`, `markdown`, etc.).
   - Decide D3 modules to use for network layout and drag/zoom behaviors.
2. Data helpers for dependencies (Status: Completed)
   - Create a helper to build adjacency maps: `parentsById`, `childrenById`.
   - Add guardrails for cycles and missing IDs (record only, no UI placeholders).
3. Detail view dependency graph (Status: Completed)
   - Add a new D3-backed component to render a radial network with the current metric centered.
   - Show direct parents and direct children, with links between nodes.
   - Add expand/collapse control that toggles the entire graph panel.
   - Ensure desktop interactions (hover/click) and mobile safe rendering (no required gestures).
4. Full-catalogue graph view (Status: Completed)
   - Add a new route and update the header navigation.
   - Implement a D3 network view with pan/zoom and selectable nodes.
   - Default node count rule: show all nodes if `metrics.length <= 30`; otherwise show 30 and show a “more nodes available” indicator.
   - Display node labels (metric name) and a side panel with description + button to detail view.
5. Styling and UX polish (Status: Completed)
   - Applied phase-based node colors and base styling for graph views.
   - Aligned empty state styling inside the graph detail panel.
6. Navigation update (Status: Completed)
   - Replace the “Metrics” navbar link with a drawer that links to the list view and the graph view.
7. Full graph structure update (Status: Completed)
   - Add a central node connected to SSDLC phase nodes.
   - Connect each metric to its phase node, then retain metric-to-metric dependency edges.
   - Use distinct shapes for the central node and phase nodes.
   - Add phase selection highlighting and SSDLC phase descriptions in the side panel.
8. Detail view layout update (Status: Completed)
   - Move the dependency graph panel to the very bottom of the metric detail view (below all other content).
9. Tests (Status: Completed)
   - Unit tests for adjacency map helpers.
   - View tests for graph expansion, phase selection, metric selection, and detail graph presence.

## Decisions Captured

1. Graph library: D3 (use networks or hierarchies depending on fit).
2. Graph base: networks.
3. Detail graph layout: radial (as a constrained network).
4. Expand/collapse: toggle the entire graph panel.
5. Full-catalogue graph scale: show all when <= 30 metrics; otherwise show 30 with an indication that more nodes can be expanded.
6. Node detail content: label nodes with the metric name; clicking shows the metric description and a button to the existing detail view.
7. Styling: color nodes by phase; use a single node shape for now.
8. Missing dependencies: assume none, but still guard defensively.
9. Accessibility: desktop mouse interactions are primary; mobile should display but can be minimally interactive.
10. Full graph route: `/graph` with a navbar link labeled “Graph”.
11. Full graph expansion: show a “Show all metrics” action when more than 30 metrics exist.
12. Detail graph interaction: clicking parent/child nodes navigates to their metric detail view.
13. Description in graph detail panel: do not truncate.
14. Detail view graph placement: move to bottom below other content.
15. Full graph topology: central node -> phases -> metrics -> dependency links.

## Open Questions

- None at the moment.
