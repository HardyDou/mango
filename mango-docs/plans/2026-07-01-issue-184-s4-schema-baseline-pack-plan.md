# Issue #184 S4 Schema Baseline Pack 计划

状态：已完成设计说明，基于 S3 的外部 `filesystem:` locations 使用。

当前使用入口见 [mango-infra-persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) 和 [Issue #184 总设计](../designs/2026-07-01-issue-184-data-governance-design.md)。

## 背景

模块历史 migration 持续增长后，新数据库会从 V1 开始执行所有历史 SQL，启动慢、排查困难，也很难直接看到当前完整表结构。旧数据库又不能删除历史 migration，否则已有 Flyway history 会失去可追溯性。

## 目标

基于 S3 的模块级 `locations` 能力，形成 Schema Baseline Pack 方案：

- 新数据库可以选择只执行模块当前全量结构 baseline pack 和 baseline 之后的新 migration。
- 旧数据库继续使用原模块 history table 和历史 migration 升级，不被 baseline pack 强制切换。
- baseline pack 仍使用 Flyway 版本化 SQL 和模块独立 history table，不绕过 Flyway。

## 方案

模块发布 baseline pack 时，提供一组独立 migration 目录，例如：

```text
/opt/mango/baseline/payment/
  V2026070100__baseline_payment_schema.sql
  V2026070101__add_payment_channel_index.sql
```

新数据库初始化时配置：

```yaml
mango:
  persistence:
    flyway:
      modules:
        payment:
          locations:
            - filesystem:/opt/mango/baseline/payment
```

旧数据库升级时继续使用默认路径或历史升级包：

```yaml
mango:
  persistence:
    flyway:
      modules:
        payment:
          locations:
            - classpath:db/migration/payment
            - filesystem:/opt/mango/upgrade/payment
```

## 边界

- baseline pack 不是默认自动替换历史 migration。
- baseline pack 不能和同模块历史 V1...Vn 目录在同一个新库配置中混用，否则会重复建表或版本冲突。
- 旧库切换 baseline pack 必须先做独立升级评审，不作为 S4 自动能力。
- baseline SQL 由模块 Owner/DBA 维护，必须能单独创建当前完整结构。

## 验收计划

- 文档明确新库和旧库两条路径。
- 文档明确 baseline pack 不绕过 Flyway。
- 文档明确 baseline pack 与历史 migration 不能盲目混用。
- S3 已验证 `filesystem:` 外部目录可执行并写入模块 history table。
