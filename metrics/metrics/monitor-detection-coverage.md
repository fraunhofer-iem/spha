---
id: monitor-detection-coverage
title: Security Detection Coverage
phase: monitor
tags:
  - monitoring
  - detection
related_tools:
  - splunk
  - datadog
depends_on:
  - operate-incident-response-readiness
thresholds:
  - name: Coverage target
    value: ">= 90%"
    description: Percentage of critical assets with detection rules.
references:
  - https://attack.mitre.org/techniques/
---

# Description

Track coverage of security detection rules for critical assets.

# Measurement

Divide the number of critical assets with active detection rules by the total number of critical assets.

# Notes

Ensures monitoring keeps pace with expanding infrastructure.
