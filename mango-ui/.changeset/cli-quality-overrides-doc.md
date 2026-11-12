---
'@mango/cli': patch
---

README 补充业务项目调整 Java 质量门禁的三种方式：用 `mango.check.reportOnlyRules` 把指定规则降为只报告不阻断，用 `mango.check.checkstyle.cyclomaticComplexityMax` 与 `mango.check.checkstyle.fileLengthMax` 覆盖阈值，仍可用 `mango.check.pmdRulesets` 引入自定义 PMD 规则集，无需复制整份规则文件。CLI 命令、生成产物和模板内容不变。
