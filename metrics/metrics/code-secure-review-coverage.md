---
id: code-secure-review-coverage
title: Secure Code Review Coverage
phase: code
tags:
  - code-review
  - governance
related_tools:
  - github
  - gitlab
depends_on:
  - plan-security-requirements-coverage
thresholds:
  - name: Coverage target
    value: ">= 95%"
    description: Percentage of pull requests that include a secure review.
references:
  - https://owasp.org/www-project-code-review-guide/
---

# Description

Measure the percentage of pull requests that include a documented secure code review.

# Measurement

Count pull requests marked with a secure review label divided by all merged pull requests.

# Notes

Provides insight into adherence to secure review requirements.
