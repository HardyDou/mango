---
'@mango/cli': patch
---

full 后端模板的 `config/quality/pmd-mango.xml` 收敛为 Mango Java 规则集 v3：规则从 55 条减到 24 条，只保留能定位真实缺陷、资源泄漏、并发正确性、死代码和基础安全问题的检查。参数与局部变量命名、Javadoc 形式、NPath 复杂度、字面量抽取、微性能和笼统异常捕获不再进入 `gate=all` 门禁，避免高误报和与 Mango 架构规则冲突的检查阻断存量业务仓。业务项目仍可用 `-Dmango.check.pmdRulesets=<path>` 覆盖规则集，存量代码继续通过 `no-new-violations` 与 baseline 控制。
