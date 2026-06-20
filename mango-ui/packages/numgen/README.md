# @mango/numgen

## 1. 概览

`@mango/numgen` 是编号生成的管理后台前端包，配套后端 `mango-numgen` 使用。

它提供：

- 编号规则管理页面。
- 生成器、规则版本、规则片段、生成历史的前端 API 封装。
- 单个取号、批量取号和规则预览的前端调用封装。

它不是通用业务组件，也不适合官网、C 端页面直接集成。普通业务页面如需取号，优先通过业务后端调用 `mango-numgen`，由后端负责和业务保存流程一起处理唯一约束、重试和幂等。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 生成器管理 | 创建、编辑、删除、启停编号生成器。 |
| 规则版本管理 | 创建规则版本、编辑草稿、发布版本、查看历史版本。 |
| 规则片段编辑 | 支持 `TEXT`、`DATE`、`PARAM`、`SEQ`、`EXPR` 片段。 |
| 编号预览 | 按当前规则和动态参数预览编号。 |
| 手工取号 | 在管理页面按生成器取一个或批量取多个编号。 |
| 历史查询 | 查询生成历史、结果编号、业务键、状态和错误信息。 |
| API 封装 | 导出 `numgenApi` 和 TypeScript 类型。 |

## 3. 集成形态

| 形态 | 是否支持 | 说明 |
|------|----------|------|
| `admin-shell` | 否 | 不提供管理后台壳、登录态、菜单运行时。 |
| `admin-pages` | 是 | 通过 `registerMangoNumgenAdminPages` 注册编号规则页面。 |
| `business-component` | 否 | 不提供可嵌入普通业务页面的通用组件。 |
| `api-client` | 是 | 导出 `numgenApi`，封装后端 `/numgen/**` 接口。 |

## 4. 接入方式

安装依赖：

```bash
pnpm add @mango/numgen
```

注册管理页面：

```ts
import { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';

registerMangoNumgenAdminPages();
```

页面注册后可被以下 component key 打开：

```text
platform/numgen/index
numgen/index
```

后端菜单资源中的 component 应使用上面的页面 key。

## 5. 快速开始

1. 后端启用 `mango-numgen`，执行 numgen 相关 migration，并同步 numgen 菜单资源。
2. 管理后台安装并注册 `@mango/numgen/admin-pages`。
3. 给角色授权 `numgen:manage:list`；需要维护规则时再授权 `numgen:manage:write`。
4. 打开 `/data/numgen` 编号规则菜单。
5. 创建生成器和规则版本，编辑规则片段。
6. 预览编号，确认格式后发布规则。
7. 业务后端调用 `NumgenApi` 取号，业务表用唯一索引兜底。

## 6. 配置说明

本包没有独立运行时配置文件。页面行为由三类输入决定：

| 输入 | 来源 | 影响 |
|------|------|------|
| 页面注册 | `registerMangoNumgenAdminPages` | 决定菜单 component 能否打开页面。 |
| 菜单权限 | numgen `AUTH_MENU` 资源 / 角色授权 | 决定用户能否看到菜单和维护规则。 |
| 编号规则 | 后端 `mango-numgen` 数据表 | 决定取号格式、发布版本和历史记录。 |

包依赖：

| 类型 | 依赖 |
|------|------|
| dependencies | `@mango/admin-pages`、`@mango/api-schema`、`@mango/common`、`@element-plus/icons-vue` |
| peerDependencies | `vue`、`element-plus` |

## 7. API 与扩展

### 7.1 导出对象

| 导出 | 用途 |
|------|------|
| `NumgenView` | 编号规则管理页。 |
| `registerMangoNumgenAdminPages` | 注册 admin-pages 页面。 |
| `numgenApi` | 前端 API 调用封装。 |

### 7.2 页面 key

| key | 页面 |
|-----|------|
| `platform/numgen/index` | 编号规则管理页 |
| `numgen/index` | 编号规则管理页 |

### 7.3 常用 API

