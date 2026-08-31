# 标准交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #909
- 交付模式：STANDARD
- 需求影响：L2 - 用户管理的重要列表、组织过滤、成员创建和角色展示行为错误，涉及租户成员、组织关系与授权结果。
- 方案风险：L2 - 方案跨 Identity、Org、Authorization 与 RBAC 页面，包含事务写入和公共 API 调整，但不修改数据库结构且可按模块回滚。
- 最终风险：L2
- 工作区决策：REUSE（`/Users/hardy/Work/mango-issue-909-user-management`，`issue-909-user-management`）
- 启用能力：M01、M08、M09、M10、M11、M12、M13

## 2. 目标与范围

- 目标：按方案 A 修复用户管理的组织范围、成员新增、已有成员搜索、角色展示和技术字段暴露问题。
- 成功条件：默认可查看全部成员；组织节点按本级及下级过滤；可明确清除组织选择；新增账号能原子建立用户、租户成员和目标组织关系；已有成员搜索与排除正确；列表一次批量展示用户直接角色；表单只展示业务可读机构和部门信息。
- 处理范围：Identity 用户/成员契约与事务、Org 组织范围与成员命令、Authorization 成员角色摘要、RBAC 用户管理页面/API、相关 README、能力地图、后端定向测试和 Mango Admin 永久 UI/E2E 用例。
- 不处理范围：数据库表、字段或 migration；继承角色或数据权限计算；角色分配交互重做；发布、部署、提交、Push、PR、Issue 关闭或任务 worktree 清理。

## 3. 可观察系统要求

| ID      | 参与者或入口   | 输入或前置条件                           | 预期行为                                                                                  | 失败语义                                         | 验收标准                                 |
| ------- | -------------- | ---------------------------------------- | ----------------------------------------------------------------------------------------- | ------------------------------------------------ | ---------------------------------------- |
| REQ-001 | 用户管理列表   | 打开页面                                 | 组织树顶部固定显示并默认选择“全部成员”，列表查询当前租户全部成员                          | 查询失败显示明确错误且不保留错误结果             | 页面、请求和空状态验证通过               |
| REQ-002 | 用户管理组织树 | 选择集团、公司或部门                     | 列表显示该节点本级及所有下级组织成员                                                      | 非当前租户、禁用或不存在节点被后端拒绝           | 层级范围单元/API/UI 验证通过             |
| REQ-003 | 用户管理组织树 | 已选择具体组织                           | 提供明确的“清除选择”动作，执行后回到“全部成员”                                            | 清除后不得继续携带旧组织过滤                     | 页面交互和请求参数验证通过               |
| REQ-004 | 新增用户       | 选择具体部门后点击新增                   | 表单预选该部门；提交后一次成功建立账号、当前租户成员和该部门成员关系                      | 任一写入失败时整体回滚，不留下部分用户或成员数据 | 事务集成测试和真实页面新增验证通过       |
| REQ-005 | 新增用户表单   | 打开表单                                 | 不显示登录域、操作者类型、归属主体类型和归属主体 ID；显示只读所属机构和可读的完整部门路径 | 技术默认值由后端确定，前端不得从会话复制主体 ID  | 表单与请求 payload 验证通过              |
| REQ-006 | 添加已有成员   | 选择具体组织并打开“添加已有成员”         | 一个关键词对用户名、姓名、手机号、邮箱执行 OR 搜索                                        | 空关键词返回可选成员；查询失败明确提示           | 查询单元/API/UI 验证通过                 |
| REQ-007 | 添加已有成员   | 组织内已有或其它页面存在成员             | 后端排除已经属于目标组织的成员，不依赖当前列表页数据                                      | 重复添加返回明确业务失败，不产生重复关系         | 排除与重复添加测试通过                   |
| REQ-008 | 用户管理列表   | 当前页包含多个成员                       | 直接展示每个成员已分配的直接角色名称                                                      | 角色查询失败不伪装成“无角色”，页面给出错误反馈   | 每页一次批量请求、角色映射和 UI 验证通过 |
| REQ-009 | 用户管理页面   | 组织树、列表、弹窗处于加载、空或错误状态 | 布局稳定，无字段溢出、遮挡或无反馈状态                                                    | Console、Vue 或相关 Network 错误不得判定通过     | Chromium 截图、console/network 记录通过  |

