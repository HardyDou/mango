---
'@mango/pmo': patch
---

架构债预算校验的 `--base-ref` 支持 `HEAD^`、`origin/main~2` 这类 git revision 语法，修复 `Mango Architecture Debt Inventory` 定时任务长期失败（`invalid --base-ref: <sha>^`）。
