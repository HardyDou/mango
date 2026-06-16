# @mango/numgen

## 1. 概览
`@mango/numgen` 提供编号生成管理前端页面和 API 封装，用于维护生成器、规则、规则段、版本发布、预览、取号和取号历史。

本包属于 `admin-pages` 配套能力，依赖后端 `mango-numgen`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理业务单据编号规则 | 前端注册 / 组件 / API 封装 |
| 配置文本、日期、参数、序列、表达式等编号段 | 前端注册 / 组件 / API 封装 |
| 发布规则版本并预览生成结果 | 前端注册 / 组件 / API 封装 |
| 查询取号历史、失败原因和业务键 | 前端注册 / 组件 / API 封装 |
| 业务页面需要调用后端取号接口 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 管理业务单据编号规则。
- 配置文本、日期、参数、序列、表达式等编号段。
- 发布规则版本并预览生成结果。
- 查询取号历史、失败原因和业务键。
- 业务页面需要调用后端取号接口。

## 4. 边界说明
- 不负责后端并发取号、唯一约束和幂等。
- 不替代业务单据表上的唯一索引。
- 不负责编号规则的领域审批流程。
- 不初始化菜单、权限和默认编号规则。

## 5. 模块组成
本包包含：

- `NumgenView`：编号管理页面。
- `registerMangoNumgenAdminPages`：页面注册函数。
- `numgenApi`：生成器、规则、规则段、历史、取号 API。

后端负责规则存储、版本发布、序列并发控制和取号幂等。

## 6. 接入方式
安装：

```bash
pnpm add @mango/numgen
```

注册管理页面：

```ts
import { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';

registerMangoNumgenAdminPages();
```

业务取号：

```ts
import { numgenApi } from '@mango/numgen';

const no = await numgenApi.nextValue({
  genKey: 'ORDER_NO',
  params: { orgCode: 'HQ' },
});
```

菜单 component key：

```text
platform/numgen/index
numgen/index
```

## 7. 配置说明
本包没有运行时配置文件。行为由后端规则和 API 参数决定。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `registerMangoNumgenAdminPages` | `moduleCode` | `mango-numgen` | 页面归属模块 | 和后端菜单匹配 | `admin-pages.ts` |
| 页面注册 | `component` | `platform/numgen/index`、`numgen/index` | 编号管理页 key | 菜单打开页面 | `admin-pages.ts` |
| `NumgenGenerator` | `genKey` | 无 | 生成器编码 | 业务取号主键 | `api/numgen.ts` |
| `NumgenGenerator` | `domainCode` | 可选 | 所属领域 | 规则归类 | `api/numgen.ts` |
| `NumgenRule` | `version` | 后端生成 | 规则版本 | 发布和历史追踪 | `api/numgen.ts` |
| `NumgenRuleSegment` | `segmentType` | 无 | `TEXT`、`DATE`、`PARAM`、`SEQ`、`EXPR` | 决定编号段计算方式 | `api/numgen.ts` |
| `NumgenRuleSegment` | `seqWidth`、`padChar` | 可选 | 序列宽度和补位 | 影响序列格式 | `api/numgen.ts` |
| `NumgenNextRequest` | `genKey` | 无 | 取号生成器 | 决定使用哪套规则 | `api/numgen.ts` |
| `NumgenNextRequest` | `params` | 可选 | 取号参数 | PARAM/EXPR 段使用 | `api/numgen.ts` |
| `NumgenBatchRequest` | `count` | 无 | 批量取号数量 | 返回多个编号 | `api/numgen.ts` |

## 8. API 与扩展
| 导出 | 用途 |
|------|------|
| `NumgenView` | 编号管理页 |
| `registerMangoNumgenAdminPages` | 注册管理页 |
| `numgenApi.pageGenerators`、`createGenerator`、`updateGenerator` | 生成器管理 |
| `numgenApi.pageRules`、`createRule`、`updateRule`、`publishRule` | 规则管理和发布 |
| `numgenApi.pageSegments`、`createSegment`、`updateSegment` | 规则段管理 |
| `numgenApi.previewRule`、`previewVersion` | 预览编号 |
| `numgenApi.pageHistories` | 取号历史 |
| `numgenApi.nextValue` | 单个取号 |
| `numgenApi.batchValue` | 批量取号 |

## 9. 数据与初始化
本包不包含数据库 migration。依赖后端 `mango-numgen` 初始化表和资源。

| 类型 | 后端来源 | 前端消费 | 排查入口 |
|------|----------|----------|----------|
| 生成器 | mango-numgen | 生成器列表和取号 | 创建后可查询 |
| 规则版本 | mango-numgen | 规则配置、发布和预览 | 发布后可取号 |
| 规则段 | mango-numgen | 规则段编辑 | 预览结果符合段顺序 |
| 序列状态 | mango-numgen | 取号 | 并发下不重复 |
| 取号历史 | mango-numgen | 历史列表 | 取号后有记录 |
| 菜单权限 | authorization / numgen resource | 页面入口和按钮权限 | 菜单可见、接口可用 |

## 10. 管理入口
| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 编号管理 | `platform/numgen/index` 或 `numgen/index` | 后端 numgen 模块定义 | 后端 resource / migration | 角色授权 | numgen admin API |

业务取号接口也必须做权限、租户和幂等校验；前端页面只负责配置和调用。

## 11. 快速开始
1. 后端启用 `mango-numgen`。
2. 前端注册 `@mango/numgen/admin-pages`。
3. 后端初始化菜单权限并授权。
4. 创建生成器和规则段。
5. 发布规则并预览。
6. 业务页面或后端调用取号接口。
7. 校验唯一约束、并发不重复、取号历史和租户隔离。

## 12. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| 取号失败 | 规则未发布或 genKey 错误 | 查生成器和规则状态 |
| 编号重复 | 业务表缺唯一约束或后端序列配置错误 | 后端补唯一约束和并发测试 |
| PARAM 段为空 | 取号 params 没传对应变量 | 检查规则段 variableKey |
| 页面打不开 | component key 没注册 | 调用 `registerMangoNumgenAdminPages` |
| 接口 403 | 角色缺编号权限 | 查 authorization 授权 |

## 13. 相关文档
- [前端代码规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [后端 Numgen](../../../mango/mango-platform/mango-numgen/README.md)

## 14. 历史资料
- [Mango UI README](../../README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
