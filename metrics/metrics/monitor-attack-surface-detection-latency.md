---
id: monitor-attack-surface-detection-latency
title: Attack Surface Detection Latency
phase: monitor
tags:
  - monitoring
  - detection
related_tools:
  - splunk
  - crowdstrike
thresholds:
  - name: Detection target
    value: "<= 24 hours"
    description: Time to detect newly exposed services or assets.
depends_on:
  - monitor-detection-coverage
  - deploy-hardening-compliance
  - operate-incident-response-readiness
references:
  - https://owasp.org/www-project-top-ten/
---

# Description

Measure how quickly new exposed services or asset changes are detected by monitoring.

# Measurement

Track the median time between an asset change and its first detection in monitoring systems.

# Notes

Lower latency helps reduce dwell time for misconfigurations and unexpected exposure.
