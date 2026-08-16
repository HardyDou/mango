---
'@mango/cli': patch
---

Allow Mango release commands to retain high-volume Maven and npm build output without terminating candidate preparation at Node's default child-process buffer, and validate mixed Release Changeset intent against the plan's committed source boundary instead of its machine-generated version projections.
