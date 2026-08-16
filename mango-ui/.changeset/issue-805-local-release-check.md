---
'@mango/cli': patch
---

Make the final-head local release gate reuse the Runner's Release-only classification, so deterministic version projections run the release plan check without requiring duplicate fake Changesets while ordinary source changes retain the full Changeset intent check.
