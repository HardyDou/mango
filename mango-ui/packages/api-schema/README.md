# @mango/api-schema

`@mango/api-schema` 提供前端和后端约定的基础 API 类型。它不发请求，只提供类型契约。

## 1. 概览

这个包属于 `api-client` 基础能力。业务前端在定义接口返回、分页结果和实体 ID 时，可以直接复用这里的类型。

## 2. 功能清单

| 类型 | 用途 |
|------|------|
| `ApiId` | 后端 Long、雪花主键、业务主键类 ID 在前端统一按字符串处理。 |
| `R<T>` | 统一响应结构。 |
| `PageQuery` | 分页查询参数。 |
| `PageResult<T>` | 分页返回结果。 |
| `BaseEntity` | 基础实体字段。 |

## 3. 接入方式

安装依赖：

```bash
pnpm add @mango/api-schema
```

使用类型：

```ts
import type { ApiId, PageResult, R } from '@mango/api-schema';

export interface OrderRow {
  id: ApiId;
  orderNo: string;
}

export type OrderPageResponse = R<PageResult<OrderRow>>;
```

## 4. 配置说明

这个包没有运行时配置、环境变量或样式文件。

## 5. API 与扩展

| 类型 | 字段 |
|------|------|
| `ApiId` | `string` |
| `R<T>` | `code`、`data`、`msg`、`success` |
| `PageQuery` | `page`、`size` 和扩展字段 |
| `PageResult<T>` | `list`、`total`、`page`、`size` |
| `BaseEntity` | `id`、`createTime`、`updateTime`、`createBy`、`updateBy` |

`ApiId` 不要转成 number。JavaScript number 无法安全表示超过 `Number.MAX_SAFE_INTEGER` 的 Long。

## 6. 数据与初始化

这个包不包含数据库、菜单、权限或初始化脚本。

## 7. 管理入口

这个包没有管理页面。

## 8. 快速开始

1. 接口类型里用 `ApiId` 表示后端 Long ID。
2. 分页接口返回用 `PageResult<T>`。
3. 统一响应包装用 `R<T>`。

## 9. 问题排查

**表格选中或路由跳转后 ID 不一致**

检查是否把 `ApiId` 转成了 number。ID 应保留为字符串。

**分页字段对不上**

确认当前接口使用的是 `page`、`size`、`list`、`total` 这一组字段；如果后端接口不同，需要在业务 API 层适配。

## 10. 相关文档

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
