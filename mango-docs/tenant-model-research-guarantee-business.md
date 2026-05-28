---
title: "保函业务系统租户模型调研与 Mango 差距分析"
type: architecture-research
status: draft
date: 2026-05-09
---

# 保函业务系统租户模型调研与 Mango 差距分析

## 1. 结论先行

保函业务系统里的“租户”不应该等同于用户、部门、菜单、系统模块，也不应该简单等同于每一笔业务的参与方。

在本系统里，租户应定义为：

> 一个拥有独立组织架构、人员账号、角色权限、审批流程、业务数据边界、审计边界和配置边界的企业级使用主体。

结合当前业务，元丰行、智诚等融资性担保公司、使用本系统处理业务的银行，都应作为企业租户。客户目前以个人账号为准，原则上不是租户，而是外部客户身份；只有当客户企业需要自己的组织、员工、权限、审批、门户配置时，才升级为客户企业租户。

最重要的一点是：保函业务是多企业协同，不是单企业内部 OA。租户之间默认隔离，但同一笔保函业务需要让元丰行、下游担保公司、银行在授权范围内共享资料、状态、任务和审批结论。因此系统不能只靠 `tenant_id = 当前租户` 的简单过滤解决所有问题，还需要“业务参与方”和“跨租户授权共享”模型。

## 2. 外部调研结论

### 2.1 通用 SaaS 租户定义

Microsoft Azure 架构文档对多租户的定义是：一个解决方案服务多个客户或租户；租户不同于用户，一个组织、公司或用户组通常形成一个租户。B2B 软件里，租户通常映射到客户企业；但如果客户企业内部存在国家、区域、事业部等强隔离诉求，也可以拆为多个租户。

AWS SaaS 文档强调，租户隔离不同于普通认证授权。用户登录成功、有角色权限，并不自动意味着租户隔离已经成立。系统必须基于当前 tenant context 限制资源访问，阻止访问其他租户的数据。

Salesforce 的多租户平台以 org 作为核心隔离单元，数据、元数据、权限、安全域和查询优化都围绕 org 进行。这个思路对 Mango 有参考价值：租户不只是数据字段，而是配置、权限、流程、审计、扩展能力的边界。

AWS 还区分 data partitioning 和 tenant isolation：数据分区只是“怎么存”，隔离是“如何确保不能越权访问”。这对 Mango 很关键，因为当前使用 `tenant_id` 行级过滤只是分区/过滤手段的一部分，不等于完整租户能力。

参考资料：

