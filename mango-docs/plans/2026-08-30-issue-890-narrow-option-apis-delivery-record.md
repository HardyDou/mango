# 标准交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #890
- 交付模式：STANDARD
- 需求影响：L2 - Workflow 与 Home 管理页面被跨模块高权限列表接口阻断，且候选数据涉及权限与租户边界。
- 方案风险：L2 - 新增领域只读选项契约并通过平台 API 适配数据，必须限制返回字段、权限码和当前租户范围。
- 最终风险：L2
- 工作区决策：REUSE（`/Users/hardy/Work/Yunxin/mango-issue-890`，`fix/issue-890-narrow-options`）
- 启用能力：M01、M08、M09、M10、M11、M12、M13

## 2. 目标与范围

- 目标：让流程模板、首页列表和用户首页页面只调用所属领域的窄选项接口，不再要求机构管理或成员管理列表权限。
- 成功条件：流程模板页面挂载不请求机构列表，打开推送弹窗后使用 `workflow:template:push` 查询启用机构；Home 两页使用各自页面权限查询当前租户启用成员的最小候选字段；三个页面不再出现相关 403。
- 处理范围：Workflow 模板目标机构 Provider/API、Home 用户候选 Provider/API、对应前端调用、权限/API/组件契约测试和能力说明。
- 不处理范围：给页面追加 `system:tenant:list` 或 `system:user:list`、修改机构/成员管理接口权限、发布 Maven/npm 物料、部署或关闭 Issue。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | 流程模板页面 | 拥有 `workflow:template:*`，没有 `system:tenant:list` | 页面挂载不查询机构；打开推送弹窗后按需读取启用机构选项 | Provider 缺失或上游失败时明确失败，不伪装空成功 | 前端契约、Controller 权限和 Provider 测试通过 |
| REQ-002 | 首页列表 | 拥有 `home:list:view`，没有 `system:user:list` | 通过 Home API 查询当前租户启用成员的最小候选字段 | Identity 缺失或失败时明确失败，页面候选为空但首页列表仍可重试 | API、Provider 和页面调用契约通过 |
| REQ-003 | 用户首页 | 拥有 `home:user:view`，没有 `system:user:list` | 按关键词查询当前租户启用成员，并回填用户与成员 ID | 不返回其它租户成员，不暴露成员管理详情字段 | API、Provider 和页面调用契约通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001 | Workflow 新增模板目标机构 Provider 与 `GET /workflow/templates/tenant-options`，权限固定为 `workflow:template:push` | `mango-workflow-api/core/starter`、`@mango/workflow` | 删除新增契约并恢复前端调用，不涉及数据修复 |
| DEC-002 | REQ-002、REQ-003 | Home 新增用户候选 Provider/API；两个 HTTP 方法分别使用 `home:list:view`、`home:user:view`，返回用户 ID、成员 ID、显示名和用户名 | `mango-home-api/core/starter`、`@mango/home`、`@mango/admin-shell` | 删除新增契约并恢复前端调用，不涉及数据修复 |
| DEC-003 | REQ-001~REQ-003 | 平台 API 只在 Starter Adapter 内消费；前端不再直接调用 System/Identity 管理端点 | Workflow/Home Adapter 与页面 | 单点回滚 Adapter/API/页面改动 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | DEC-001 | 1 | Workflow API、Core、Starter、前端包 | 推送机构按需加载且权限/Provider 契约通过 |
| IMP-002 | DEC-002 | 2 | Home API、Core、Starter、前端包、Admin Shell | 两个页面不再引用 Identity 管理 API |
| IMP-003 | DEC-003 | 3 | 后端定向测试、前端测试、README 与能力地图 | 静态、单元、集成、API 和页面验证完成 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001 | M09/M10/M12 | Workflow Provider、Controller/API、API 面指纹和前端端点测试 | PASS | Maven 定向测试中 Workflow 13 个用例通过；`@mango/workflow` 10 个测试文件、52 个用例通过 |
| REQ-002、REQ-003 | M09/M10/M11/M12 | Home Provider、Service、Controller/API、Admin Shell 页面调用测试 | PASS | Maven 定向测试中 Home Core 2 个、Home Starter 7 个用例通过；`@mango/admin-shell` 15 个测试文件、73 个用例通过 |
| REQ-001~REQ-003 | M13 | workspace `mango_001` 启动后以真实管理员账号访问三个页面并检查 Network/Console | PASS | `/home-management/list` 只调用 `/api/home/options/page-users?size=200`；`/home-management/user` 只调用 `/api/home/options/visible-users?size=50`；Workflow 挂载不查询机构，打开推送弹窗后调用 `/api/workflow/templates/tenant-options`；上述请求均为 200，无 Console error |

补充门禁结果：Home、Workflow、Admin Shell 及依赖拓扑构建通过；前端架构和边界检查通过；模块 README 审计、README source facts 审计、STANDARD 记录检查和 `git diff --check` 通过；目标页面静态搜索不再包含 `/system/tenant/list`、`/identity/users/page` 或 `userApi`。

## 7. 例外与剩余风险

- 当前未获得 Commit、Push、PR、合并、发布或部署授权。
- 浏览器验收使用本地冷库中的平台管理员账号，运行时请求和页面交互已覆盖；“仅有页面自身权限且没有 System/Identity 管理权限”的独立低权限账号未单独创建，权限码由 Controller/API 合同测试覆盖。
- Workflow 推送弹窗出现 2 条既有 Element Plus `el-radio label` 弃用 warning，无 Console error；该 warning 不影响本次窄接口和权限行为，未在本任务中扩张修复范围。
