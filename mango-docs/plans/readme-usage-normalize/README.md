# README Usage Normalize Workbench

本目录登记本轮 README 重写工作。它只服务一个目标：把模块 README 改成业务开发者能直接使用的说明，不沿用旧 README 的模板章节。

## 1. 重写目标

每个有效模块 README 必须回答：

- 这个模块是什么。
- 对外提供什么能力。
- 开发时如何接入依赖。
- 部署时需要启用什么 starter、服务或配置。
- 前端如何使用，属于 Admin Shell / Admin Pages 配套能力，还是业务可复用组件。
- 配置在哪里，配置字段是什么含义。
- 数据、菜单、权限、字典等初始化入口在哪里。
- 常见问题从哪里排查。

`mango/mango-platform/mango-file/README.md` 是本轮使用说明的标杆。

## 2. 不写什么

- 不把内部设计思路写进使用说明主线；设计内容登记到“架构设计”章节或相关设计文档，再从 README 引用。
- 不保留泛泛的 `适用场景`、`边界说明`、`模块组成`、`历史资料` 模板章节。
- 不新增通用 `验证方式`、`质量检查` 章节。
- 不用 starter 混淆开发依赖和部署依赖。开发时依赖 API 契约；部署能力提供方才启用 starter。
- 不把纯管理页面写成公共组件。纯管理页面只说明管理能力、页面 key、菜单权限和依赖接口。

## 3. 目标章节

### 3.1 后端平台能力

用于 `mango/mango-platform/**/README.md`。

1. 概览
2. 功能清单
3. 后端接入
4. 前端接入或前端调用方式
5. 快速开始
6. 配置说明
7. YAML 配置字段
8. 运行时配置字段
9. 返回字段
10. 管理入口
11. 数据与初始化
12. 问题排查
13. 相关文档

没有运行时配置、返回字段或管理入口时可以省略，但不能用空章节占位。

### 3.2 前端能力包

用于 `mango-ui/packages/*/README.md`。

1. 概览
2. 功能清单
3. 集成形态
4. 接入方式
5. 快速开始
6. 配置说明
7. API、组件和页面
8. 数据与初始化
9. 管理入口
10. 问题排查
11. 相关文档

必须明确标识能力属于：

- `admin-shell`：管理后台壳、运行时、导航、登录态等底座。
- `admin-pages`：管理后台页面插件。
- `business-component`：业务页面可复用组件。
- `api-client`：前端请求封装。

### 3.3 基础能力和 starter

用于 `mango/mango-infra/**`、`mango-business-starter/**`、`deploy/**`、CLI 模板。

1. 概览
2. 功能清单
3. 接入方式
4. 配置说明
5. 常用用法
6. 数据与初始化
7. 问题排查
8. 相关文档

公共能力必须写清楚 API、配置字段、默认行为。模板和部署类文档必须写清楚复制后业务项目需要改什么。

## 4. 本轮重写顺序

优先处理已经确认“不一致”的 file 同类能力和用户点名模块：

