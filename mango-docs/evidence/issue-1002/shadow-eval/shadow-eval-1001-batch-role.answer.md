已读完 `mango-pmo/rules/03-ai-coding-redlines.md` 和 `mango-pmo/agents/03-dev-agent.md`，并按 `REDLINE-REUSE-010` 先核对了 `mango-platform/mango-authorization`、`mango-identity`、`mango-org`、`mango-system` 与 `mango-ui/packages/rbac` 的现有实现。以下只给方案，不改文件。

**结论先说**：这个需求大部分已在授权模块里存在，缺的只有"成员集合解析"这一层；可以做到零新增表、零新增依赖、零新增权限码。但"仅租户管理员或授权管理员可执行"和"刷新授权缓存"两项与现有事实不符，需要先确认口径，不能自行造。

---

## 1. 复用判断（REDLINE-REUSE-010）

按"能力 → 模块实现 → 标准库 → 已装依赖 → 新增"顺序逐项落地：

| 需求点 | 复用对象（1/2 级） | 放弃新增的原因 |
|---|---|---|
| 按 tenantId+appCode+realm+actorType+roleCode 定位角色 | `RoleService.findRoleId(RoleLookupQuery)`，字段已完全覆盖 | 无需新查询结构 |
| 幂等创建、不产生重复关系 | `RoleService.ensureSubjectRoleBinding(SubjectRoleBindingCommand)`（先 count 再 insert）+ `SubjectRoleBindingMapper` + `authorization_subject_role` 唯一键 `uk_..._subject_role`（subject_type+subject_id+role_id+tenant_id+app_code+party_type+party_id） | DB 约束已保证幂等，禁止在应用层另造去重表 |
| 批量解绑/回滚 | `RoleService.deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand)`，已支持 10000 条上限的批量按主体删除 | 无需新解绑命令 |
| 角色属于当前租户校验 | `RoleService.isCurrentTenantRole / areCurrentTenantRoles` 已有同租户判定，`RoleService.assignRoles` 的拒绝分支已写好 | 不再写第二套租户校验 |
| 成员属于当前租户、排除停用成员 | `IdentityUserApi.page(IdentityUserPageQuery)`（已有 `status`、`realm`、`actorType`、`partyType`、`partyId` 过滤）与 `listUserInfosByTarget` → `IdentityUserService.currentTenantOrgUserIds`（已过滤 `left_at is null`，可传组织 ID 集合）；`LocalTenantMemberProvider` 的 `getMember/listMembers` 用于逐条终态复核 | 复用 Identity 的租户与停用口径，不在授权模块重查 identity 表 |
| 指定组织 / 组织及下级 | `SysOrgApi.memberScope(orgId)` 已实现"本级 + 有下级"（内部校验启用、同租户、拒绝跨租户） | 不再写组织树递归 |
| 指定用户 ID | `ListTenantMembersRequest` / `TenantMemberProvider.listMembers(memberIds)` 批量成员校验 | 复用现有批量请求类型 |
| 权限门禁 | `@ApiAccess(mode = PERMISSION, permission = "authorization:role:assign")` + `AuthorizationCode.AUTHORIZATION_FORBIDDEN` | 见第 2 项：权限码不新增，但语义要确认 |
| 审计 | `@ApiAccess` 声明 + `mango-system` 的 `RecordOperationLogCommand` / `SysOperationLogApi`（操作日志表已存在） | 不新增事件表、不新增生命周期事件类型 |
| 授权缓存刷新 | `ApiResourceAuthorizationManager.hasPermission` 每次请求实时调 `authorizationProvider.load(query)`；`ApiResourceService` 的进程内缓存只覆盖 API 资源决策，不含成员角色 | **无对应缓存可刷**，见第 3 项验证入口 |
| 前端入口 | `mango-ui/packages/rbac`：`role.ts` 的 `assignSubjectRoles`、`user/index.vue` 的"分配成员角色"弹框、`org.ts` 组织树、`components/OrganizationSelector`（`mango-admin/e2e/specs/org-selector.spec.ts` 已验收） | 复用现有页面与组织选择器，不新建授权管理页 |

**真的需要新增的结构（仅 3 个，全部在 `mango-authorization-api`，消费者明确）**：

1. `BatchSubjectRoleBindingCommand`（或沿用模块内 `XxxRequest` 惯例命名）：tenantId、appCode、realm、actorType、roleCode、target（范围类型：`ALL` / `USER_IDS` / `ORG` / `ORG_AND_CHILDREN` 中的既有取值）、userIdIds、orgId。
2. 预览结果 VO：成员数 + 成员清单（memberId、userId、username、displayName、orgId）。
3. 执行结果 VO：成功数、跳过（已存在）数、失败明细（memberId + 原因码）。

2 与 3 也可合并为一个 `SubjectRoleBatchResultVO`，但预览与执行是同一入口的两种模式，合并会引入可空字段，建议分开。

**明确放弃**：新增数据库表、新增 Mapper 方法以外的 SQL、新增 Flyway migration、新增 Maven 依赖（`mango-authorization-starter` 已依赖 `mango-identity-api` 与 `mango-org-api`）、新增 SPI 扩展点。

