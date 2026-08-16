---
'@mango/admin': patch
'@mango/admin-extension': patch
'@mango/admin-pages': patch
'@mango/cli': patch
'@mango/common': patch
'@mango/file': patch
'@mango/link': patch
'@mango/notice': patch
'@mango/rbac': patch
'@mango/system': patch
---

Fix the Issue #805 full-consumer regressions and make the Mango npm release graph acyclic. The release adds the FE1 `@mango/admin-extension` contract, keeps the published Admin Pages subpaths as same-instance compatibility re-exports, moves File and newly generated business registrars off the FE3 dependency, restores CMS, system-event, and hidden Notice settings routes in full projects, removes the confirmed frontend warnings, and adds candidate-package consumer contracts.