## 4. 技术决定

| ID      | 对应要求         | 接口/数据/权限/兼容性决定                                                                                                                                     | 影响路径                                  | 回滚方式                                                   |
| ------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------- | ---------------------------------------------------------- |
| DEC-001 | REQ-001~REQ-003  | Org 负责校验当前租户组织并解析包含自身的后代 ID；Identity 分页接受有界组织 ID 集合并按任一组织关系过滤                                                        | `mango-org-*`、`mango-identity-*`、RBAC   | 删除新增组织范围契约并恢复精确组织过滤                     |
| DEC-002 | REQ-004、REQ-005 | Org 暴露面向管理端的新增组织成员命令，先校验组织事实，再调用 Identity 受信内部命令；Identity 在单事务中写用户、租户成员和组织关系，技术默认值由 Identity 确定 | `mango-org-*`、`mango-identity-*`、RBAC   | 移除复合命令并恢复原用户新增入口；无 schema 或数据迁移回滚 |
| DEC-003 | REQ-006、REQ-007 | Identity 提供目标组织可选成员查询，关键词使用单个 OR 条件，数据库侧排除目标组织既有关系；Org 添加关系前再次校验避免竞态重复                                   | `mango-identity-*`、`mango-org-*`、RBAC   | 删除候选查询并恢复原成员查询                               |
| DEC-004 | REQ-008          | Authorization 提供当前租户、当前应用、最多一页成员 ID 的直接角色摘要批量查询；只返回直接绑定且有效的角色，不计算继承角色                                      | `mango-authorization-*`、RBAC             | 删除批量摘要接口并移除角色列                               |
| DEC-005 | REQ-009          | 页面使用业务语义锚点和现有 Element Plus 结构；所有读取请求防止陈旧响应覆盖并提供加载、空和错误反馈                                                            | `mango-ui/packages/rbac`、Mango Admin E2E | 回滚页面和永久用例改动                                     |

## 5. 实施清单

| ID      | 对应决定         | 顺序 | 改动路径                                   | 完成条件                                   |
| ------- | ---------------- | ---- | ------------------------------------------ | ------------------------------------------ |
| IMP-001 | DEC-001、DEC-002 | 1    | Identity 与 Org API/Core/Starter/Remote    | 层级过滤与原子新增契约、实现和测试通过     |
| IMP-002 | DEC-003          | 2    | Identity 与 Org API/Core/Starter/Remote    | 候选 OR 搜索、数据库排除和重复关系保护通过 |
| IMP-003 | DEC-004          | 3    | Authorization API/Core/Starter/Remote      | 批量直接角色摘要通过测试且无逐行请求       |
| IMP-004 | DEC-001~DEC-005  | 4    | `@mango/rbac` 用户页面与 API               | 方案 A 全部页面行为和类型检查通过          |
| IMP-005 | DEC-001~DEC-005  | 5    | 模块 README、能力地图、永久 E2E 与验收证据 | 能力审计、定向验证和真实浏览器验收完成     |

## 6. 验收映射与结果

