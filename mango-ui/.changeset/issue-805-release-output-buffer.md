---
'@mango/cli': patch
---

Allow Mango release commands to retain high-volume Maven and npm build output without terminating candidate preparation at Node's default child-process buffer, validate mixed Release Changeset intent against the plan's committed source boundary instead of its machine-generated version projections, and verify all sealed Maven coordinates in one mirror-isolated candidate consumer instead of launching one Maven process per coordinate.
