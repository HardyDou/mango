# 系统配置控制面板交付契约

## 1. 目标

落地 Issue #217：将 Mango 系统配置升级为按业务域管理的业务控制面板能力，统一“系统配置/参数配置”入口，并提供可复用前端配置面板组件。

## 2. 范围

- 后端系统配置追加面板元数据字段和展示类型枚举。
- 系统配置列表、详情、更新值接口支持元数据和不可编辑校验。
- 系统配置资源声明同步支持元数据。
- 前端 `@mango/system` 新增 `SystemConfigPanel` 组件。
- 系统配置页合并为单一“系统配置”入口并复用新组件。
- 文档补充组件使用方式和后端能力说明。

## 3. 不做什么

- 不做敏感配置脱敏或重置式编辑。
- 不做配置审计、审批、灰度、回滚。
- 不做租户差异化配置规则。
- 不做 JSON、日期时间、敏感配置等高级配置类型。
- 不新增业务域级后端权限模型。

## 4. 设计输入

- PRD：`mango-docs/designs/system-config-control-panel/system-config-control-panel-prd.md`
- 详细设计：`mango-docs/designs/system-config-control-panel/system-config-control-panel-design.md`
- 用户确认：合并系统配置/参数配置，新增传入 `domainCodes` 的前端通用组件，一期支持日期/日期区间。

## 5. 设计说明

### 5.1 影响模块

- `mango/mango-platform/mango-system/mango-system-api`
- `mango/mango-platform/mango-system/mango-system-core`
- `mango/mango-platform/mango-system/mango-system-starter`
- `mango-ui/packages/system`
- `mango-docs/designs/system-config-control-panel`

### 5.2 接口变化

- `GET /system/config/list`：响应追加配置面板元数据字段。
- `GET /system/config/detail`：响应追加配置面板元数据字段。
- `PUT /system/config/value`：保留 query 参数形式，增加禁用/不可编辑配置拒绝更新。
- `GET /system/config/value-types`：新增展示类型列表。

### 5.3 数据变化

`sys_config` 新增可空字段：`value_type`、`group_code`、`group_name`、`default_value`、`options`、`option_source`、`dict_type`、`editable`、`editable_reason`。

### 5.4 菜单/页面/权限变化

- 系统配置页沿用现有菜单和权限，不新增路由。
- 页面不再展示“系统参数 / 系统配置”两个重复 Tab。
- 后端权限沿用 `system:config:list/query/add/edit/delete`。

### 5.5 测试范围

- 后端：系统配置资源声明同步、配置值更新校验、展示类型接口。
- 前端：`@mango/system` 包构建，组件类型检查，系统配置页构建。
- 手工：配置页业务域 Tab、卡片状态、直接编辑、详情弹窗、空态/失败态。

## 6. 风险与限制

