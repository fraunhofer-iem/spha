---
id: release-change-risk-score
title: Release Change Risk Score
phase: release
tags:
  - release
  - risk
related_tools:
  - servicenow
  - jira
thresholds:
  - name: Risk threshold
    value: "<= 2"
    description: Average weighted risk score for approved releases.
depends_on:
  - test-security-coverage
  - release-security-approval-latency
  - code-secure-review-coverage
references:
  - https://owasp.org/www-project-application-security-verification-standard/
---

# Description

Combine test coverage, review completeness, and approval readiness into a weighted risk score for each release.

# Measurement

Score each release based on defined criteria and track the average score over time.

# Notes

A stable risk score signals consistent release readiness and control maturity.
