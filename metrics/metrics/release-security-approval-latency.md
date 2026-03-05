---
id: release-security-approval-latency
title: Security Approval Latency
phase: release
tags:
  - approvals
  - governance
related_tools:
  - jira
  - servicenow
depends_on:
  - test-security-coverage
thresholds:
  - name: Approval SLA
    value: "<= 2 days"
    description: Median time for security approval.
references:
  - https://owasp.org/www-project-application-security-verification-standard/
---

# Description

Track the median time to obtain security approval for release packages.

# Measurement

Measure the time between security review request and approval completion.

# Notes

Helps identify bottlenecks in release governance.
