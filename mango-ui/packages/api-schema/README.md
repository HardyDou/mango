# @mango/api-schema

## 1. 概览
`@mango/api-schema` 提供前端 API 基础类型。它把后端 Long、雪花主键和业务主键统一建模为字符串，避免 JavaScript `number` 精度丢失。

本包是类型契约包，不发请求、不注册页面、不包含运行时代码生成。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 前端 API 包定义 ID、分页请求、分页响应和统一响应结构 | 前端 import / API 封装 |
| 页面表格、表单、路由参数、选择器 model 需要安全承接后端 Long ID | 前端 import / API 封装 |
| 业务包和平台包需要共享基础实体字段 | 前端 import / API 封装 |

## 3. 适用场景
- 前端 API 包定义 ID、分页请求、分页响应和统一响应结构。
- 页面表格、表单、路由参数、选择器 model 需要安全承接后端 Long ID。
- 业务包和平台包需要共享基础实体字段。

## 4. 边界说明
- 不负责 axios、fetch 或请求拦截器。
- 不生成 OpenAPI client。
- 不替代后端 DTO、VO 和 validation。
- 不处理菜单、权限、租户和数据初始化。

## 5. 模块组成
包名：`@mango/api-schema`。当前入口是 `src/index.ts`，只导出 TypeScript 类型：

- `ApiId`
- `R<T>`
- `PageQuery`
- `PageResult<T>`
- `BaseEntity`

后端仍是接口契约源头；前端类型必须跟后端 API 同步维护。

## 6. 接入方式
安装：

```bash
pnpm add @mango/api-schema
```

API 包使用：

```ts
import type { ApiId, PageQuery, PageResult, BaseEntity } from '@mango/api-schema';

export interface UserVO extends BaseEntity {
  id: ApiId;
  username: string;
}

export interface UserPageQuery extends PageQuery {
  username?: string;
}

export type UserPageResult = PageResult<UserVO>;
```

ID 使用要求：

```ts
const id: ApiId = row.id;
```

不要把后端 ID 转成 `number`。

## 7. 配置说明
本包没有运行时配置。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `ApiId` | 类型别名 | `string` | 后端 Long 和业务 ID 的前端表示 | 避免 Number 精度丢失 | `src/index.ts` |
| `R<T>` | `code` | 无 | 后端业务码 | 请求层判断成功或失败 | `src/index.ts` |
| `R<T>` | `data` | 泛型 | 业务数据 | API 函数返回数据类型 | `src/index.ts` |
| `R<T>` | `msg` | 无 | 响应消息 | 错误提示或诊断 | `src/index.ts` |
| `R<T>` | `success` | 无 | 是否成功 | 请求层成功判断 | `src/index.ts` |
| `PageQuery` | `page` | 无 | 页码 | 分页接口入参 | `src/index.ts` |
| `PageQuery` | `size` | 无 | 每页数量 | 分页接口入参 | `src/index.ts` |
| `PageResult<T>` | `list` | 无 | 当前页数据 | 表格数据源 | `src/index.ts` |
| `PageResult<T>` | `total` | 无 | 总数量 | 分页器 total | `src/index.ts` |
| `BaseEntity` | `id`、`createTime`、`updateTime`、`createBy`、`updateBy` | 可选 | 通用实体字段 | 页面列表和详情类型复用 | `src/index.ts` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `ApiId` | ID 字段、路由参数、选择器值 |
| `R<T>` | 统一响应包装 |
| `PageQuery` | 分页查询入参基类 |
| `PageResult<T>` | 分页返回结构 |
| `BaseEntity` | 通用审计字段基类 |

## 9. 数据与初始化
本包不包含数据库 migration 和初始化数据。它只描述前端类型，数据库字段、主键策略和审计字段由后端模块定义。

## 10. 管理入口
本包不处理菜单、权限和租户。涉及租户 ID、用户 ID、角色 ID、菜单 ID 时，前端类型应使用 `ApiId`，后端接口必须负责权限和租户校验。

## 11. 快速开始
1. 在前端 API 包引入基础类型。
2. 所有后端 ID 字段使用 `ApiId`。
3. 分页接口使用 `PageQuery` 和 `PageResult<T>`。
4. 页面组件和表单 model 沿用同一套类型。
5. 执行 TypeScript 构建和页面链路验证。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 大 ID 显示不一致 | 把 ID 转成 number | 使用 `ApiId`，不要 `Number(id)` |
| API 类型和后端不一致 | 只改前端类型，没同步后端契约 | 以后端接口为准同步 Command、Query、VO |
| 分页 total 不准 | 后端返回结构不是 `PageResult<T>` | 修正 API 包类型或后端响应 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [Monorepo 架构规范](../../../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