---

## 2. 术语（REDLINE-TERMS-030）

**已有的主术语，必须沿用，不得换名**：

| 概念 | 现有主术语 | 出处 |
|---|---|---|
| 主体角色绑定 | subject role binding / `authorization_subject_role` | `SubjectRoleBindingEntity`、`RoleBindingApi` |
| 批量范围抽象 | 无（现有 `SubjectRoleBatchRequest` 只用于"批量查询角色"） | 不要复用这个已占用"查询"语义的名字 |
| 主体类型 | `TENANT_MEMBER`、`user`、`ANONYMOUS` | `AuthorizationQuery` 常量 |
| 操作者/归属上下文 | `realm`、`actorType`、`partyType`、`partyId` | `AssignSubjectRolesCommand` |
| 角色业务键 | `roleCode` | `RoleEntity`、`RoleLookupQuery` |
| 组织范围 | "指定组织"、"组织及下级" | `SysOrgApi.memberScope`、`role/index.vue` 数据范围文案 |
| 操作日志 | 操作日志（operation log） | `SysOperationLogApi` |

**新增公共标识**：2-3 个 API 层类型 + 1 个 HTTP 路径。命名建议 `BatchSubjectRoleBindingCommand` / `SubjectRoleMemberVO` / `SubjectRoleBatchResultVO`，路径沿用 `/authorization/roles/subject-bindings/...`（与 `RoleBindingController` 的 `/subject-bindings/ensure`、`DELETE /subject-bindings` 同族），HTTP 方法用 `POST`。

**是否产生同义词**：只要不把"组织及下级"写成 `subtree`、`cascade`、`includeChildren` 混用，就不产生。**`includeChildren` 已在 `RoleDataScopeVO` 中使用，本任务不要再造第二个布尔名**；直接复用 `includeChildren` 语义表达"及下级"，或直接用组织范围枚举。

**必须 `ASK`、不得自行决定的两项（阻塞点）**：

1. `authorization:role:assign` 是否等价于需求里的"租户管理员或授权管理员"？现有代码与菜单资源（`authorization-common-menu.json`）只有这一粒度，`AuthorizationCode` 里没有租户管理员/授权管理员的区分。若要新增 `authorization:role:assign-batch` 之类权限码，属于新增公共标识，须走确认。
2. "绑定后刷新授权缓存"指什么？现有链路里没有需要刷新的角色/权限缓存。若实际指"成员需重新登录才能看到新权限"，那属于前端说明文案，不是后端缓存刷新接口；若确指新增缓存，属于新增结构，须单独确认。

---

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界**（建议落点，按现有分层）：

| 层 | 文件 | 内容 |
|---|---|---|
| api | `mango-authorization-api/.../command/BatchSubjectRoleBindingCommand.java`、`vo/SubjectRoleMemberVO.java`、`vo/SubjectRoleBatchResultVO.java`、`RoleApi.java`（加方法声明） | 契约 |
| core | `IRoleService` + `RoleService` 增 `previewSubjectRoleBinding` / `bindSubjectRoleSubjectBatch` | 目标解析、成员集合解析过滤、幂等插入、统计 |
| starter | `RoleController` 加两个 `POST`，`@ApiAccess(PERMISSION, "authorization:role:assign")` | HTTP 适配、权限门禁、审计触发 |
| ui | `packages/rbac/src/api/role.ts`、`views/role/index.vue`（或 `views/user` 的批量动作） | 预览弹框 + 结果反馈 |
| 测试 | `mango-authorization-core/src/test/.../RoleServiceImplIntegrationTest.java` 同目录新增用例；`mango-authorization-starter/src/test/.../controller/AuthorizationApiContractTest.java` 补契约断言 | 见下 |

**责任边界（适用/不适用）**：

- 适用：当前租户内、`TENANT_MEMBER` 主体的授权角色批量绑定；目标范围限于"全部启用成员 / 指定用户 / 指定组织 / 组织及下级"。
- 不适用：跨租户绑定；`ROLE_LOGIN`、`ROLE_ANONYMOUS` 等系统默认角色（README 明确禁止逐个绑定）；组织关系本身的增删（属 Org/Identity）；成员移出（属 issue-919 生命周期）。

**当前消费者**（每个新增结构都要指出）：

- `BatchSubjectRoleBindingCommand`：`RoleController` 的两个接口 + `packages/rbac` 前端。
- 预览/结果 VO：同上；无其他消费者，因此不得放进 `support` 或做成通用分页结构。
- 新增的 service 方法：只被 `RoleController` 调用；不对外暴露 `IAuthorizationProvider` 之类运行时接口。

**验证入口**：

1. core 集成测试（H2 + 真实 Mapper，参照 `RoleServiceImplIntegrationTest` 的 `@SpringBootTest` + `AuthorizationTestSchema`）：`assignRolesReplacesSubjectRoleBindingsThroughRealMapper` 同族新增——
   - 重复执行两次 → `authorization_subject_role` 行数不变（幂等）；
   - 组织及下级 → 命中子组织成员，未命中平行组织；
   - 双租户数据 → 另一租户角色/成员被拒绝并计入失败明细；
   - 停用成员、`left_at` 非空成员被排除；
   - 解绑 → `deleteSubjectRoleBindings` 后行数归零（可逆）。
