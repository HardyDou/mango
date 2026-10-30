---
'@mango/admin-extension': patch
'@mango/admin-pages': patch
'@mango/admin-shell': patch
'@mango/notice': patch
---

Fix Notice ROUTE actions that target authorized dynamic ShellMenu paths. Admin Shell now resolves Notice targets from the current dynamic menu tree while custom Notice interactions remain host-handled events.
