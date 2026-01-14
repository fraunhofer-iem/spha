# Demo Environment Setup

## GitLab Setup

This demo environment includes a GitLab instance using Docker Compose.

### Prerequisites

Set the `GITLAB_HOME` environment variable to specify where GitLab data will be stored:

```bash
export GITLAB_HOME=/path/to/your/gitlab/data
```

For example:

```bash
export GITLAB_HOME=$HOME/gitlab
```

### Name Resolution

For the GitLab runner to communicate with GitLab, you need to add a hostname entry to your `/etc/hosts` file:

```bash
sudo sh -c 'echo "127.0.0.1 gitlab.spha.demo" >> /etc/hosts'
```

Or manually add this line to `/etc/hosts`:

```
127.0.0.1 gitlab.spha.demo
```

### Starting the Environment

**Important:** Start the services in the correct order to ensure proper Docker networking:

1. First, start the main SPHA stack from the repository root to create the shared network:
   ```bash
   docker compose up -d
   ```

2. Then, start the GitLab demo environment:
   ```bash
   cd demo
   docker compose up -d
   ```

This order is necessary because the demo environment relies on the `spha-network` Docker network created by the main compose file. The GitLab runner is configured to join this network, allowing CI/CD jobs to communicate with the SPHA container using the hostname `spha` (e.g., `http://spha:8080`).

GitLab will be accessible at:

- `http://localhost:80`
- `http://gitlab.spha.demo:80`

### Initial Login

1. Wait for GitLab to fully initialize (this may take several minutes on first startup)

2. Retrieve the initial root password:
   ```bash
   docker exec -it gitlab grep 'Password:' /etc/gitlab/initial_root_password
   ```

3. Login with:
    - **Username:** `root`
    - **Password:** (use the password retrieved from the command above)

4. **Important:** Change the root password immediately after first login

### Runner Registration

1. In GitLab, navigate to **Admin Area** → **CI/CD** → **Runners**

2. Click **New instance runner**

3. Select **Linux** as the operating system and create the runner and use the tag "shared" to match the tag from the
   example pipeline configuration.

4. Copy the generated **runner token**

5. Export the token as an environment variable:
   ```bash
   export RUNNER_TOKEN=your-token-here
   ```

6. Start the runner container:
   ```bash
   docker compose up runner -d
   ```

7. Register the runner **once** with:
   ```bash
   ./registerRunner.sh
   ```
   
### Prepare SPHA CLI Runner GitLab component

Follow the steps described in spha-component/README.md to setup the GitLab SPHA component.

### Analzying a Demo Project

1. Create a new project in GitLab you with to analyze.
2. Copy the file `demo/.gitlab-ci.yml` to the newly created repo.

This pipeline demonstrates:

- **Security scanning** with OSV Scanner and TruffleHog
- **Artifact collection** from security tools
- **Runs SPHA** against the collected artifacts and reports the results to the SPHA dashboard at http://localhost:8080

### Notes

- The initial root password file is automatically deleted 24 hours after installation
- GitLab data persists in the `$GITLAB_HOME` directory across container restarts
- Ports exposed:
    - `80`: HTTP (GitLab web interface)
    - `4443`: HTTPS
    - `2222`: SSH