- Microsoft Azure: [Architect multitenant solutions on Azure](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/overview)
- Microsoft Azure: [Tenancy models for a multitenant solution](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models)
- AWS: [Tenant isolation](https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/tenant-isolation.html)
- AWS: [SaaS identity](https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/saas-identity.html)
- AWS: [Data partitioning](https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/data-partitioning.html)
- Salesforce: [Platform Multitenant Architecture](https://architect.salesforce.com/docs/architect/fundamentals/guide/platform-multitenant-architecture.html)

### 2.2 ERP/OA/SaaS 中租户真实解决的问题

租户真正解决的是企业级隔离和企业级自治：

| 能力 | 租户要解决的问题 |
|---|---|
| 数据隔离 | A 公司不能看到 B 公司的客户、合同、审批、附件、业务单据 |
| 组织隔离 | 每个企业维护自己的部门、岗位、人员关系 |
| 权限隔离 | 每个企业有自己的角色、用户授权、菜单授权 |
| 流程隔离 | 每个企业有自己的审批流程、风控节点、印章/签约规则 |
| 配置隔离 | 每个企业有自己的参数、通知模板、业务规则、材料清单 |
| 审计隔离 | 每个企业只能审计本企业操作；平台可按授权审计全局 |
| 运营隔离 | 租户可开通、停用、计费、限额、分配套餐 |
| 合规隔离 | 金融、担保、银行等机构可能需要更强数据边界和日志留存 |

什么时候需要租户：

- 一套系统服务多个企业客户。
- 每个企业有独立组织、角色、审批、业务数据。
- 企业之间不能默认互相看数据。
- 需要按企业开通、停用、计费、审计。
- 需要平台统一升级，但企业独立使用。

什么时候不应该用租户：

- 公司内部部门不是租户，是组织。
- 系统模块不是租户，是应用/菜单/功能。
- 用户身份不是租户，是账号。
- 一笔业务的参与方也不天然是租户；参与方是业务关系，只有参与方企业本身需要独立后台能力时才是租户。

## 3. 保函业务场景拆解

### 3.1 当前业务描述

目前涉及两类主链路：

1. 元丰行直接开商保：
   - 客户申请开保函。
   - 元丰行第一手接单。
   - 元丰行整理签约资料、风控、审批。
   - 元丰行直接出具商业保函。

2. 元丰行将银行保函业务倒流给融资性担保公司：
   - 客户申请银行保函。
   - 元丰行接单、整理资料、签约、初步风控。
   - 元丰行递交给智诚等融资性担保公司。
   - 智诚等担保公司内部风控、审批。
   - 担保公司将资料提交银行。
   - 银行最终开具银行保函。
   - 多数银行使用自有系统或线下处理，少数银行可能使用本系统处理。

### 3.2 参与方分类

| 参与方 | 是否租户 | 原因 |
|---|---:|---|
| 客户个人 | 否，默认是外部个人账号 | 目前客户以个人账号为准，没有独立组织、菜单、审批、员工管理诉求 |
| 客户企业 | 视情况 | 如果只是申请主体资料，不是租户；如果客户企业也要多人协同、审批、授权，则可以成为客户租户 |
| 元丰行 | 是 | 第一手接单方，需要组织、角色、风控流程、业务台账、签约资料管理 |
| 智诚等融资性担保公司 | 是 | 独立企业，独立风控、审批、资料管理、角色权限、审计 |
| 银行 | 视接入方式 | 使用本系统处理业务的银行应作为租户；只通过接口/线下接收资料的银行可先作为外部机构档案 |
| 平台运营方 | 是，平台空间/平台租户 | 负责租户开通、全局应用、菜单、字典、运营配置、审计运维 |

## 4. 推荐租户模型

### 4.0 三个概念必须分开

保函业务里最容易混淆的是“租户、业务参与方、权限角色”。

| 概念 | 回答的问题 | 示例 |
|---|---|---|
| 租户 / 企业空间 | 谁拥有独立后台、组织、成员、权限、流程、数据边界 | 元丰行企业空间、智诚担保企业空间、某银行企业空间 |
| 业务参与方 | 一笔保函业务中谁参与、承担什么职责 | 接单方、担保方、开函银行、申请人 |
| 权限角色 | 某个人在某个企业空间内能做什么 | 客户经理、风控员、审批人、资料员、管理员 |

所以元丰行、智诚、银行、客户不是 RBAC 角色。它们首先是业务参与方类型或机构类型；当这个机构需要自己的员工登录、组织架构、菜单权限和审批流程时，它同时也是一个租户。

不要建这种角色：

```text
role = 元丰行
role = 智诚
role = 银行
```

应该建：

```text
tenant = 元丰行
tenant_type = CHANNEL_COMPANY

tenant = 智诚担保
tenant_type = GUARANTEE_COMPANY

tenant = 某银行
tenant_type = BANK

role = 客户经理 / 风控员 / 审批人 / 管理员

case_participant = 某笔业务中，元丰行是接单方，智诚是担保方，某银行是开函方
```

### 4.1 核心模型

建议采用“平台统一部署 + 企业租户隔离 + 业务协同授权”的模型。

```text
platform
  管理租户、应用、菜单、接口资源、基础字典、行政区划、套餐、开通状态

tenant
  元丰行
  智诚担保
  其他融资性担保公司
  接入本系统的银行
  可选：需要企业门户能力的客户企业

identity_user
  全局登录身份。一个手机号/账号可以加入多个租户。

tenant_member
  某个身份账号在某个租户里的成员身份。
  例如 admin 在元丰行是平台管理员，在智诚是实施顾问。

org / post
  租户内部组织和岗位。

role / permission
  租户内部角色授权。

case / application
  保函申请主业务。

case_participant
  一笔业务里有哪些企业参与，每个企业是什么角色。

case_share / document_share
  跨租户共享资料、任务、审批结论和状态的授权边界。
```

### 4.2 为什么需要 tenant_member

`identity_user` 只回答“谁能登录”，不能回答“这个人在某个租户里是什么身份”。

同一个人可能在多个企业中出现：

```text
张三账号
  在元丰行：客户经理
  在智诚担保：外部协作顾问
  在平台：实施支持人员
```

所以正式模型应拆开：

```text
identity_user      全局账号
tenant_member      账号在某个租户内的成员身份
member_org_post    成员在租户内属于哪个组织/岗位
subject_role       成员或账号在租户内绑定哪些角色
```

没有 `tenant_member`，系统会长期混淆：

- 账号是否属于某个租户。
- 登录时是否允许选择某个租户。
- 用户管理列表到底列租户成员还是全局身份账号。
- 离职/禁用是禁用全局账号，还是禁用某个租户内成员身份。
- 一个客户经理跨多个租户时，权限和组织如何表达。

### 4.3 保函业务中的跨租户协同

不能把跨租户协同理解成“关闭租户隔离”。正确做法是：

```text
默认隔离
  元丰行看不到智诚全部业务。
  智诚看不到元丰行全部业务。
  银行看不到担保公司全部业务。

显式共享
  对某一笔保函申请，系统创建 case_participant。
  元丰行、智诚、银行只在该业务范围内看到被授权的字段、附件、任务、审批结论。
```

推荐模型：

```text
guarantee_case
  id
  case_no
  applicant_user_id
  applicant_customer_id
  product_type: COMMERCIAL_GUARANTEE / BANK_GUARANTEE
  source_tenant_id: 元丰行
  status

case_participant
  case_id
  tenant_id
  participant_type:
    SOURCE_CHANNEL       元丰行
    GUARANTEE_COMPANY    智诚等融资性担保公司
    BANK                 银行
    CUSTOMER_PORTAL      客户企业，可选
  role_in_case
  access_level
  joined_at

case_task
  case_id
  tenant_id
  assignee_member_id
  task_type
  status

case_document
  case_id
  owner_tenant_id
  document_type
  storage_id
  visibility_scope

case_document_share
  document_id
  target_tenant_id
  permission: VIEW / DOWNLOAD / UPDATE / SIGN
```

这个模型的关键点：

- 业务主单可以有一个发起租户，但不代表只有发起租户能参与。
- 每个参与租户看到的是“自己参与的业务视图”，不是全量业务库。
- 资料共享要按文件、字段、阶段授权，不能简单全量开放。
- 审批流按租户内部执行，但跨租户流转由业务编排连接。

## 5. 租户内与平台级数据边界

### 5.1 建议不按租户隔离的数据

这些是平台元数据，应全局维护，租户按授权使用：

| 数据 | 原因 |
|---|---|
| 应用定义 | 系统能力由平台统一发布 |
| 菜单定义 | 菜单是产品能力，不应每个租户复制一份 |
| 接口资源 | API 是平台能力 |
| 基础行政区划 | 全国行政区划是公共基础数据 |
| 系统级字典 | 如状态、操作者类型、登录域等基础字典 |
| 产品包/套餐 | 平台运营配置 |
| 机构类型基础档案 | 银行、担保公司类型等基础枚举 |

### 5.2 建议按租户隔离的数据

这些数据属于租户内部治理，应按 `tenant_id` 隔离：

| 数据 | 说明 |
|---|---|
| 组织架构 | 每个企业不同 |
| 岗位 | 每个企业不同 |
| 租户成员 | 每个企业自己的成员 |
| 角色 | 每个企业自己的角色 |
| 角色菜单授权 | 同一菜单能力，不同企业授权不同 |
| 审批流程定义 | 元丰行、智诚、银行流程不同 |
| 业务规则配置 | 材料清单、风控规则、额度规则等 |
| 通知模板 | 每个企业可有自己的短信/邮件/系统消息模板 |
| 操作日志 | 按租户审计 |
| 保函业务内部处理数据 | 每个企业自己的处理意见、审批记录、任务 |

### 5.3 需要跨租户授权的数据

这些数据不能简单“全局共享”或“完全隔离”，必须由业务参与关系控制：

| 数据 | 推荐控制方式 |
|---|---|
| 保函申请主信息 | `case_participant` 控制可见 |
| 客户资料 | 按申请、字段、资料类型授权 |
| 签约资料 | 按当前办理阶段和接收方授权 |
| 风控结论 | 可共享结论，不一定共享内部评分细节 |
| 银行递交资料 | 只给目标银行或接口通道 |
| 审批状态 | 可共享节点状态，内部审批意见按规则脱敏 |
| 附件 | `document_share` 控制查看/下载/更新/签署 |

## 6. 登录与身份建议

### 6.1 个人客户登录

客户以个人账号为准，建议进入客户门户，不进入企业管理后台。

```text
realm = CUSTOMER
actor_type = CUSTOMER_USER
tenant_id = 可为空或绑定到平台客户域
```

客户申请保函时，创建客户档案和保函申请，不创建企业租户，除非客户企业需要多人协同。

### 6.2 企业员工登录

元丰行、智诚、银行员工应先选择租户，或系统根据账号所属租户列出可选项。

```text
账号登录
  -> 查询该账号可进入的 tenant_member
  -> 用户选择企业
  -> token 写入 tenantId、memberId、realm、actorType、partyType、partyId、appCode
```

重点：登录时不能只校验租户存在，还必须校验账号是该租户成员，或拥有平台授权的跨租户支持身份。

### 6.3 平台运维/实施账号

平台人员可能需要进入多个租户处理问题。不要用全局超级权限绕过模型，应该显式授权：

```text
platform_support_grant
  user_id
  target_tenant_id
  scope
  reason
  start_time
  expire_time
  approved_by
```

这样可以保证进入租户的行为可审计、可过期、可追责。

## 7. Mango 当前租户实现现状

基于当前代码和数据库，Mango 已有这些能力：

| 能力 | 当前状态 |
|---|---|
| 租户表 | `sys_tenant` 已有，当前有芒果集团、A公司、B公司、C公司 |
| 登录选择租户 | `LoginCommand` 支持 `tenantId/tenantCode`，登录 token 写入租户上下文 |
| 租户上下文 | `MangoContextHolder` 持有 `tenantId/appCode/realm/actorType/partyType/partyId` |
| 行级租户插件 | MyBatis-Plus TenantLine 拦截器已启用 |
| 排除平台元数据表 | `sys_tenant`、字典、行政区划、应用、菜单、接口资源、身份用户等已排除 |
| 角色租户隔离 | `authorization_role` 已按 `tenant_id + app_code + role_code` 唯一 |
| 主体角色绑定 | `authorization_subject_role` 已带 `tenant_id/app_code/realm/actor_type/party_type/party_id` |
| 组织/岗位隔离 | `sys_org`、`org_post` 已带租户字段 |
| 用户菜单 | `/authorization/menus/user?fmt=tree` 按当前登录上下文返回菜单 |

当前测试数据现状：

```text
租户：
  1 芒果集团
  2 A公司
  3 B公司
  4 C公司

身份账号：
  admin 只有一个全局身份账号

角色：
  每个租户都有 ROLE_ADMIN

角色绑定：
  同一个 admin 被绑定到 4 个租户的 ROLE_ADMIN
```

这说明当前不是“每个租户一个独立超级账号”，而是“同一个全局账号可进入多个租户并获得对应租户角色”。

这个方向可以成立，但缺少正式的租户成员模型。

## 8. Mango 与理论最佳模型的差距

### 8.1 缺少 tenant_member

当前 `identity_user` 被排除在租户插件之外，但表上又存在 `tenant_id`、`party_type`、`party_id` 字段。这会造成概念混乱：

- `identity_user` 到底是全局账号，还是租户内用户？
- `identity_user.tenant_id` 是否可信？
- 用户管理页面管理的是全局账号还是租户成员？
- 一个账号加入多个企业时，成员状态、组织、岗位、离职如何表达？

建议新增：

```text
tenant_member
  id
  tenant_id
  user_id
  member_no
  display_name
  member_type: EMPLOYEE / EXTERNAL / SUPPORT
  status: ENABLED / DISABLED / LEFT
  primary_org_id
  primary_post_id
```

并逐步把租户内用户管理从 `identity_user` 迁移为“成员管理”。

### 8.2 登录时未强校验账号是否属于租户

当前登录流程会校验租户存在且启用，然后加载该租户下的角色权限。但模型上还缺少“账号是否允许进入该租户”的独立校验。

短期看，未绑定角色的账号可能登录成功但没有权限；长期看，这会让登录语义不严谨。

建议：

```text
登录选择租户
  -> 校验租户启用
  -> 校验 identity_user 与 tenant_member 关系存在且启用
  -> 校验成员可进入 appCode/realm/actorType
  -> 生成 token
```

### 8.3 用户、成员、主体角色边界不清

当前 `authorization_subject_role.subject_id` 指向的是 `identity_user.id`。如果未来引入 `tenant_member`，需要明确角色到底绑定给谁：

推荐：

- 登录账号级角色极少使用，只用于平台级特殊账号。
- 普通企业权限绑定给 `tenant_member`。
- 客户个人门户权限可绑定给 `customer_user` 或 `identity_user + CUSTOMER realm`。

否则同一个账号跨租户、跨身份时，权限边界会越来越难解释。

### 8.4 缺少租户类型和业务参与方建模

当前 `sys_tenant` 只有基础字段，无法区分：

- 平台运营方
- 渠道/接单方，元丰行
- 融资性担保公司，智诚等
- 银行
- 客户企业
- 外部合作方

建议扩展：

```text
tenant_type:
  PLATFORM
  CHANNEL_COMPANY
  GUARANTEE_COMPANY
  BANK
  CUSTOMER_COMPANY
  SERVICE_PROVIDER

tenant_capability:
  CAN_RECEIVE_APPLICATION
  CAN_ISSUE_COMMERCIAL_GUARANTEE
  CAN_REVIEW_FINANCING_GUARANTEE
  CAN_SUBMIT_TO_BANK
  CAN_PROCESS_BANK_GUARANTEE
```

### 8.5 缺少业务参与关系和跨租户共享模型

当前租户隔离主要靠 `tenant_id`，但保函业务一定存在跨租户协同。

缺少这些核心表/概念：

- `guarantee_case`
- `case_participant`
- `case_task`
- `case_document`
- `case_document_share`
- `case_audit_timeline`
- `case_handover`

没有这些模型，就会在实现业务时出现两种错误：

1. 为了协同绕过租户隔离。
2. 为了隔离导致业务流转不了。

### 8.6 缺少租户级流程配置

元丰行、智诚、银行的审批流程明显不同：

- 元丰行：接单、资料整理、签约、初审、递交下游。
- 智诚：尽调、风控、审批、反担保、递交银行。
- 银行：资料接收、授信/审查、开函、回传。

当前 Mango 尚未形成租户级流程定义、流程版本、流程授权、流程实例隔离能力。

建议后续引入：

```text
workflow_definition
  tenant_id
  business_type
  version
  status

workflow_instance
  tenant_id
  case_id
  definition_id

workflow_task
  tenant_id
  case_id
  assignee_member_id
```

### 8.7 缺少租户开通初始化

当前创建租户只是插入 `sys_tenant`，不会完整初始化：

- 默认组织根节点。
- 默认岗位。
- 租户管理员成员。
- 默认角色。
- 默认角色菜单授权。
- 默认流程模板。
- 默认业务参数。
- 默认材料清单。

建议创建租户时进入 provisioning 流程，而不是普通 CRUD：

```text
create tenant
  -> create tenant profile
  -> create root org
  -> create admin member
  -> create tenant admin role
  -> grant default menus
  -> initialize workflow templates
  -> initialize business parameters
  -> write provisioning audit
```

### 8.8 租户插件默认租户存在风险

当前租户插件无上下文时使用默认租户 `1`。这对开发方便，但生产上有风险：

- 某些接口忘记写入上下文时，可能落到平台租户。
- 定时任务、异步任务、消息消费如果没有显式租户上下文，可能污染默认租户数据。

建议：

- Web 请求缺少租户上下文时，对需要租户隔离的接口直接失败。
- 定时任务和异步任务必须显式声明 tenant scope。
- 平台级任务使用 `TenantScope.none()` 或 `TenantScope.platform()`，不能隐式使用默认值。

### 8.9 缺少跨租户审计和支持访问模型

保函业务涉及金融、担保、银行资料，平台人员跨租户查看资料必须受控。

建议补：

- 平台支持授权。
- 跨租户访问原因。
- 临时授权有效期。
- 敏感资料查看水印。
- 下载审计。
- 资料脱敏规则。

## 9. 投产前一步到位重构方案

当前系统尚未投产，不需要兼容历史租户数据，也不需要为了短期页面可用继续保留模糊模型。建议直接按正式模型重构，不再走“先凑合、后迁移”的路线。

### P0：冻结正式概念边界

立即明确：

- `identity_user` 是全局身份账号。
- `tenant` 是企业级隔离单元。
- `tenant_member` 是账号在租户里的成员身份。
- `org/post/role/workflow/business_config` 属于租户内部。
- `app/menu/api/base_dict/area` 属于平台元数据。
- 跨租户业务协作必须通过业务参与关系授权。

同时统一产品语言：

- 后台产品可显示“企业空间”或“机构空间”。
- 技术模型仍使用 `tenant`。
- 文档、代码、接口里必须明确：tenant 不是部门、不是角色、不是业务参与方。

### P1：直接建立正式租户成员模型

新增 `tenant_member`，并以它作为租户内用户管理的主模型：

- 登录租户列表只返回账号已加入的租户。
- 登录时校验成员状态。
- 用户管理页面改为“成员管理”，管理 `tenant_member`。
- 平台侧单独提供“身份账号管理”，管理 `identity_user`。
- 角色绑定主体改为 `tenant_member`，不再直接绑定 `identity_user`。
- 组织、岗位、上级、在职状态都挂到 `tenant_member` 或成员组织岗位关系。

建议表：

```text
tenant_member
  id
  tenant_id
  user_id
  member_no
  display_name
  member_type
  status
  primary_org_id
  primary_post_id
  joined_at
  left_at

tenant_member_org
  id
  tenant_id
  member_id
  org_id
  post_id
  primary_flag
```

### P2：重构登录与授权主链路

登录必须改为：

```text
输入账号密码
  -> 校验 identity_user
  -> 查询可进入的 tenant_member 列表
  -> 用户选择企业空间
  -> 校验 member 状态、租户状态、应用开通状态
  -> token 写入 tenantId、memberId、userId、appCode、realm、actorType
  -> 权限按 memberId + tenantId 加载
```

授权模型同步改造：

```text
authorization_subject_role.subject_type = TENANT_MEMBER
authorization_subject_role.subject_id = tenant_member.id
```

保留 `identity_user.id` 在 token 中用于识别登录账号，但业务权限不要再直接绑定全局账号。

### P3：租户开通改为 provisioning

租户管理不能只是 CRUD。创建企业空间时必须一次性初始化：

- 企业基础信息和 `tenant_type`。
- 根组织。
- 默认岗位。
- 租户管理员身份账号和成员。
- 租户管理员角色。
- 默认菜单授权。
- 默认流程模板。
- 默认业务参数。
- 默认材料清单。
- 开通审计记录。

建议表：

```text
tenant_profile
  tenant_id
  tenant_type
  institution_license_no
  contact_info
  status

tenant_capability
  tenant_id
  capability_code
  enabled
```

### P4：一次性设计保函业务协同模型

优先建立：

- `guarantee_case`
- `case_participant`
- `case_document`
- `case_document_share`
- `case_task`

用它解决元丰行、智诚、银行之间的资料流转和权限可见范围。

业务参与方不要复用 RBAC 角色，也不要复用租户本身：

```text
case_participant
  case_id
  tenant_id
  participant_type
  participant_role
  access_level
  status
```

### P5：建立租户级流程和配置

按租户维护：

- 商保流程。
- 银行保函流程。
- 担保公司内部审批流程。
- 银行资料递交流程。
- 材料清单。
- 风控规则。
- 通知模板。

### P6：补安全审计和跨租户支持

尤其针对金融资料：

- 跨租户访问授权。
- 敏感附件下载审计。
- 数据脱敏。
- 操作水印。
- 支持人员临时授权。
- 租户级日志导出。

## 10. 对当前 Mango 的直接处理建议

因为尚未投产，建议不要保留当前“身份用户直接承担租户用户管理”的模型。当前 T5 完成的身份用户 CRUD 可以作为平台账号管理的基础，但不应继续作为租户内成员管理的最终形态。

建议直接创建新的系统基础任务：

1. 新增 `tenant_member`、`tenant_member_org` 迁移。
2. 改造登录：租户选项从“所有启用租户”改为“当前账号可进入的企业空间”。
3. 改造 token：增加 `memberId`，保留 `userId`。
4. 改造权限：角色绑定主体切到 `tenant_member`。
5. 页面拆分：
   - 平台运营 / 账号管理：管理全局 `identity_user`。
   - 账号权限 / 成员管理：管理当前租户 `tenant_member`。
6. 改造租户创建为 provisioning。
7. 新增 `case_participant`、`case_document_share` 的设计和迁移，为保函业务做准备。
8. 清理旧测试数据：不要再使用一个 `admin` 直接绑定所有租户超级角色的模式，改为平台管理员账号 + 每个企业空间自己的管理员成员。

最终目标不是“所有表都加 tenant_id”，而是：

```text
平台元数据全局共享
租户内部治理数据严格隔离
跨租户保函业务通过显式参与关系共享
身份账号全局唯一
租户成员表达账号在企业内的身份
权限绑定到租户成员而不是全局账号
```
