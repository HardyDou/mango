# Mango PMO Baseline

Source:

- Mango commit: generated from current Mango source
- Mango CLI version: generated from current `@mango/cli`
- Synced at: generated during `mango pmo sync`

## 1. 能力定位

`business-pmo/mango-baseline` 是生成业务项目携带的 Mango PMO 可执行快照，提供 preflight、交付台账检查和规则索引。主要使用者是脱离 Mango 源码后的业务仓 Agent 和交付人员。

## 2. 适用场景

- 业务仓执行 PMO preflight。
- 业务仓按设计和交付台账推进任务。
- 业务仓在没有 Mango 源码的情况下读取 baseline 规则。
- Mango baseline 升级任务同步规则和工具。

## 3. 不适用场景

- 不作为业务项目随意编辑区。
- 不保存业务需求、业务设计或业务交付证据。
- 不替代 Mango 主仓 `mango-pmo` 的长期规范源。

## 4. 模块边界

baseline 是快照。Mango 主仓负责长期规范演进，业务仓通过 CLI 或升级任务同步 baseline；业务项目自有规则应放在 baseline 外。

## 5. 接入方式

preflight：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<task>" \
  --paths "<paths>"
```

交付台账检查：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs \
  --design <design.md> \
  --ledger <ledger.md> \
  --mode plan
```

## 6. 配置项

规则索引：`rules/index.json`。

已发现索引结构：

- always：`rules/00-dev-flow.md`、`rules/01-delivery-contract.md`、`rules/03-ai-coding-redlines.md`
- bundles：`backend`、`frontend`、`pmo`、`devEnvironment`、`product`、`deliveryContract`、`versioning`、`security`

preflight 参数支持 `--role`、`--phase`、`--task`、`--paths`、`--json`。

## 7. 对外接口 / 扩展点

- `tools/pmo-preflight.mjs`：根据 role、phase、task、paths 输出 Must read。
- `tools/delivery-contract-check.mjs`：校验设计和交付台账。
- `rules/index.json`：规则路由索引。
- `templates/**`：baseline 内的模板资产。

## 8. 数据库 / 初始化数据

本目录不包含数据库 migration。初始化数据是模板生成的规则、工具和模板文件。

## 9. 菜单 / 权限 / 租户

本目录不提供菜单、权限资源或租户数据。

## 10. 验证方式

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs --role dev --phase develop --task "baseline smoke" --paths "backend"
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs --help
node -e "JSON.parse(require('fs').readFileSync('business-pmo/mango-baseline/rules/index.json','utf8')); console.log('index ok')"
```

## 11. 业务接入最小闭环

业务仓中从项目根目录执行 baseline 工具，不从 `business-pmo/mango-baseline` 子目录执行。Agent 入口应把任务路由到本目录的 `tools/pmo-preflight.mjs`，并逐个读取输出的 Must read 文件。

本目录的长期规则以当前快照内 `rules/**` 为准；升级 Mango baseline 时整体同步，不在业务功能任务中手改。验收断言覆盖：`rules/index.json` 可解析，preflight 对 backend/frontend/pmo/product 路径能命中规则，台账检查 help 和 plan/verify 模式可执行。

## 12. 常见问题

- Must read 为空或异常时检查 `rules/index.json`。
- verify 模式台账状态需要是 `DONE` 或 `EXCEPTION`。
- baseline 和 Mango 主仓规则不一致时，应通过 baseline 升级同步，而不是在业务任务中手改。

## 13. 关联 PMO 规则

- [开发流程规范](./rules/00-dev-flow.md)
- [交付契约规范](./rules/01-delivery-contract.md)
- [AI 编码红线](./rules/03-ai-coding-redlines.md)

## 14. 历史设计 / 交付记录

- [Business PMO README](../README.md)
- [Business Starter README](../../README.md)
