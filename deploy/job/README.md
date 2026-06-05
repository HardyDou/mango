# Mango Job PowerJob Deployment

## Modes

Mango Job supports one Mango UI/API contract and multiple runtime layouts:

- `external`: PowerJob Server is operated outside Mango. Mango starts only the Worker.
- `embedded-single`: Mango startup scripts manage a local PowerJob Server container or sidecar, and the Mango process starts the Worker.
- `embedded-cluster`: every Mango node may start a Worker, while all nodes point to the same PowerJob Server cluster.

Embedded mode does not import PowerJob Server source code into the Mango Spring Boot 3 application context. PowerJob Server must run as the official image, release process, container, or sidecar.

## Database

Mango Job governance tables are managed by Mango Flyway in `mango_job`.
PowerJob internal tables are managed only by PowerJob Server.

Supported layouts:

- `mango` + `mango_job`: recommended default. PowerJob internal tables may co-locate in `mango_job`.
- `mango` + `mango_job` + `powerjob`: higher isolation. PowerJob Server uses a separate database or schema.
- `primary` fallback: development or explicitly accepted small deployments only.

Mango must not query or modify PowerJob internal tables.

## Start PowerJob Server

```bash
cp deploy/job/powerjob.env.example deploy/job/.env
docker compose --env-file deploy/job/.env -f deploy/job/docker-compose.powerjob.yml up -d
```

Host network mode is an override for local environments where the Worker cannot be reached through Docker port mapping:

```bash
docker compose --env-file deploy/job/.env \
  -f deploy/job/docker-compose.powerjob.yml \
  -f deploy/job/docker-compose.powerjob-host.yml \
  up -d
```

Default endpoints:

- PowerJob Server HTTP: `127.0.0.1:7700`
- PowerJob Server Akka: `127.0.0.1:10086`
- PowerJob MySQL: `127.0.0.1:33306`

## Mango Configuration

Use one of the sample profiles as an external config file:

```bash
SPRING_CONFIG_ADDITIONAL_LOCATION=file:deploy/job/application-job-external.yml \
POWERJOB_APP_ID=1 \
POWERJOB_APP_NAME=mango-job \
POWERJOB_APP_PASSWORD=change-me \
POWERJOB_SERVER_ADDRESS=127.0.0.1:7700 \
scripts/dev-workspace.sh backend
```

PowerJob `server-addresses` must use `host:port` format. Do not use `http://host:port`; the PowerJob Worker prepends the protocol internally.

## Cluster Rules

- Multiple Mango nodes can run Workers for the same `app-name`.
- All Mango nodes in one deployment must point to the same PowerJob Server cluster.
- Do not start isolated PowerJob Server instances per Mango node unless they form one PowerJob Server cluster and share the same PowerJob database.
- This avoids multiple independent schedulers dispatching the same Mango job.
