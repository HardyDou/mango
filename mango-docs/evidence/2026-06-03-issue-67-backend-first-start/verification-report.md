# Issue 67 Verification Report

## Objective

Fix #67: generated enterprise projects should provide a reliable first backend startup flow after adding local business modules.

## Root Cause

After `mango module add`, `backend/app/pom.xml` depends on the generated local business starter, for example:

- `com.acme.issue67:procurement-order-starter:1.0.0-SNAPSHOT`

Running `mvn -f backend/app/pom.xml spring-boot:run` directly builds only the app module. Maven is outside the parent reactor and tries to resolve the local business starter from the remote Maven repository, so first startup fails before the application starts.

`mvn -f backend/pom.xml -DskipTests package` is also insufficient for this direct app startup mode because `package` does not install local artifacts into the user Maven repository.

## Change

Generated full preset projects now include:

- `scripts/backend-dev.sh`

The script:

1. Runs `mvn -f backend/pom.xml -DskipTests install`.
2. Starts the app with `mvn -f backend/app/pom.xml spring-boot:run`.
3. Supports `MANGO_BACKEND_PORT`, defaulting to `5555`.

CLI next steps and generated README now point business developers to `scripts/backend-dev.sh`.

## Verification Commands

CLI template check:

```bash
node mango-ui/packages/mango-cli/scripts/check-cli.mjs
```

Failure reproduced before fix:

```bash
node mango-ui/packages/mango-cli/src/index.mjs init acme-issue67 --preset full --package com.acme.issue67 --force
node mango-ui/packages/mango-cli/src/index.mjs module add procurement-order --aggregate procurement --module-name 采购订单 --project-dir /tmp/mango-issue-67-repro/acme-issue67
mvn -f backend/app/pom.xml -DskipTests spring-boot:run
```

Observed failure:

```text
Could not find artifact com.acme.issue67:procurement-order-starter:jar:1.0.0-SNAPSHOT
```

Fixed generated project verification:

```bash
node mango-ui/packages/mango-cli/src/index.mjs init acme-issue67-fixed --preset full --package com.acme.issue67fixed --force
node mango-ui/packages/mango-cli/src/index.mjs module add procurement-order --aggregate procurement --module-name 采购订单 --project-dir /tmp/mango-issue-67-fixed/acme-issue67-fixed
test -x scripts/backend-dev.sh
mysql -uroot -e 'CREATE DATABASE IF NOT EXISTS `acme-issue67-fixed` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
MANGO_BACKEND_PORT=19077 scripts/backend-dev.sh
curl -sS http://127.0.0.1:19077/actuator/health
```

## Result

Passed.

- Generated `scripts/backend-dev.sh` exists and is executable.
- CLI next steps print `scripts/backend-dev.sh`.
- The script installed all local backend modules, including `procurement-order-starter`.
- The generated backend started successfully on port `19077`.
- Health endpoint returned `{"status":"UP"}`.

## Notes

Database creation remains a required explicit environment step and is documented in the generated README. The first run without creating the database failed with `Unknown database 'acme-issue67-fixed'`, which is a separate environment prerequisite, not the #67 local module dependency issue.
