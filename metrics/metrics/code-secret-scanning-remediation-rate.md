---
id: code-secret-scanning-remediation-rate
title: Secret Scanning Remediation Rate
phase: code
tags:
  - secrets
  - code-quality
related_tools:
  - github
  - gitleaks
thresholds:
  - name: Remediation SLA
    value: "<= 3 days"
    description: Time to remediate a detected secret in source control.
depends_on:
  - plan-security-requirements-coverage
  - code-secure-review-coverage
references:
  - https://owasp.org/www-project-top-ten/
---

# Description

Track how quickly detected secrets in source control are removed, rotated, or revoked.

# Measurement

Measure the median time between secret detection and remediation across all repositories.

# Notes

Fast remediation reduces the window of exposure for leaked credentials.