- 敏感配置规则未确认，本次不实现敏感类型。
- 历史配置没有元数据时按文本配置兼容展示。
- 并发编辑沿用后写覆盖现状。
- 业务域 Tab 展示名优先使用调用方传入 label，否则展示业务域编码。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | AC-001/AC-015 | 系统配置页按业务域展示且不再展示重复 Tab | `ConfigView` 复用 `SystemConfigPanel` | `mango-ui/packages/system/src/views/config/index.vue` | 前端构建 + 代码检查 | DONE | `pnpm --filter @mango/system build` 通过；页面文件已替换为统一配置入口 |
| TASK-002 | AC-002/PG-002 | 通用组件支持多个业务域 | `SystemConfigPanel` 接收 `domainCodes` | `mango-ui/packages/system/src/components/SystemConfigPanel/index.vue` | 前端构建 + 组件代码检查 | DONE | `pnpm --filter @mango/system build` 通过；组件 props 包含 `domainCodes` |
| TASK-003 | AC-003/AC-004 | 配置卡片展示名称、当前值、介绍和操作项 | 卡片按 `valueType` 渲染控件 | `SystemConfigPanel` | 前端构建 + 组件代码检查 | DONE | `pnpm --filter @mango/system build` 通过；卡片模板覆盖标题、介绍、状态、控件和详情入口 |
| TASK-004 | AC-005/AC-006 | 空态和失败态可见且可重试 | 组件维护 loading/error/empty 状态 | `SystemConfigPanel` | 前端构建 + 组件代码检查 | DONE | `pnpm --filter @mango/system build` 通过；组件模板包含 loading、empty、error 和 retry 分支 |
| TASK-005 | AC-007/AC-008/AC-009 | 支持开关、文本、数字、单选、下拉、多选、日期、日期区间直接编辑 | `valueType` 映射控件，多选和日期区间存 JSON 数组字符串 | 前端组件 + 后端配置值接口 | 前端构建 + 后端测试 + 浏览器验收 | DONE | `pnpm --filter @mango/system build` 通过；`panel-polish-acceptance.json` 覆盖八类配置卡片 |
| TASK-006 | AC-010/AC-011 | 保存成功刷新，保存失败保留原值 | 保存失败恢复本地旧值 | `SystemConfigPanel`、`SysConfigServiceImpl` | 前端构建 + 后端测试 | DONE | `pnpm --filter @mango/system build` 通过；`SysConfigServiceImplTest` 覆盖保存成功和拒绝保存分支 |
| TASK-007 | AC-012/BR-005 | 不可编辑配置只读 | 新增 `editable`、`editableReason` 并后端兜底拒绝保存 | API/entity/migration/service | 后端测试 | DONE | 后端目标测试通过；`SysConfigServiceImplTest` 覆盖 disabled 与 readonly 拒绝保存 |
| TASK-008 | AC-013/AC-014 | 配置详情展示完整信息，取消不影响原值 | 详情弹窗使用编辑副本 | `SystemConfigPanel` | 前端构建 + 组件代码检查 | DONE | `pnpm --filter @mango/system build` 通过；详情弹窗使用 `detailValue` 编辑副本 |
| TASK-009 | BO-001/BO-003 | 配置元数据可持久化和资源声明同步 | `sys_config` 追加元数据字段，资源处理器支持字段，默认配置通过 `mango-resource` 注入 | migration、entity、数据传输对象、resource handler、resource yml | 后端集成测试 | DONE | `SystemConfigResourceHandlerIntegrationTest` 通过；新增 `V6__system_config_panel_metadata.sql`、`V7__system_config_option_sources.sql` |
| TASK-010 | 文档要求 | 更新组件和后端 README | 说明新组件与配置元数据 | README/组件 README | 文档检查 | DONE | `mango/mango-platform/mango-system/README.md`、`mango-ui/packages/system/README.md`、`mango-ui/packages/system/src/components/README.md` 已更新 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | `/#/system/config` | 统一入口 | `selectedDomains=['COMMON']` | 页面复用 `SystemConfigPanel`，页面正文不再保留“系统参数 / 系统配置”重复 Tab | 浏览器验收通过：登录后进入系统配置页，业务域 Tab 与卡片可见 | console error 0，pageerror 0，目标接口 4xx/5xx 0 | `mango-docs/evidence/2026-06-23-issue-217-system-config-panel/acceptance-result.json`；`screenshots/03-system-config-page.png` | PASS |
| TASK-002 | `SystemConfigPanel` | 多业务域 Tab | `domainCodes=['COMMON']`，组件能力支持外部传入多个业务域 | Tab 来源为业务域列表，按业务域请求 `/api/system/config/list?domainCode=COMMON` | 浏览器验收通过：`COMMON` Tab 可见，卡片列表按业务域渲染 | `GET /api/system/config/list?domainCode=COMMON` 返回 200 | `mango-docs/evidence/2026-06-23-issue-217-system-config-panel/acceptance-result.json`；`screenshots/03-system-config-page.png` | PASS |
| TASK-003/TASK-008 | `SystemConfigPanel` | 卡片与详情弹窗 | `通知中心总开关` 等真实业务域配置 | 卡片显示配置名称、配置介绍、展示类型和详情入口；详情弹窗显示完整配置信息 | 浏览器验收通过：卡片可见，点击详情弹出“配置操作”弹窗 | console error 0，pageerror 0，目标接口 4xx/5xx 0 | `mango-docs/evidence/2026-06-23-issue-217-system-config-panel/panel-polish-acceptance.json`；`screenshots/07-polished-panel-dialog.png` | PASS |
| TASK-005/TASK-006 | `/system/config/value` | BOOLEAN 开关直接编辑并保存 | `Issue217浏览器验收开关`，初始值 `true` | 点击卡片开关后调用保存接口，接口返回成功 | 浏览器验收通过：开关点击后页面保持可用并完成保存 | `PUT /api/system/config/value?id=...&value=false` 返回 200，目标接口 4xx/5xx 0 | `mango-docs/evidence/2026-06-23-issue-217-system-config-panel/screenshots/05-config-after-switch-update.png` | PASS |
| TASK-007 | `/system/config/value` | 不可编辑拒绝保存 | `editable=false`、`status=0` | 后端返回失败，前端控件按只读/禁用状态不可编辑 | 后端目标测试覆盖 disabled 与 readonly 拒绝保存分支 | 后端目标测试通过 | 后端测试输出；`SysConfigServiceImplTest` | PASS |

