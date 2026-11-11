---
'@mango/cli': patch
---

full 后端模板的复杂度门禁调整为更贴近真实维护成本：`pmd-mango.xml` 移除 PMD 类级总圈复杂度检查，方法级复杂度继续由内置 Checkstyle `CyclomaticComplexity max=20` 约束。类内方法各自合规但方法数量多的解析器和聚合 Service 不再被类级累加判定阻断；NPath、布尔表达式复杂度和类级总复杂度仍不启用，需要类级约束的项目使用 `-Dmango.check.pmdRulesets` 引入自己的规则集。
