---
'@mango/cli': patch
---

Keep the published Maven source version stable when an in-progress release plan is recalculated, and make registry doctor probe the same exact published Mango BOM POM through the Maven publish and consume roles instead of treating a non-browsable repository root as unavailable.
