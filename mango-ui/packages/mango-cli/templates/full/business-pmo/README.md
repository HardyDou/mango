# Business PMO

## 1. 能力定位

`business-pmo` 是生成业务项目中的 PMO 工作区，用于承载业务项目自己的计划、设计、交付台账和 Mango baseline 快照。主要使用者是业务项目开发者、PM、QA 和 Agent。

## 2. 适用场景

- 业务项目脱离 Mango 源码后仍执行 PMO preflight。
- 业务需求、设计、开发、验证和发布需要交付台账。
- 业务项目需要在本仓保存项目级规则、计划和证据。

## 3. 不适用场景

- 不作为 Mango 长期规范源。
- 不随普通业务需求直接修改 `mango-baseline/**`。
- 不替代业务模块 README 和业务设计文档。

## 4. 模块边界

`business-pmo/mango-baseline` 是 Mango PMO 的可执行快照；业务项目自有规则和交付记录应放在 baseline 外的业务 PMO 目录中。baseline 升级应通过 Mango baseline 同步任务处理。

## 5. 接入方式

生成项目后，Agent 入口通过业务仓 `AGENTS.md` 路由到：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role <role> \
  --phase <phase> \
  --task "<task>" \
  --paths "<paths>"
```

交付台账检查入口：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs
```

## 6. 配置项

本目录没有运行时配置项。preflight 和台账检查规则来自 `business-pmo/mango-baseline/rules/index.json` 及其引用的规则文件。

## 7. 对外接口 / 扩展点

- `mango-baseline/tools/pmo-preflight.mjs`
- `mango-baseline/tools/delivery-contract-check.mjs`
- `mango-baseline/rules/index.json`
- 业务项目可在 baseline 外追加项目级规则、计划和交付记录。

## 8. 数据库 / 初始化数据

本目录不包含数据库 migration。初始化内容来自 starter 模板生成的 baseline、规则、工具和 README。

## 9. 菜单 / 权限 / 租户

本目录不提供菜单、权限资源或租户数据。

## 10. 验证方式

在生成后的业务项目中验证：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs --role dev --phase develop --task "验证 PMO baseline" --paths "backend,frontend"
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs --help
```

模板源校验：

```bash
node mango-business-starter/scripts/check-template.mjs
```

## 11. 业务接入最小闭环

生成业务仓后，所有命令默认从业务项目根目录执行。正式交付前先运行 `business-pmo/mango-baseline/tools/pmo-preflight.mjs`，再按输出读取 baseline 文件和业务自有规则；涉及设计或台账时再运行 `delivery-contract-check.mjs`。

本 README 位于 Mango 源仓模板时，关联规则链接指向源仓 `mango-pmo`；生成业务仓后，应以 `business-pmo/mango-baseline/rules/**` 为实际可读规则路径。验收断言覆盖：preflight 能输出 Must read，台账检查能识别 plan/verify 模式，普通业务需求没有修改 `mango-baseline/**`。

## 12. 常见问题

- 普通业务需求不要直接改 `mango-baseline/**`，否则后续 baseline 同步会产生冲突。
- preflight 输出的 Must read 文件需要逐个读取原文。
- 台账校验失败时先检查列名和状态值是否符合 baseline 工具要求。

## 13. 关联 PMO 规则

- [开发流程规范](./mango-baseline/rules/00-dev-flow.md)
- [交付契约规范](./mango-baseline/rules/01-delivery-contract.md)
- [AI 编码红线](./mango-baseline/rules/03-ai-coding-redlines.md)

## 14. 历史设计 / 交付记录

- [Business Starter README](../README.md)
- [Mango Baseline README](./mango-baseline/README.md)
