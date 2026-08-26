---
'@mango/pmo': patch
---

修正 Maven 变更范围识别，跳过源码目录中的测试夹具 `pom.xml`，避免资源启动与模块改动触发错误的 Reactor 项目范围。
