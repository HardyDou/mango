---
'@mango/cli': patch
---

`dev start` 在构建 Spring Boot reactor 前，先按同一开发 revision 安装 reactor 父 POM 及其 import 的 BOM POM，使干净环境与本地仓库缓存异常时也能解析父链。