## 9. 验证记录

- 前端依赖安装：`pnpm install --frozen-lockfile` 通过，lockfile 未重新解析。
- 前端依赖包构建：`pnpm --filter @mango/common build` 通过，用于生成 `@mango/common` 子路径导出产物。
- 前端目标包构建：`pnpm --filter @mango/system build` 通过。
- 后端目标测试：`mvn -pl mango-platform/mango-system/mango-system-core -am -Dtest=SysConfigServiceImplTest,SystemConfigResourceHandlerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过，10 tests，0 failures，0 errors。
- 浏览器运行态验收：
  - 启动命令：`scripts/dev-workspace.sh start`。
  - 后端地址：`http://127.0.0.1:18965`，`/actuator/health` 返回 `UP`。
  - 前端地址：`http://127.0.0.1:8655`，入口返回 200。
  - 数据库：`mango_dev_37cdfa`。
  - 账号标识：`admin / tenantCode=default / tenantName=芒果集团`，未记录密码或明文 token。
  - 验收脚本：`.runtime/issue-217-browser-acceptance/run-acceptance.cjs`。
  - 结果证据：`mango-docs/evidence/2026-06-23-issue-217-system-config-panel/acceptance-result.json`。
  - 截图证据：`mango-docs/evidence/2026-06-23-issue-217-system-config-panel/screenshots/01-login-page.png`、`02-home-after-login.png`、`03-system-config-page.png`、`04-config-detail-drawer.png`、`05-config-after-switch-update.png`。
  - 运行结果：PASS；console error 0；pageerror 0；目标接口 4xx/5xx 0。
- 操作面板精修验收：
  - 验收脚本：`.runtime/issue-217-panel-polish/check-panel.mjs`。
  - 结果证据：`mango-docs/evidence/2026-06-23-issue-217-system-config-panel/panel-polish-acceptance.json`。
  - 截图证据：`screenshots/06-polished-panel-overview.png`、`07-polished-panel-dialog.png`、`08-polished-panel-workflow-tab.png`。
  - 运行结果：PASS；console error 0；pageerror 0；目标接口 4xx/5xx 0；覆盖开关、文本、数字、单选、下拉、多选、日期、日期区间。

## 10. 未执行项与风险

- `pnpm install` 使用 pnpm 10 安全策略，提示若干依赖构建脚本被忽略；本次目标构建已通过。
- 本地 dev 前端首次启动因缺少包内 `dist/style.css` 样式产物出现 Vite CSS 500；已执行 `pnpm -F @mango/admin run build:style-deps` 生成样式依赖后重启服务，最终浏览器验收通过。