| 要求 ID         | 验证方式        | 命令或步骤                                                                             | 结果                                                                                               | 证据                                                                                                                                              |
| --------------- | --------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| REQ-001~REQ-003 | M09/M10/M12/M13 | Org/Identity 定向测试；RBAC API Vitest；Chromium `@user-management` 组织切换与清除选择 | PASS：Org 11、Identity 30、前端 4、Chromium 2 条通过                                               | [验收证据 TC-001~TC-003](../evidence/2026-09-01-issue-909-user-management/acceptance-evidence.md)；`02-all-members-after-clear-org.png`；P0 trace |
| REQ-004~REQ-007 | M10/M11/M12/M13 | Identity/Org 事务与候选查询测试；真实新增账号、邮箱过滤、排除和加入已有成员            | PASS：Identity 30、Org 11；真实新增与加入链路通过且用例数据已清理                                  | [验收证据 TC-004~TC-007](../evidence/2026-09-01-issue-909-user-management/acceptance-evidence.md)；`01-member-role-assignment.png`；P0 trace      |
| REQ-008         | M10/M12/M13     | Authorization 定向测试；浏览器监听批量角色请求、执行角色分配并断言角色单元格           | PASS：Authorization 7；角色批量请求计数和真实角色名称回显断言通过                                  | [验收证据 TC-008](../evidence/2026-09-01-issue-909-user-management/acceptance-evidence.md)；`01-member-role-assignment.png`；P0 trace             |
| REQ-009         | M09/M13         | RBAC 生产构建与类型生成；Chromium 正常链路诊断及受控 500 错误态                        | PASS：RBAC 725 modules 构建成功；Chromium `2 passed (11.3s)`；正常链路诊断为空，错误态显示重试入口 | [验收证据 TC-009](../evidence/2026-09-01-issue-909-user-management/acceptance-evidence.md)；`03-retryable-list-error.png`；P1 trace               |

### 6.1 最终验证命令

```bash
mvn -f mango/pom.xml \
  -pl mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-identity/mango-identity-core,mango-platform/mango-org/mango-org-core \
  -am \
  -Dtest=RoleServiceImplIntegrationTest,IdentityUserServiceIntegrationTest,SysOrgServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test

PATH=/Users/hardy/.nvm/versions/node/v22.23.1/bin:$PATH \
  pnpm -C mango-ui -F @mango/rbac build

PATH=/Users/hardy/.nvm/versions/node/v22.23.1/bin:$PATH \
  pnpm -C mango-ui exec vitest run \
  packages/rbac/src/api/__tests__/user-management.spec.ts \
  packages/rbac/src/api/__tests__/post.spec.ts

PATH=/Users/hardy/.nvm/versions/node/v22.23.1/bin:$PATH \
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
  pnpm exec playwright test \
  --config apps/mango-admin/playwright.config.ts \
  --project chromium \
  --grep @user-management \
  --trace on
```

浏览器命令在 `mango-ui` 目录执行，结果为 `2 passed (11.3s)`。后端命令结果为 37 个 Reactor 模块成功，Authorization 7、Identity 30、Org 11 条测试通过。前端构建转换 725 个模块并生成类型，Vitest 为 2 个文件、4 条测试通过。

质量与证据命令：

```bash
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
node mango-pmo/tools/test-quality-check.mjs --base origin/main
pnpm -C mango-ui e2e-selectors:check
node mango-pmo/tools/acceptance-evidence-check.mjs \
  --evidence mango-docs/evidence/2026-09-01-issue-909-user-management/acceptance-evidence.md
git diff --check
```

README 结构和源码事实均为 `OK`；测试质量检查覆盖 8 个改动测试文件；E2E 选择器治理通过；验收证据检查识别 9 行并通过。

## 7. 例外与剩余风险

- 当前未获得 Commit、Push、PR、合并、发布、部署、Issue 关闭或清理授权。
- 本地运行态最初缺少“组织架构”和“成员管理”菜单权限，根因是 demo 菜单包出现后账号/组织菜单 Resource 未重放。该问题属于 Issue #910；本次只在独立验收数据库中重放 Resource 以取得正式菜单入口，临时 Resource 版本修改已从 #909 源码恢复，#909 不包含菜单修复。
- 验收数据库 `mango_dev_mango_issue_909_user_management_001` 保留；浏览器用例创建的 `E2E_USER_*`、`E2E_CANDIDATE_*`、`E2E_TARGET_*` 和 `E2E_SOURCE_*` 租户成员或组织数据已按正式 API 清理。
- 方案 A 已在当前源码工作树和本地真实运行态通过定向测试与 Chromium 验收；尚未进入发布物与部署环境验证，不能据此声明任一正式版本已包含修复。
