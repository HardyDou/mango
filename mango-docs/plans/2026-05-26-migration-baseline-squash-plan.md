# Migration 基线压缩设计说明

## 目标

在未正式发布、无共享环境依赖历史 migration 的前提下，将每个模块的多版本 migration 压缩为模块级 `V1__init_{module}.sql`。

## 范围

- 只处理 `src/main/resources/db/migration/{module}/V*.sql`。
- 只压缩当前存在多版本 migration 的模块。
- 压缩后重新创建本地开发库并验证 Flyway 可从空库执行。

## 不做什么

- 不修改 `target` 目录。
- 不修改测试资源 migration。
- 不做最终 DDL 反向生成，只做按版本顺序合并为新的基线文件。
- 不用于已发布或共享环境。

## 设计决策

- 每个模块保留一个 `V1__init_{module}.sql`。
- 按 Flyway 版本号从小到大合并原文件内容。
- 在合并文件中保留原文件分段注释，方便追溯压缩来源。
- 删除该模块后续 `V2+` migration 文件。

## 影响模块

- authorization
- calendar
- file
- numgen
- system
- template

## 验收方式

- 本地 `mango` 库 drop/create 后，单体应用启动时 Flyway 完成迁移。
- 查询每个模块 history table，确认模块最终只记录 `V1`。
- 执行全量 compile。
