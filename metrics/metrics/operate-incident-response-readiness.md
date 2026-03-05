---
id: operate-incident-response-readiness
title: Incident Response Readiness
phase: operate
tags:
  - incident-response
  - readiness
related_tools:
  - pagerduty
  - splunk
depends_on:
  - deploy-hardening-compliance
thresholds:
  - name: Readiness target
    value: ">= 95%"
    description: Percentage of services with up-to-date runbooks.
references:
  - https://www.cisa.gov/resources-tools/services/incident-response
---

# Description

Measure the percentage of services with current incident response runbooks and on-call coverage.

# Measurement

Count services with reviewed runbooks in the last 6 months divided by total services.

# Notes

Keeps response posture aligned with operational reality.
