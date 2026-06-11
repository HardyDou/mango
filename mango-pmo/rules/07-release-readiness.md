# 发布就绪门禁

## 1. 适用范围

适用于 Mango 发布、发版验证、公共前端包、后端 Maven 模块、CLI、starter、模板和新增模块交付。

## 2. 必须执行

- 发布前必须执行 `scripts/check-release-readiness.sh`。
- 发布 CLI、starter、模板或新增模块前，必须执行 `scripts/check-release-readiness.sh --check-registry`。
- 发布包含新增特性的新版本前，必须列出本次新增特性的验证清单，并确认每一项均为 `PASS`。
- 新增或调整业务可消费模块时，必须同步判断 npm 包、Maven 模块、CLI `release-versions.json`、业务模板依赖、admin 样式聚合和 starter 依赖是否需要发布。
- 发布报告必须列出实际发布对象、版本、新特性、升级说明、仓库验证结果和业务消费入口验证结果。

## 3. 禁止事项

- 禁止只发布 npm 包或 Maven 模块，却不判断 CLI、模板、starter 是否需要同步。
- 禁止 CLI lock 指向 registry 不存在的版本。
- 禁止新增模块未接入发布就绪门禁就声明业务可消费。
- 禁止新增特性存在未验证、`FAIL`、`BLOCKED` 或未解释 `EXCEPTION` 时发布新版本。
- 禁止发布新版本但不维护面向业务的升级说明或变更说明。
