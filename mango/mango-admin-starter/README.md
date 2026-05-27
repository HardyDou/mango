# Mango Admin Starter

`mango-admin-starter` is the reusable backend admin assembly entry for business projects.

## Usage

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-admin-starter</artifactId>
    <version>${mango.version}</version>
</dependency>
```

Host applications still own Spring Boot runtime configuration such as `server.port`, datasource, context path and deployment profiles.

## Boundary

- This module only aggregates public Mango starters.
- This module does not provide controllers, domain services, database migrations or seed data.
- This module must not directly depend on Mango `*-core` modules.
- This module must not ship `application.yml`, `application.yaml` or `application.properties`.
