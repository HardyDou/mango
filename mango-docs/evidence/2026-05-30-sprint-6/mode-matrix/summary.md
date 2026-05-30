# Mango Frontend Mode Matrix E2E

- Temporary root: /var/folders/v3/xy1sw5vj1czc1qkwhhh0g4v40000gn/T/mango-mode-matrix-e2e-sf1njl
- Modes: local, micro, mixed
- Checks: package build, generated project template check, install, typecheck, build, browser smoke
- UI layout checks:
  - local: business list page, search, actions, table, pagination, vertical order, horizontal overflow
  - micro/mixed: shell runtime outlet, real remote page, runtime version, health check, menu, permissions, theme, business data, horizontal overflow
  - micro/mixed negative: remote health check failure renders Mango standard error panel

| Mode | Runtime decision | Layout result | Evidence |
|---|---|---|---|
| local | LOCAL_ROUTE/local | passed | local/layout-report.json, local/frontend-mode.png |
| micro | MICRO_ROUTE/micro | passed | micro/layout-report.json, micro/frontend-mode.png, micro/remote-failure-report.json, micro/remote-failure.png |
| mixed | MICRO_ROUTE/micro | passed | mixed/layout-report.json, mixed/frontend-mode.png, mixed/remote-failure-report.json, mixed/remote-failure.png |
