# 业务域管理模块 Sprint 计划

## 1. 背景

业务域需要成为 Mango 平台级基础能力，用于统一维护业务域编码、前缀、层级和状态，并为字典、参数、工作流、通知等后续模块适配提供标准查询接口。

## 2. 目标

- 新增独立平台模块 `mango-domain`。
- 提供业务域基础信息管理、启停、逻辑删除、层级查询和启用树接口。
- 在系统设置中增加“业务域”管理页面。
- 提供可复用的业务域选择组件。
- 完成字典、系统参数/配置、工作流、通知中心首批业务域适配。

## 3. 范围

本 Sprint 包含：

- 后端 `mango-domain-api/core/starter/starter-remote`。
- `biz_domain` Flyway migration 和内置业务域 seed。
- 业务域管理 REST 接口和远程 Feign 适配。
- 授权菜单 migration，将“业务域”挂到系统设置下。
- 前端系统设置业务域页面、API 封装和 `DomainSelector` 组件。
- 页面注册表更新。
- 字典类型、系统配置/参数、工作流分类/定义、通知业务类型关联业务域。

本 Sprint 不包含：

- 业务域对全局数据权限、菜单权限或租户模型的控制。
- 任何业务流程侵入式改造。

## 4. 改动项

### 4.1 后端

- 新增 `mango-platform/mango-domain` 聚合模块。
- 新增 `DomainApi`、`CreateDomainCommand`、`UpdateDomainCommand`、`UpdateDomainStatusCommand`、`DomainPageQuery`、`DomainVO`。
- 新增 `DomainEntity`、`DomainMapper`、`IDomainService` 和实现。
- API 路径使用 `/domain/domains`，避免 path-param。
- 创建时根据父级自动拼接 `domain_code`；编辑时不允许修改编码和父级。
- 删除为逻辑删除，存在子节点时禁止删除。
- 停用业务域不出现在启用树接口中。

### 4.2 数据

- 新增 `biz_domain` 表。
- 表包含 `tenant_id`、`created_by`、`created_at`、`updated_by`、`updated_at` 标准字段。
- `domain_code` 和 `domain_short_code` 在租户内唯一。
- 内置通用域、工作流、通知域、日历域、编号域、文件域、模板域。
- 新增 authorization migration 注册系统设置菜单和权限。

### 4.3 前端

- `@mango/system` 新增 `domainApi`。
- 新增 `DomainSelector`，支持树形、单选、多选、禁用、清空。
- 新增 `DomainView`，采用树形表格和 Element Plus 标准表单布局。
- `packages/admin-pages` 注册 `system/domain/index`。

## 5. 验证方式

- `mvn -pl mango-platform/mango-domain/mango-domain-starter -am test`
- `mvn -pl mango-platform/mango-domain/mango-domain-starter-remote -am test`
- `mvn -pl mango-platform/mango-system/mango-system-starter -am test`
- `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -am test`
- `mvn -pl mango-platform/mango-notice/mango-notice-starter -am test`
- `pnpm -C mango-ui --filter @mango/system build`
- `pnpm -C mango-ui --filter @mango/workflow build`
- `pnpm -C mango-ui --filter @mango/notice build`
- `pnpm -C mango-ui --filter @mango/admin-pages build`

如环境允许，补充单体管理后台真实接口和页面操作验证。

## 6. 完成标准

- 后端模块可编译，业务规则测试通过。
- 前端包构建通过。
- 业务域页面完成列表、新增、编辑、删除、启停和启用树选择器能力。
- 首批适配 issue 已完成，剩余编号、文件、模板等平台能力按需进入后续迭代。

## 7. 后续适配 Issue

| Issue | 范围 | 验证口径 |
|---|---|---|
| BD-ISSUE-01 | 字典类型关联业务域，支持按业务域筛选 | 字典列表、创建、编辑、筛选回归 |
| BD-ISSUE-02 | 系统参数关联业务域，支持按业务域筛选 | 参数列表、创建、编辑、筛选回归 |
| BD-ISSUE-03 | 工作流流程分类适配业务域 | 流程定义分类、发起流程、通用流程回归 |
| BD-ISSUE-04 | 通知中心消息配置业务域替换为平台业务域 | 消息配置、发送任务、站内消息回归 |
| BD-ISSUE-05 | 编号、文件、模板等平台能力按需接入业务域选择 | 各模块管理页和关键业务链路回归 |

## 8. 风险与限制

- 菜单长期数据当前仍由 authorization migration 维护，本 Sprint 通过新增 migration 注册入口。
- 业务域编码作为其它模块 BizKey 前缀的强约束，需在后续适配 issue 中逐模块落地。
- 编辑父级会导致子树编码级联变化，首版禁止修改父级。
