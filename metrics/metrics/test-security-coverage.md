---
id: test-security-coverage
title: Security Test Coverage
phase: test
tags:
  - testing
  - coverage
related_tools:
  - zap
  - burp-suite
depends_on:
  - build-dependency-vulnerability-rate
thresholds:
  - name: Coverage target
    value: ">= 85%"
    description: Percentage of security test cases executed.
references:
  - https://owasp.org/www-project-web-security-testing-guide/
---

# Description

Measure coverage of security-focused test cases in the test suite.

# Measurement

Calculate the percentage of required security tests executed in CI.

# Notes

Helps ensure regression tests cover known security scenarios.