| API | 后端路径 | 用途 |
|-----|----------|------|
| `numgenApi.pageGenerators` | `/numgen/generators/page` | 查询生成器分页。 |
| `numgenApi.createGenerator` | `/numgen/generators` | 新增生成器。 |
| `numgenApi.updateGenerator` | `/numgen/generators` | 修改生成器。 |
| `numgenApi.updateGeneratorStatus` | `/numgen/generators/status` | 启停生成器。 |
| `numgenApi.deleteGenerator` | `/numgen/generators` | 删除生成器。 |
| `numgenApi.pageRules` / `pageVersions` | `/numgen/rules/page` | 查询规则版本分页。 |
| `numgenApi.createRule` / `createVersion` | `/numgen/rules` | 新增规则版本。 |
| `numgenApi.updateRule` / `updateVersion` | `/numgen/rules` | 修改规则版本。 |
| `numgenApi.publishRule` / `publishVersion` | `/numgen/rules/publish` | 发布规则版本。 |
| `numgenApi.previewRule` / `previewVersion` | `/numgen/rules/preview` | 预览编号。 |
| `numgenApi.pageSegments` | `/numgen/segments/page` | 查询规则片段。 |
| `numgenApi.createSegment` | `/numgen/segments` | 新增规则片段。 |
| `numgenApi.updateSegment` | `/numgen/segments` | 修改规则片段。 |
| `numgenApi.deleteSegment` | `/numgen/segments` | 删除规则片段。 |
| `numgenApi.pageHistories` | `/numgen/histories/page` | 查询生成历史。 |
| `numgenApi.nextValue` | `/numgen/next` | 生成一个编号。 |
| `numgenApi.batchValue` | `/numgen/batch` | 批量生成编号。 |

### 7.4 常用类型

| 类型 | 关键字段 |
|------|----------|
| `NumgenGenerator` | `id`、`genKey`、`genName`、`domainCode`、`status`、`currentRuleVersion`、`currentPublishStatus`、`hasUnpublishedChanges` |
| `NumgenRule` / `NumgenVersion` | `id`、`genKey`、`ruleName`、`version`、`status`、`publishStatus`、`versionState` |
| `NumgenRuleSegment` | `ruleId`、`sortOrder`、`segmentType`、`segmentName`、`literalValue`、`variableKey`、`dateFormat`、`seqWidth`、`padChar`、`sequenceScope` |
| `NumgenHistory` | `genKey`、`ruleVersion`、`resultNo`、`bizKey`、`inputDigest`、`costMillis`、`status`、`errorMessage` |
| `NumgenNextRequest` | `genKey`、`params` |
| `NumgenBatchRequest` | `genKey`、`count`、`params` |
| `NumgenPreviewRequest` | `genKey`、`count`、`params` |

业务前端直接取号示例：

```ts
import { numgenApi } from '@mango/numgen';

const no = await numgenApi.nextValue({
  genKey: 'ORDER_NO',
  params: { orgCode: 'HQ' },
});
```

## 8. 数据与初始化

本包不创建数据库表，也不初始化菜单权限。它依赖后端完成以下初始化：

| 数据 | 初始化来源 | 前端用途 |
|------|------------|----------|
| 编号生成表 | `mango-numgen` migration | 展示和维护生成器、规则、片段、序列和历史。 |
| 支付域默认编号 | `mango-numgen` migration | 支付模块开箱使用支付编号规则。 |
| 菜单与权限 | `mango-authorization` migration | 提供 `/data/numgen` 菜单和 `numgen:manage:*` 权限。 |
| 前端模块运行策略 | `mango-authorization` migration | 让管理后台知道 `mango-numgen` 的前端页面来源。 |

## 9. 管理入口

| 菜单 | 路由 | 权限码 | 页面 key |
|------|------|--------|----------|
| 编号规则 | `/data/numgen` | `numgen:manage:list` | `platform/numgen/index` / `numgen/index` |
| 编号规则维护 | 无独立页面 | `numgen:manage:write` | 同编号规则页面 |

页面可见但操作失败时，分别检查：

- 角色是否有 `numgen:manage:list` 和 `numgen:manage:write`。
- 后端 `/numgen/**` 接口是否已由部署应用提供。
- 当前管理后台是否执行了 `registerMangoNumgenAdminPages()`。

## 10. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 菜单能看到但页面打不开 | 是否注册 `@mango/numgen/admin-pages`，菜单 component 是否能映射到 `platform/numgen/index` 或 `numgen/index`。 |
| 页面请求 404 | 后端是否启用 `mango-numgen-starter`，网关是否转发 `/numgen/**`。 |
| 页面请求 403 | 当前角色是否拥有 `numgen:manage:list` 或 `numgen:manage:write`。 |
| 发布后取号格式没变 | 后端规则缓存 TTL、当前生效版本、发布状态。 |
| 参数片段为空 | 预览或取号时 `params` 是否传入规则片段的 `variableKey`。 |
| 编号重复 | 这不是前端问题；检查业务表唯一索引、后端取号链路和 KV 后端。 |

## 11. 相关文档

- [后端编号生成模块](../../../mango/mango-platform/mango-numgen/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
