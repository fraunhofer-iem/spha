---
id: test-security-regression-coverage
title: Security Regression Coverage
phase: test
tags:
  - testing
  - regression
related_tools:
  - junit
  - zap
thresholds:
  - name: Regression target
    value: ">= 85%"
    description: Percentage of security test cases executed in regression runs.
depends_on:
  - code-secure-review-coverage
  - test-security-coverage
  - build-dependency-vulnerability-rate
references:
  - https://owasp.org/www-project-web-security-testing-guide/
---

# Description

Measure how much of the security test suite is exercised in every regression run.

# Measurement

Calculate the percentage of security test cases executed in nightly or pre-release regression pipelines.

# Notes

High regression coverage prevents known vulnerabilities from resurfacing.
