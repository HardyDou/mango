---
'@mango/admin-shell': patch
'@mango/app-runtime': patch
---

修复 Shell 混合模式切换微应用页签时的 Wujie 生命周期竞态，确保已打开的 RBAC 页签恢复后仍显示页面内容。
