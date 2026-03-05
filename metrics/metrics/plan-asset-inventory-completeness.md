---
id: plan-asset-inventory-completeness
title: Asset Inventory Completeness
phase: plan
tags:
  - asset-management
  - governance
related_tools:
  - confluence
  - cmdb
thresholds:
  - name: Coverage target
    value: ">= 95%"
    description: Percentage of in-scope systems mapped to an owner and data classification.
depends_on:
  - plan-security-requirements-coverage
  - plan-threat-model-coverage
references:
  - https://owasp.org/www-project-application-security-verification-standard/
---

# Description

Measure how many in-scope applications and services are represented in the asset inventory with owner, data classification, and criticality recorded.

# Measurement

Calculate the percentage of scoped systems with a complete inventory record before development planning starts.

# Notes

A complete inventory reduces blind spots during threat modeling and downstream controls.
