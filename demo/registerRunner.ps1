#!/usr/bin/env pwsh
#
# Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
#
# Licensed under the MIT license. See LICENSE file in the project root for details.
#
# SPDX-License-Identifier: MIT
# License-Filename: LICENSE
#
# Register runner

Write-Host "runner token: $env:RUNNER_TOKEN"

docker compose run --rm runner register `
  --non-interactive `
  --url "http://gitlab.spha.demo" `
  --token "$env:RUNNER_TOKEN" `
  --executor "docker" `
  --docker-image alpine:latest `
  --description "safe-docker-runner" `
  --docker-privileged `
  --docker-extra-hosts "gitlab.spha.demo:host-gateway" `
  --docker-image "docker:29-cli"