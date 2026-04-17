# 后端开发流程

## 1. 适用范围

- 本文件只约束后端开发。
- 通用流程以 [00-dev-flow.md](/Users/hardy/Work/mango/mango-pmo/rules/00-dev-flow.md) 为准。

## 2. 开发前

必须明确：

- 目标
- 范围
- 验收标准
- 影响模块
- 接口变化
- 数据变化
- 测试范围

## 3. 开发中

- 只改本次需求相关代码。
- 新增代码必须遵守最新规范。
- 不把 `PO`、`Entity` 直接暴露到 API。
- 不新增跨模块错误依赖。
- 不在本次任务中顺手扩大改动范围。

## 4. 数据库变更

- DDL 变更必须使用 Flyway migration。
- 禁止直接改线上库。
- 每次变更新增 migration 文件。
- 不修改已执行的历史 migration。
- migration 路径使用 `db/migration/{module}/V{version}__{description}.sql`。
- migration 按模块隔离。

## 5. 提交前验证

至少执行与改动范围对应的检查：

- `mvn test`
- `mvn verify`
- `mvn mango:check`

## 6. 提交要求

提交说明必须写清：

- 改动范围
- 验证结果
- 未完成项
- 风险

## 7. 禁止事项

- 没有设计就直接改跨模块边界
- 没有验证就提交
- 直接修改生产数据结构
- 用一次性脚本替代正式 migration
