## SPHA Server

### Database Setup

The server requires a PostgreSQL database to function. The default configuration expects the following:

- **Database**: PostgreSQL
- **Host**: `localhost:5432`
- **Database Name**: `spha_db`
- **Username**: `postgres`
- **Password**: `postgres`

These settings can be configured in `server/src/main/resources/application.yaml`.

#### Starting the Database

The easiest way to start the required PostgreSQL database is using the included `compose.yml` file from the project root:

```bash
docker compose up db
```

This will start a PostgreSQL 18.1-alpine container with the correct configuration.

To start both the server and database together:

```bash
docker compose up
```

### Testing

**Note:** Docker must be available to run all tests successfully. The tests use Testcontainers to spin up a PostgreSQL database instance.

### Docker Build

To build the server as a Docker image, run from the project root:

```bash
docker build -f server/Dockerfile -t spha-server .
```

To build with a specific version:

```bash
docker build -f server/Dockerfile --build-arg VERSION=1.0.0 -t spha-server:1.0.0 .
```

To run the container:

```bash
docker run -p 8080:8080 spha-server
```

**Note:** The Dockerfile must be built from the project root (not the `server/` directory) as it requires access to `buildSrc/`, `lib/`, `cli/`, and other project files.