2. starter 契约测试：新增 `RoleApi` 方法有实现、`@ApiAccess` 权限码正确、路径不与方法冲突（现有 `AuthorizationApiContractTest` 已做同类断言）。
3. 前端 E2E：`mango-ui/apps/mango-admin/e2e/specs/` 新增（或扩展 `role-permission.spec.ts` / `user-management.spec.ts`）——预览数量与列表一致、确认后成员角色列回显、403 角色被拒。
4. 授权生效观察点：以被绑定成员身份调 `POST /authorization/roles/subjects/batch` 与 `/auth/info`，观察角色是否即时可见。**这是替代"刷新缓存"的可观察目标**——如果这里已经即时生效，说明无需缓存刷新，应把该项从需求里删掉而不是补一个空接口。
5. Maven 定向验证只跑直接改动的模块：`mango-authorization-api`、`mango-authorization-core`、`mango-authorization-starter`（遵循 dev agent 第 9 条，不做全 Reactor）。

---

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不需要生成固定问卷，也不需要生成术语表文件。**

理由：本任务不引入新公共概念。术语全部命中现有主术语（第 2 项表格），唯一的新增公共类型是 2-3 个 API DTO，且命名由既有 `SubjectRoleBinding` + `Batch` 组合确定。`REDLINE-QUEST-050` 只在"出现新增公共概念或认知风险"时要求按需记录术语决定。

**但需要一次 ASK（不是问卷）**：第 2 项列出的两个阻塞点——权限码粒度、"刷新授权缓存"的指代——属于需求缺口，按 `03-dev-agent.md` 第 3 条必须 STOP/ASK 回上游（`pm`/`requirement` 或 `tech-lead`/`design`），不得在开发阶段自行补设计。

另外，本任务的交付模式判定：跨模块（Authorization ↔ Identity ↔ Org）、涉及权限与租户边界、多步骤 → 按 `rules/11-delivery-assurance.md` 属 `L3`，默认映射为 **FULL**，需要先有获批的 `APPROVED` 文档链再实施。这一条也是我需要你确认的前提，而不是我自行降级为 STANDARD。

---

## 5. 过度设计（REDLINE-OVERDESIGN-060）

逐条对照：

| 判定项 | 本方案情况 |
|---|---|
| 新增抽象但本 PR 无消费者 | 无。所有新类型都有 controller + 前端消费者 |
| 新增配置项但无读取逻辑 | 无。不新增任何 `application.yml` 配置 |
| 新增接口/扩展点但只有一个实现 | 无。**不新增 SPI**。特别是不新增 `BatchRoleTargetResolver` 之类扩展点——四种范围在一个方法里用 switch 表达即可，扩展需求未出现在已批准需求中 |
| 新增依赖但标准库/已装依赖可实现 | 无。`mango-identity-api`、`mango-org-api` 已在 `mango-authorization-starter/pom.xml`；组织侧复用 `ObjectProvider<SysOrgApi>` 的既有反压模式（参照 `AuthRoleDataScopeResourceHandler` 对 `AuthorizationOrgReferenceProvider` 的用法），**不新增 `mango-org-starter-remote` 依赖** |
| 新增公共类型只在单一模块内部使用 | 需守住：`BatchSubjectRoleBindingCommand` 与两个 VO 必须放 `mango-authorization-api`（否则就是内部类型，应下沉 core） |
| 同一概念一个交付范围内多个名称 | 需守住：`includeChildren` 不要另造 `cascade`/`subtree`/`recursive` |
| 以"代码更整洁"为由新增抽象层 | 无。不新增 `BatchRoleBindingService` 独立服务类，方法并入 `RoleService` |

需要重点提防的三处过度设计：

1. 为"预览"和"执行"各建一套 command 与 service 方法——预览与执行共用同一份目标解析和成员过滤逻辑，只差"是否写库"，应共用私有方法而不是两份实现。
2. 为"失败明细"新建失败表——失败明细直接在返回结构里，持久化审计由既有操作日志承担。
3. 为"回滚"新增撤销单/批次表——解绑复用 `deleteSubjectRoleBindings`（按 subjectType + subjectIds + tenantId 删除），"可逆"由该接口提供，不需要批次概念。

---

## 下一步（等你确认后我才进入实施）

1. `ASK`：`authorization:role:assign` 是否可作为"租户管理员或授权管理员"的执行门禁？需要新权限码吗？
2. `ASK`："绑定后刷新授权缓存"具体指什么？如果 API 请求侧已实时读取角色，是否把该项改为"成员重新登录后生效"的前端提示？
3. 确认本任务为 FULL 模式、且已有获批的 BRD/SRS/TDD/Plan（或确认降级依据），否则我停在设计阶段。
4. 上述确认后，我再走 `pmo-preflight --role dev --phase develop` 并按 `REDLINE-MINIMAL-020` 给出可执行的实施清单。