| 状态 | README | 类型 | 处理要点 |
|------|--------|------|----------|
| DONE | `mango/mango-platform/mango-calendar/README.md` | 后端平台能力 | 日历、节假日、工作日判断、菜单/权限/种子数据 |
| DONE | `mango/mango-platform/mango-captcha/README.md` | 后端平台能力 | 图形验证码、短信/邮件验证码、校验接口、前端调用 |
| DONE | `mango-ui/packages/calendar/README.md` | 前端能力包 | admin-pages 页面插件和前端 API 封装 |
| DONE | `mango/mango-platform/mango-domain/README.md` | 后端平台能力 | 业务域编码、树、接口、种子数据和权限 |
| DONE | `mango/mango-platform/mango-access/README.md` | 后端平台能力 | 边界入口鉴权、web/gateway starter、token 读取、API 资源决策和排障 |
| DONE | `mango/mango-platform/mango-auth/README.md` | 后端平台能力 | 登录认证、机构选择、token、验证码、企微登录、前端登录页接入 |
| DONE | `mango/mango-platform/mango-authorization/README.md` | 后端平台能力 | API 资源、菜单/按钮权限、manifest 入库、角色授权和用户菜单 |
| DONE | `mango/mango-platform/mango-file-preview/README.md` | 后端平台能力 | 文件预览能力 |
| DONE | `mango/mango-platform/mango-identity/README.md` | 后端平台能力 | 身份能力 |
| DONE | `mango/mango-platform/mango-job/README.md` | 后端平台能力 | 任务调度能力 |
| DONE | `mango/mango-platform/mango-notice/README.md` | 后端平台能力 | 通知能力 |
| DONE | `mango/mango-platform/mango-numgen/README.md` | 后端平台能力 | 编号生成能力 |
| DONE | `mango-ui/packages/notice/README.md` | 前端能力包 | admin-pages 页面插件、通知 API 和管理入口 |
| DONE | `mango-ui/packages/numgen/README.md` | 前端能力包 | admin-pages 页面插件、编号 API 和规则管理 |
| DONE | `mango-ui/packages/job/README.md` | 前端能力包 | admin-pages 页面插件、任务 API 和管理入口 |
| DONE | `mango-ui/packages/admin-pages/README.md` | 前端底座 | 页面注册、隐藏路由、默认页面和通知铃铛提供方 |
| DONE | `mango-ui/packages/admin-shell/README.md` | 前端底座 | 管理后台壳、菜单、运行时页面挂载和功能开关 |
| DONE | `mango-ui/packages/admin/README.md` | 前端底座 | 管理后台聚合入口和全量样式 |
| DONE | `mango-ui/packages/api-schema/README.md` | 前端基础类型 | API ID、响应、分页和基础实体类型 |
| DONE | `mango-ui/packages/app-runtime/README.md` | 前端运行时 | runtime-config、微前端适配、运行时上下文 |
| DONE | `mango-ui/packages/auth/README.md` | 前端能力包 | admin-shell 认证页面、登录 API、用户信息和租户登录 |
| DONE | `mango-ui/packages/common/README.md` | 前端公共能力 | request、session、公共组件、公共 API 和管理端依赖边界 |
| DONE | `mango-ui/packages/file/README.md` | 前端能力包 | admin-pages、MUpload、文件 API、大小限制和妙传配置 |
| DONE | `mango-ui/packages/file/src/components/README.md` | 前端公共组件 | MUpload、FilePreviewPanel、props、文件后端依赖和权限边界 |
| DONE | `mango/mango-platform/mango-org/README.md` | 后端平台能力 | 组织能力 |
| DONE | `mango/mango-platform/mango-payment/README.md` | 后端平台能力 | 支付能力 |
| DONE | `mango/mango-platform/mango-system/README.md` | 后端平台能力 | 系统管理能力 |
| REMOVED | 原 `mango/mango-platform/mango-seed/README.md` | 后端平台能力 | Issue #182 已移除独立种子初始化模块 |
| DONE | `mango/mango-platform/mango-template/README.md` | 后端平台能力 | 模板能力 |
| DONE | `mango-ui/packages/template/README.md` | 前端能力包 | admin-pages 页面插件、模板 API 和文件选择依赖 |
| DONE | `mango-ui/packages/payment/README.md` | 前端能力包 | admin-pages、PaymentCashier、支付 API、初始化和高风险入口 |
| DONE | `mango-ui/packages/rbac/README.md` | 前端能力包 | admin-pages、authorization/identity/org API、菜单权限初始化 |
| DONE | `mango-ui/packages/system/README.md` | 前端能力包 | admin-pages、system/domain/event API、业务组件和初始化来源 |
| DONE | `mango-ui/packages/system/src/components/README.md` | 前端公共组件 | ParticipantSelector、DomainSelector、DomainSideTree 的 props 和后端依赖 |
| DONE | `mango/mango-platform/mango-workflow/README.md` | 后端平台能力 | 工作流能力 |
| DONE | `mango-ui/packages/workflow/README.md` | 前端能力包 | admin-pages、business-component、api-client |
| DONE | `mango-ui/packages/workflow/src/components/README.md` | 前端公共组件 | 动态表单、审批轨迹、业务申请/审批注册 |
| DONE | `mango-ui/packages/workflow-business-example/README.md` | 前端示例包 | 工作流业务申请/审批组件注册示例 |
| DONE | `mango-ui/packages/mango-cli/README.md` | CLI | 项目生成、能力追加、业务模块生成、PMO 同步和本地开发编排 |
| DONE | `mango-ui/packages/mango-cli/templates/full/README.md` | CLI 模板 | full 业务项目生成后的启动、配置、平台能力和业务扩展 |
| DONE | `mango-ui/packages/mango-cli/templates/full/business-pmo/README.md` | CLI 模板 | 业务仓 PMO baseline、preflight 和交付检查 |
| DONE | `mango-ui/packages/mango-cli/templates/full/business-pmo/mango-baseline/README.md` | CLI 模板 | baseline 规则、工具、模板和同步边界 |
| DONE | `mango-ui/packages/mango-cli/templates/full/topologies/monolith/README.md` | CLI 模板 | 单体拓扑、starter 依赖、菜单权限和初始化 |
| DONE | `mango-ui/packages/mango-cli/templates/full/topologies/microservice/README.md` | CLI 模板 | 微服务拓扑、starter-remote、网关和资源同步 |
| DONE | `mango-ui/packages/*/README.md` | 前端能力包 | 去模板化，标识集成形态 |
| DONE | `mango/mango-infra/**/README.md` | 基础能力 | 公共 API、配置和使用闭环 |
| DONE | `mango-business-starter/**/README.md` | starter | 开发/部署/复制后修改点 |
| DONE | `deploy/**/README.md` | 部署 | 部署参数和运行入口 |

完整候选由以下命令维护：

```bash
rg -l "^## [0-9]+\\. (适用场景|边界说明|模块组成|历史资料)$" \
  README.md deploy/**/*.md mango/**/*.md mango-ui/packages/**/*.md mango-business-starter/**/*.md \
  -g 'README.md' | sort
```

## 5. 工作规则

- 每次重写前先看代码、配置类、controller、API、前端导出、资源 manifest 和迁移脚本。
- 每个 README 只写源码能证明的内容；不确定的内容不写。
- 修改状态要在本目录登记。
- 批量完成后运行 README 门禁和 docs build。
