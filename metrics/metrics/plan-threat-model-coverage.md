---
id: plan-threat-model-coverage
title: Threat Model Coverage
phase: plan
tags:
  - requirements
  - threat-modeling
related_tools:
  - threatmodeler
  - miro
depends_on:
  - plan-security-requirements-coverage
thresholds:
  - name: Coverage target
    value: ">= 80%"
    description: Percentage of critical features with a threat model.
references:
  - https://owasp.org/www-community/Threat_Modeling
---

# Description

Track how many critical features have a documented threat model before implementation begins.

# Measurement

Divide the count of critical features with a threat model by the total number of critical features planned for the release.

# Notes

Helps ensure that high-risk areas are assessed before coding starts.
