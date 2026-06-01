# Monolith Topology

The generated backend starts full Mango Admin capabilities in one Spring Boot process through `io.mango:mango-admin-starter`.

Frontend runtime config should use local modules:

```bash
cp frontend/public/runtime-config.monolith.json frontend/public/runtime-config.json
```
