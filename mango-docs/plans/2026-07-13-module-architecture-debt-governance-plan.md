# 模块级架构债务治理实施计划

## 1. 目标与范围

在现有全 Reactor 架构门禁上增加可靠模块归属、模块查询和模块递减预算；保留全局 no-new-violations 与 base -> PR -> current 三方检查。本计划不清理具体历史违规，不引入部分 Reactor 预算写入。

## 2. 实施步骤

### 2.1 架构报告 schema v2

- 在 Maven architecture goal 中建立 Reactor 模块目录。
- 为 dependency、ArchUnit、PMD 和 blocking issue 生成唯一 `moduleKey`。
- 无法归属、重复模块键或重复 GAV 时 fail-closed。
- 更新 Maven plugin 单元测试，覆盖三类归属和 schema v3/v4 基线只读兼容。

### 2.2 债务预算 schema v4

- 扩展 `check-architecture-debt-budget.mjs`，生成模块明细和全局聚合。
- 增加可重复 `--module`，支持 moduleKey、唯一 artifactId 和目录前缀。
- 增加模块定向检查与原子递减写入。
- 禁止模块模式接受预算增加，校验模块与全局聚合完全一致。
- 保留 schema v3 Git base 迁移读取，禁止 schema v3 继续作为当前预算。

### 2.3 真实基线迁移

- 完成一次完整 Maven Reactor 架构扫描，生成 schema v2 报告。
- 全局写入 schema v4 正式预算。
- 用至少一个领域目录和一个 leaf artifact 执行模块查询。
- 执行全局 `--base-ref`，证明迁移没有新增或替换历史 identity。

### 2.4 规范与使用说明

- 更新 PMO 历史债务规则的正向、禁止、正例、反例和机器判定。
- 更新规则索引、架构债务 README、Mango tools README、能力地图和根 CHANGELOG。
- 同步 PMO npm bundle、business starter baseline、Skill eval 和版本文档快照。

### 2.5 投产收口

- 执行 PMO、Maven plugin、CLI、starter、README、文档站和发布门禁。
- 审计最终 diff，提交并创建 follow-up PR。
- 等待 required `pmo-doc-check`，合并 `main`。
- 发布 Maven 1.0.16、`@mango/pmo@1.1.0`、`@mango/cli@1.0.68`，完成 registry 和干净业务项目验收。

## 3. 验证命令

```bash
node --test mango-pmo/tests/architecture-debt-budget.test.mjs
mvn -f mango/pom.xml -pl :mango-maven-plugin -am test
node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-platform/mango-system
node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref "$(git merge-base HEAD origin/main)"
node mango-pmo/tools/check-governance-intent.mjs
node mango-business-starter/scripts/check-template.mjs
node mango-ui/packages/mango-cli/scripts/check-cli.mjs
npm --prefix mango-docs run docs:build
```

## 4. 完成标准

- 每条现有架构债务都能唯一归属模块。
- 模块内新增、身份替换和跨模块移动均被机器拒绝。
- 目标模块减少后可以独立降低预算，非目标模块保持原值。
- 全局检查继续证明完整 Reactor、全局身份和模块聚合一致。
- README、CHANGELOG、starter、PMO package 和发布快照与实现一致。
- follow-up PR 合并且三类制品发布、回查、消费验收全部成功。
