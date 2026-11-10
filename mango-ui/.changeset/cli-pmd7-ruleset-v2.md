---
'@mango/cli': minor
---

full 后端模板切换到 PMD 7 与 Mango Java 规则集 v2：`config/quality/pmd-mango.xml` 取代 P3C 2.1.1 规则集，生成项目的 `pom.xml` 使用 `maven-pmd-plugin 3.28.0` 与 parent 属性传入的规则集路径。`check-cli` 增加规则集文件与插件配置校验，生成项目仍可自调整规则集。
