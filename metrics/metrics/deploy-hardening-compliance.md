---
id: deploy-hardening-compliance
title: Deployment Hardening Compliance
phase: deploy
tags:
  - hardening
  - compliance
related_tools:
  - ansible
  - terraform
depends_on:
  - release-security-approval-latency
thresholds:
  - name: Compliance target
    value: ">= 90%"
    description: Percentage of environments meeting hardening baseline.
references:
  - https://www.cisecurity.org/cis-benchmarks
---

# Description

Measure how many deployment environments meet the defined hardening baseline.

# Measurement

Divide the number of environments passing hardening checks by the total number of environments.

# Notes

Ensures production environments remain aligned with security baselines.
