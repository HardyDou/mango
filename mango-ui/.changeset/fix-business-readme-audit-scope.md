---
'@mango/pmo': patch
'@mango/cli': patch
---

Fix packaged README audits so Mango source checkouts retain their fixed platform README gate while business consumers resolve `mango.config.json.paths`, audit capability-map-owned repository documents, and fail closed on wrong roots or empty scopes. Align the generated full-project README with the exact PMO, CLI and Maven release tuple.
