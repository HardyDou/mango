# 逻辑应用与集成应用管理验收证据

## 1. 验收范围

- 菜单：`平台能力` → `集成管理` → `应用定义`、`集成应用`
- 页面：`#/integration/application`
- 接口：集成应用分页、详情、创建、更新、启停、轮换入站密钥、更新出站凭证和连接测试
- 权限：`internal-admin`，当前部署管理员 `admin`
- 数据：真实 MySQL 工作区数据库，测试数据使用 `E2E_INTEGRATION_` / `demo-partner-app` 前缀并已清理
- 部署形态：Admin 单体开发入口

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30021`
- 后端地址：`http://127.0.0.1:18021`
- 数据库：`mango_dev_mango_app_access_management_021`
- 测试账号：`admin`，密码不记录
- 浏览器：Chromium（Playwright）
- 出站测试目标：`http://127.0.0.1:18021/actuator/health`，仅在本机显式开启 `mango.authorization.integration.allow-private-network=true`

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-008 | TC-009 | `#/integration/definition` | 应用定义清单与连接器编辑器 | 连接器 `wecom-message` v1、草稿 `aliyun-sms` v1 | 列表展示版本/分类/能力/被引用数；“新增/编辑连接器”按页签分组：基础信息、配置规范、认证规范、能力；保存即生效，无版本概念，停用连接器不能创建实例 | 页签分组、可视化表格、能力标签、空态提示统一，无 JSON 文本框 | console error 0；API 4xx/5xx 0 | `connector-definition.png`、`connector-editor.png`、`connector-editor-specs.png`；`mango-ui/apps/mango-admin/e2e/specs/integration-application-management.spec.ts` | PASS |
| TASK-008 | TC-009 | `#/integration/definition`、`#/integration/application` | 支付渠道配置迁移模拟（不改支付模块） | 连接器 `wechat-pay`、`alipay`；实例 `wechat-pay-main` | 商户号/AppId/网关/账单拉取方式用配置规范表达；APIv3 密钥加密保存、证书字段保存文件标识；能力按方法授权；实例回调路径生成 | 配置规范表格支持枚举、认证规范支持证书类型声明 | console error 0；API 4xx/5xx 0 | `connector-definition.png`、`integration-instance-list.png`；本文件第 5 节缺口 | PASS |
| TASK-008 | TC-009 | `#/integration/application` | 按连接器动态渲染表单创建实例并加密保存凭证 | 连接器 `wecom-message`，实例 `wecom-e2e-<timestamp>` | 表单按配置规范与认证规范动态渲染；保存后列表显示已配置、回调路径 `/integration/callback/wecom-message/<实例>`；列表不含凭证原文 | 实例信息、连接器配置、凭证、能力授权分区可见 | console error 0；API 4xx/5xx 0 | `integration-instance-form.png`、`integration-instance-list.png` | PASS |
| TASK-003 | TC-004 | `POST /integration/credentials`、`/rotate` | 多字段凭证加密、轮换和脱敏 | 真实数据库凭证记录 | 入站密钥与出站凭证明文均不等于库内密文；轮换后密钥变化且版本递增；更新出站凭证后版本递增 | 凭证弹窗提示只展示一次；出站凭证只显示已配置 | 未记录任何凭证原文 | `IntegrationApplicationServiceTest` surefire 报告；数据库密文回读 | PASS |
| TASK-004 | TC-007 | `POST /authorization/frontend-apps/health-check` | 前端运行单元可达性探测与目标限制 | 已配置出站连接的集成应用 | 开启私网后真实请求返回 200 且 `success=true`；默认配置下回环目标返回“目标地址不在允许范围内” | 页面明确提示连接结果 | 请求成功，无 5xx；`OutboundHttpInvokerTest` 覆盖五种认证方式和目标限制 | `list.png`；surefire 报告 | PASS |
| TASK-008 | TC-009 | `#/system/micro-application` | 微应用总览只读展示、可达性与引用关系 | 运行单元 `mango-admin-rbac-app`（前端入口 `http://127.0.0.1:32021/`） | 页面只展示 `MICRO_APP` 运行单元，提示由部署资源声明登记，无新增入口；可达性探测返回 200 与耗时；引用关系可展开查看宿主和模块 | 统一搜索区、列表面板、可达性标签和引用弹层可见 | console error 0；API 4xx/5xx 0 | `micro-application.png`；`mango-ui/apps/mango-admin/e2e/specs/micro-application.spec.ts` | PASS |
| TASK-009 | TC-010 | `POST /authorization/frontend-apps/health-check` | 运行单元健康检查语义 | 已登记运行单元、未登记编码、回环目标 | 可访问返回成功和状态码；非成功状态码记录失败；未登记返回业务错误；默认拒绝私网目标 | 页面通过“检测”按钮触发并展示可达/不可达标签，无异常弹层 | 预期业务结果，无 5xx | `FrontendAppHealthServiceTest` surefire 报告 | PASS |
| TASK-003 | TC-003 | 创建和更新接口 | 非法输入校验 | 非法 IP 行、非法扩展参数、非 http 地址、重复编码、方向变更 | 非法输入返回业务错误且不写库；方向创建后不允许修改 | 页面错误由真实接口返回 | 预期业务错误，无 5xx | `IntegrationApplicationServiceTest` surefire 报告 | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| rbac | `#/system/micro-application` | 空列表可加载 | 只读无新增入口 | 登记来源提示和引用列可见 | `micro-application.png` | PASS |
| rbac | `#/integration/application` | 空列表可加载 | 创建后列表回显 | 目录菜单“应用管理”下可见“逻辑应用”“集成应用”，两页均使用框架统一搜索区和列表面板 | `menu.png`、`list.png` | PASS |
| rbac | `#/system/app` | 逻辑应用列表 | 逻辑应用编辑 | 未出现集成应用凭证字段 | 复用既有 `app-management-save` E2E | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 前端全量 typecheck | 仓库存在既有 workspace 类型错误，本次未新增诊断 | 不影响本次 Admin 生产构建，但不能宣称全仓类型检查通过 | 另行治理 workspace 类型构建链 | 待确认 |
| 接口级授权、Scope 校验和调用审计 | 本期明确不实现 | 集成应用只能做到应用级配置 | 后续独立需求 | 待确认 |
| OAuth2、MTLS、SFTP 出站方式 | 需要独立协议和安全设计 | 当前只覆盖 HTTP 系列和五种认证方式 | 后续按需扩展 | 待确认 |
| 集成应用行级数据权限 | 已确认本期不需要 | 当前部署内可见全部集成应用 | 如需组织隔离须新增需求 | 已确认 |
| 微应用人工登记与删除 | 注册表由部署资源声明维护，人工写入会与同步冲突 | 后台只读 | 需要人工接管时另立需求 | 已确认 |
| 集成应用运行时（调用/令牌/验签/审计） | 本模块只负责定义与声明，运行由业务域实现 | 业务域需自行实现消费 | 各业务域按需接入 | 已确认 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 集成应用登记、入站密钥与出站凭证生命周期、连接器和未实现边界 | `mango-docs/designs/application-integration-management/technical-design.md`、`mango/mango-platform/mango-authorization/README.md` | Core：`mvn -f mango/pom.xml -pl :mango-authorization-core verify`；前端：`pnpm --filter mango-admin build`；E2E：`pnpm --filter mango-admin test:e2e -- e2e/specs/integration-application-management.spec.ts` | 真实数据库；凭证不写入文档、日志或长期浏览器存储 | 本期不宣称接口级授权、调用审计和多租户运行时已交付 | EXCEPTION |

## 7. 决策复核记录

- **事实与用户视角：** 管理员通过 `internal-admin` 在当前部署登记合作系统，可配置入站接入与出站连接，创建和轮换时看到一次性入站密钥。依据：真实浏览器流程和 API 记录。
- **技术与风险视角：** 入站密钥与出站凭证独立加密，列表和详情只返回掩码；出站连接默认拒绝回环和私网目标，连接测试不回显凭证。依据：`IntegrationApplicationServiceTest`、数据库密文回读和真实连接测试。
- **验证与交付视角：** 文档 checker、Core verify、Admin 构建、Playwright E2E 均已通过；全仓 typecheck 仍保留既有错误。依据：本文件和对应命令输出。
- **评审选择：** `AGENT_ONLY`。当前未指定外部同行评审；本记录不表示人工审批。
