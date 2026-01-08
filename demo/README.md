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

### Starting GitLab

Start the GitLab container:

```bash
docker compose up -d
```

GitLab will be accessible at:
- `http://localhost:1234`
- `http://gitlab.spha.demo:1234`

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

3. Select **Linux** as the operating system and create the runner

4. Copy the generated **runner token**

5. Export the token as an environment variable:
   ```bash
   export RUNNER_TOKEN=your-token-here
   ```

6. Start the runner container:
   ```bash
   docker compose -f gitlab-runner-compose.yml up -d
   ```

7. Register the runner **once** with:
   ```bash
   ./registerRunner.sh
   ```

### Example Pipeline Configuration

An example GitLab CI/CD pipeline configuration is provided in `demo/.gitlab-ci.yml`. This pipeline demonstrates:

- **Security scanning** with OSV Scanner and TruffleHog
- **Artifact collection** from security tools
- **Integration placeholder** for SPHA CLI to collect and send results to the SPHA server

To use this pipeline in your GitLab project, copy `.gitlab-ci.yml` to the root of your repository.

### Notes

- The initial root password file is automatically deleted 24 hours after installation
- GitLab data persists in the `$GITLAB_HOME` directory across container restarts
- Ports exposed:
  - `1234`: HTTP (GitLab web interface)
  - `4443`: HTTPS
  - `2222`: SSH
