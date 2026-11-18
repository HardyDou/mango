# Workflow 契约指纹与发布说明

业务项目升级 Mango 时，最常见的静默风险是 `mango-workflow-api` 的公开方法签名或 HTTP 端点发生了变化，而业务适配器仍然按旧签名编译或判断。`mango-workflow` 已经用 `WorkflowApiSurfaceContractTest` 把公开契约固定成两个 SHA-256 指纹，本页说明这两个指纹如何随发布说明交付给业务项目。

## 1. 两个指纹覆盖什么

| 指纹 | 覆盖范围 | 变化时机 |
|---|---|---|
| `api` | 8 个公开 `*Api` 接口的「方法名 → 泛型返回类型」 | 增删公开方法、改返回契约 |
| `web` | 8 个 Controller 的「verb + path + 方法名 → 返回类型 + `@ApiAccess`」 | 增删端点、改 path/verb、改权限码 |

两项都**不覆盖** DTO：给 VO、Query、Request 加字段，或者增删枚举值，指纹不变。

## 2. 发布说明里的固定字段

每个 Maven 批次要在 `CHANGELOG.md` 的发布段落里写入：

```yaml
workflow-contract-fingerprint:
  api:    <sha256>
  web:    <sha256>
  changed: none|api|web|both
  since:  <上一批次的两个指纹> | n/a

workflow-compatible-additions:
  - WorkflowTaskBusinessPageRequest: + businessTypes, processDefinitionKeys
```

第二块按类名维度组织，业务项目才能直接对应到自己的消费点；发布说明的字段要求以 [发布制品与版本同步规范](../../../mango-pmo/rules/10-release-artifacts.md) 第 6 节为准。

## 3. 生成方式

```bash
node mango-pmo/tools/workflow-contract-fingerprint.mjs
node mango-pmo/tools/workflow-contract-fingerprint.mjs --base v2026.11.17-maven-1.13.0-task-multi-filter-contract-gate-release
node mango-pmo/tools/workflow-contract-fingerprint.mjs --base <上一批次 tag> --json
```

- 不带 `--base` 时自动取「创建时间最新、且与当前提交不同的发布 tag」
- `changed` 与 `since` 由两次读取的常量比对得出，不要手写
- 契约源码（`*Api.java`、`*Controller.java`）改了而指纹常量没改时，工具会在输出里插入 `# WARN`：这表示 `Workflow API contract sentinel` 没在该提交上跑过，此时发布说明里的指纹不可信

发布前再校验一次发布段落是否真的带着这个块：

```bash
node mango-pmo/tools/workflow-contract-fingerprint.mjs \
  --verify-release-notes CHANGELOG.md --base <上一批次 tag>
```

校验会拒绝四种情况：块缺失、`api`/`web` 与当前代码不一致、`changed`/`since` 与计算结果不一致、契约源码改了但指纹常量没跟上。

## 4. 业务项目怎么用

| 指纹 `changed` | 兼容新增块 | 业务动作 |
|---|---|---|
| `none` | 空 | 直接升级 |
| `none` | 非空 | 只核对新增字段与枚举值的消费端 |
| `api` / `web` / `both` | 任意 | 逐行核对方法签名与端点 |

只改文档或测试的批次也要输出该字段并写明 `changed: none`，否则业务项目只能靠 diff 自行判断。

## 5. 已知边界

- `1.12.0` 及更早批次发布的指纹值**未经验证**：当时 `mango-workflow` 模块落在 Surefire 2.17 上，JUnit 5 用例从未执行（`Tests run: 0` 却 BUILD SUCCESS），常量是陈旧值。可信历史链从 `1.13.0` 开始。
- 指纹只校验「平台声明」，不校验「业务是否消费到了」；业务侧的类型不匹配仍要靠编译与回归测试发现。
