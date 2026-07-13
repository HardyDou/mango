# 模块级架构债务治理设计

## 1. 背景与目标

Mango 已用全 Reactor 架构报告和递减预算阻止新增违规，但 schema v3 只保存全局规则计数和问题身份。它能回答“全仓是否反弹”，不能可靠回答“某个 Maven 模块还剩多少、这次是否只降低了目标模块、是否把违规转移到其它模块”。

本次目标是在不削弱全 Reactor 门禁的前提下，为历史债务增加模块归属、模块查询和模块递减能力，使维护者可以按模块持续清理，并让机器同时阻止模块内新增和跨模块转移。

## 2. 方案比较与决策

### 2.1 继续只维护全局预算

- 优点：实现最少，现有门禁不变。
- 缺点：无法分配模块清债任务，无法直接输出模块剩余量，也不能从预算本身证明债务没有跨模块转移。
- 结论：不采用，因为不能满足按模块治理目标。

### 2.2 只维护模块预算并取消全局终检

- 优点：单模块检查速度快，输出直接。
- 缺点：模块识别遗漏、模块重命名或跨模块移动可能形成逃逸路径；不能继续证明全仓报告完整。
- 结论：不采用，因为会降低现有门禁强度。

### 2.3 模块预算与全 Reactor 终检并存

- 优点：一次全 Reactor 扫描产生完整事实，随后可快速查询或递减一个或多个模块；全局聚合与模块明细相互校验，能够阻止模块内新增和跨模块转移。
- 缺点：报告和预算需要升版，首次迁移必须重新生成完整报告。
- 结论：采用。全 Reactor 扫描仍是 CI 权威事实；模块检查复用同一报告，不重复运行 Maven 全量构建。

## 3. 报告模型

架构报告升级为 schema v2。

### 3.1 Reactor 模块目录

报告增加 `modules` 数组。每个模块至少包含：

- `moduleKey`：相对 Maven 根目录的稳定路径；根项目使用 `.`。
- `groupId`：Maven groupId。
- `artifactId`：Maven artifactId。

`moduleKey` 是预算唯一键；`artifactId` 只作为命令选择器。重复 moduleKey 或重复 Maven GAV 必须失败；artifactId 在不同 groupId 下重复时允许进入报告，但按 artifactId 选择会因歧义失败。

### 3.2 问题归属

`dependencyIssues`、`archUnitIssues`、`pmdIssues` 和 `blockingIssues` 中每条问题增加 `moduleKey`：

- 依赖规则归属依赖边的来源 artifact。
- ArchUnit 规则归属 subject 中的来源类；来源类不可识别时再使用被引用类。
- PMD 规则归属违规源码所在 Maven 项目。
- 任一问题无法唯一归属时报告生成失败，禁止写入 `unknown` 后继续。

正例：`mango-system-starter -> mango-org-core` 的依赖问题归属 `mango-system-starter` 所在路径。

反例：按规则数量平均分配到领域目录。错误原因：无法追溯到真实问题，跨模块移动也无法识别。

## 4. 预算模型

债务预算升级为 schema v4，同时保留顶层 `engines`、`rules`、`identities` 和 `totalIssueCount`，供全局比较和 Maven changed 模式读取。

新增 `modules` 对象，每个 `moduleKey` 保存：

- Maven 坐标身份。
- 模块问题总数。
- 按 engine 的计数。
- 按 ruleId 的计数。
- 稳定问题 identity 多重集合。

顶层聚合必须由全部模块明细精确求和。模块合计、顶层合计或报告合计任一不一致时 fail-closed。

正向要求：同一 identity 只能属于一个模块；模块与全局 identity 多重集合必须完全一致。

禁止：手工只改顶层总数、只删模块明细、把一个模块的减少抵消另一个模块的新增，或保留无法归属的历史身份。

## 5. 命令行为

保持现有全局命令兼容，并增加可重复的 `--module <selector>`：

```bash
node mango-pmo/tools/check-architecture-debt-budget.mjs \
  --module mango-platform/mango-system

node mango-pmo/tools/check-architecture-debt-budget.mjs \
  --module mango-system-core \
  --write
```

选择器按以下顺序解析：

1. `moduleKey` 路径，选择精确模块及其全部 Maven 子模块。
2. 唯一 artifactId，选择单个 Maven 模块。

未知选择器、歧义 artifactId、空选择结果或重复解析必须失败并列出原因。

### 5.1 检查

- 不传 `--module`：执行现有全局 base -> PR -> current 三方校验。
- 传 `--module`：只输出所选模块的规则、身份和增减结果，但输入仍必须是完整全 Reactor 报告。
- 模块出现任一 rule 或 identity 增加时失败；即使模块总数不变也失败。
- 模块有减少但预算未降低时返回 `ratchet-required`。

### 5.2 写入

- 全局 `--write`：用完整报告重建全部模块和顶层聚合。
- `--module ... --write`：只替换所选模块，保留其它模块预算，再重新计算顶层聚合。
- 模块写入只允许持平或下降；`--module` 与 `--accept-increase` 互斥。规则升级造成的存量增加必须走全局治理审批，不能伪装成模块清债。

## 6. CI 与性能

CI 继续只运行一次完整 Maven Reactor 架构扫描，再执行一次全局预算检查。模块检查读取同一 JSON 报告，禁止为每个模块重复运行全量 Maven 构建。

本地清债流程为：

1. 运行一次完整架构扫描。
2. 使用一个或多个 `--module` 查看目标模块。
3. 修复目标模块并重新生成完整报告。
4. 使用 `--module ... --write` 降低该模块预算。
5. 提交前执行无 `--module` 的全局检查。

这样优化的是重复分析和任务分配，不通过部分 Reactor 报告换取速度。后续如需真正的模块级 Maven 扫描，必须另行设计其完整性证明，不能复用本次预算写入口。

## 7. 失败处理

- schema v3 不得继续作为当前预算；必须用 schema v2 报告执行一次全局 `--write` 完成迁移。
- 迁移 PR 的 Git base 可以是 schema v3。checker 只读取其顶层全局字段完成 base -> PR 比较；模块级 `--base-ref` 要求 base 已是 schema v4。Maven changed gate在迁移窗口内只读兼容 schema v3/v4 顶层 identities，不能借兼容入口写回 v3。
- 报告缺模块目录、问题缺 `moduleKey`、模块键不存在、坐标冲突或聚合不一致时退出码为 2。
- 检测到新增、替换、跨模块移动或未写回的减少时退出码为 1。
- `--write` 使用临时文件和原子 rename，失败时保留旧预算。
- `--base-ref` 继续从 Git 读取主分支预算，模块字段也参与三方比较。

## 8. 测试与验收

必须覆盖：

- schema v2 报告能为依赖、ArchUnit 和 PMD 问题生成唯一模块归属。
- schema v4 全局初始化、校验、递减和受治理增加保持原行为。
- 指定单模块和目录前缀检查成功。
- 模块减少后要求写回，并只更新目标模块。
- 模块内新增、同规则身份替换和跨模块移动均失败。
- 未知/歧义模块、缺失归属、模块聚合篡改和部分 Reactor 报告均失败。
- Maven changed 模式能读取 schema v4 顶层 identities，现有 no-new-violations 行为不回退。
- PMO package、starter baseline、README、CHANGELOG、能力地图和版本文档快照同步。

## 9. 非目标

- 本次不自动修复 9,038 条历史问题。
- 本次不允许部分 Reactor 报告修改正式预算。
- 本次不降低 `pmo-doc-check`、全 Reactor 扫描或全局三方校验。
- 本次不按 Java package、团队或业务标签建立第二套债务归属体系。
