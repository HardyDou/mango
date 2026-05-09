# IP Location Data

Place the runtime ip2region xdb file here:

```text
config/ip-location/ip2region_v4.xdb
```

The monolith config points to this file by default. If the file is absent, Mango still starts because `mango.ip-location.fail-fast=false`, and log locations fall back to `未知`.

Do not commit production xdb data files. Update the data file by replacing it and restarting the service.
