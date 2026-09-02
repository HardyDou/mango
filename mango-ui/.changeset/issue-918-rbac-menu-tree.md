---
'@mango/admin-shell': patch
'@mango/auth': patch
'@mango/rbac': patch
'@mango/system': patch
---

Fix role menu assignment hydration so authorized parent directories do not select unauthorized sibling menus, while preserving required half-checked ancestors when saving. Default the admin experience to its single available tenant, clarify tenant management terminology, mark organization roots as tenant roots, rename admin branding to website configuration, and standardize the public configuration endpoint on the PUBLIC access contract.
