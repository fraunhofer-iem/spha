# SPHA CLI GitLab Component

This directory contains a [GitLab CI/CD Component](https://docs.gitlab.com/ci/components/) that provides a reusable template for running the SPHA CLI tool in your GitLab pipelines.

## What is a GitLab Component?

GitLab Components are reusable CI/CD configurations that can be shared across multiple projects. They allow you to define common pipeline jobs once and reference them from other projects, promoting consistency and reducing duplication.

## Component Structure

- `templates/cli-component.yml` - The component definition that provides the `run-spha-cli` job
- `README.md` - This documentation file

## Setup Instructions

To make this component available for use in other GitLab projects:

### 1. Create a Component Repository

Create a new repository in your GitLab instance (e.g., in the `spha-component` component in the group `spha` (you need to edit the templates if you choose different names)):
- **Important:** Make the repository **public** or ensure consuming projects have access
- Add a non-empty repository description (required for CI/CD Catalog)

### 2. Push Component Files

Copy the contents of this directory (`demo/spha-component`) to the newly created repository:

```bash
# In your GitLab repository
cp -r /path/to/spha/demo/spha-component/* .
git add .
git commit -m "Initial component setup"
git push
```

### 3. Add SPHA CLI Artifacts

Build and add the SPHA CLI executable to the repository:

```bash
# From the SPHA project root
./gradlew installDist

# Copy the built artifacts to your component repository
cp -r build/install/spha-cli artifacts/
```

### 4. Enable CI/CD Catalog

In your GitLab component repository:
1. Navigate to **Settings** � **General** � **Visibility, project features, permissions**
2. Check the box for **CI/CD Catalog resource**
3. Save changes

### 5. Create a Release

Create a version tag to publish the component:

```bash
git tag -a 1.0.0 -m "Initial release"
git push origin 1.0.0
```

A `create-release` CI job should automatically start and publish the component to the CI/CD Catalog.

### 6. Verify Publication

Check that your component appears in:
- **CI/CD** � **CI/CD Catalog** in your GitLab instance

## Using the Component

Once published, other projects can use this component in their `.gitlab-ci.yml`:

```yaml
include:
  - component: gitlab.spha.demo/your-namespace/spha-component/cli-component@1.0.0

stages:
  - security-scan
  - test

# Your security scanning jobs
osv-scanner:
  stage: security-scan
  # ... (OSV Scanner configuration)

trufflehog:
  stage: security-scan
  # ... (TruffleHog configuration)

# The SPHA CLI component job
run-spha-cli:
  inputs:
    stage: test
    server_url: http://spha:8080/api/report
    tool_result_dir: .
```

## Component Inputs

The component accepts the following input parameters:

- `stage` (default: `"test"`) - The pipeline stage where the job runs
- `server_url` (default: `"http://spha:8080/api/report"`) - The SPHA server endpoint for reporting results
- `tool_result_dir` (default: `"."`) - The directory containing tool result JSON files

## Troubleshooting

### Runner Network Configuration

If the runner cannot communicate with the SPHA server, you may need to configure the runner to use the same Docker network:

1. Edit the runner's `config.toml` file
2. Add `network_mode = "spha-network"` to the runner configuration
3. Restart the runner container

### Repository Path

If you're using a different GitLab instance or repository structure, update the repository path in `templates/cli-component.yml`:

```yaml
git clone "http://your-gitlab-instance/your-namespace/spha-component.git" comp-repo
```
