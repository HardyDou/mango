---
documentId: TDD-ISSUE-918
documentType: technical-design
pmoVersion: 1.4.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，角色误授权与企微角色失效影响核心权限和租户安全；solution=L3，方案跨 Vue 交互、Notice、Identity、Authorization 和两模块数据迁移，并要求真实 MySQL 唯一键与幂等验证；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango RBAC、Notice、Identity 与 Authorization 技术负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-ISSUE-918
upstreamDocumentHash: f9be711bac78382e28a9a20f5627778ba1288b58eccf18ed91e23434f0a3e8d5
---

# Issue #918 RBAC 与企微主体一致性技术设计文档

## 1. 设计输入、约束与决策

| 决策ID  | 问题                                                 | 候选方案                                                                 | 选择                                                                          | 理由                                                                                                            | 来源ID或路径                                              | 是否推断 | 影响                                 | 风险                                                                           | 回退条件                                                            |
| ------- | ---------------------------------------------------- | ------------------------------------------------------------------------ | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------- | -------- | ------------------------------------ | ------------------------------------------------------------------------------ | ------------------------------------------------------------------- |
| DEC-001 | 如何避免父目录回显触发兄弟节点级联，同时保留半选祖先 | `check-strictly` 独立勾选；只设置叶子并保存全选+半选；修改后端只返回叶子 | 只设置已授权叶子，保存 `getCheckedKeys(false)` 与 `getHalfCheckedKeys()` 并集 | 不改变 API；复用 Element Plus 原生级联与半选；必要祖先可保存，兄弟节点不会因初始化被选中                        | FR-001、`mango-ui/packages/rbac/src/views/role/index.vue` | 否       | RBAC 页面与测试                      | 父目录无任何叶子时不应作为有效授权；当前菜单授权语义要求父目录服务于可访问叶子 | 若现网存在明确的目录单独授权产品需求，停止并另行设计独立直接授权 UI |
| DEC-002 | `INTERNAL_ORG.partyId` 应表示租户还是主部门          | Auth 使用租户；改 Auth 使用主部门；统一租户并保留部门关系                | 统一为当前数值 `tenantId`                                                     | Auth 无显式主体时已回退 tenantId；#909 用户管理与手工账号也按租户主体；部门已有 `primary_org_id` 与成员组织关系 | FR-002、Auth/Identity/Authorization README 与 main 代码   | 否       | Notice 同步、Identity 用户、角色绑定 | 非数值租户不能映射 Long partyId                                                | 同步对非数值租户失败关闭，不写错误主体                              |
| DEC-003 | unchanged 和禁用资料更新时是否修复主体               | 只在资料更新修复；始终独立修复同步管理用户主体                           | 始终执行主体修复                                                              | 主体是授权不变量，不属于昵称等可选资料；否则默认 `skipUnchanged=true` 会永久保留错误数据                        | FR-002、SAC-002                                           | 否       | Notice 同步统计与 Identity update    | 额外一次 detail/update 调用                                                    | 已一致时不调用 update，保持正常重复同步低写入                       |
| DEC-004 | 如何修复历史错误数据                                 | 运维手工 SQL；跨模块脚本；各 owner 模块 Flyway migration                 | Identity V6 与 Authorization V2 分别修复自有表                                | 满足表所有权与标准升级入口；已人工归一化环境幂等 no-op                                                          | FR-003、DR-002                                            | 否       | 两模块 migration 与升级验证          | Authorization 多个部门绑定归一化会触发唯一键冲突                               | 先按目标唯一键删除较大 ID 重复行，再更新保留行                      |
| DEC-005 | 如何以最小改动提供默认单租户产品态                   | 删除全部租户基础设施；新增后端模式开关；使用现有默认租户与 Resource 驱动 | 保留内部租户边界，冷库只声明租户 1；唯一选项自动选择；停用租户管理菜单          | 现有 `default-tenant-id=1` 和登录选择逻辑可复用；不改 185 张表、API 和认证上下文                               | FR-004、DR-004、SAC-004                                  | 否       | Auth 页面、System 菜单和 demo Resource | 删除 `INIT_ONLY` 声明不会清理已有租户，不能把本地冷库结果冒充存量升级效果          | 若要求物理禁止创建第二租户，回到设计阶段增加独立后端模式合同         |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包                   | 职责                                 | 改动类型                       | 依赖方向                                   | 公开能力                | 系统需求ID | 适用规范ruleId     | 验证方式                                     |
| ---------- | -------------------------- | ------------------------------------ | ------------------------------ | ------------------------------------------ | ----------------------- | ---------- | ------------------ | -------------------------------------------- |
| MOD-001    | `@mango/rbac`              | 角色菜单树状态转换与页面交互         | 内部逻辑修复、测试             | 页面依赖现有 role API 与 Element Plus      | API 与页面 key 不变     | FR-001     | M09、M10、M13      | Vitest、typecheck/build、Playwright          |
| MOD-002    | `mango-notice-core`        | 企微同步始终写租户主体并保留组织关系 | 服务逻辑修复、集成测试         | Notice 通过现有 Identity/Org gateway       | 同步 API 不变，行为纠正 | FR-002     | M09、M10、M11、M12 | 定向 Maven verify、Notice 集成测试、API 流程 |
| MOD-003    | `mango-identity-core`      | 归一化历史用户主体                   | 新增 V6 数据 migration、README | 只写 `identity_user` owner 表              | 升级行为变化            | FR-003     | M09、M11、M08      | migration 静态检查、真实 MySQL、README 审计  |
| MOD-004    | `mango-authorization-core` | 去重并归一化历史角色绑定主体         | 新增 V2 数据 migration、README | 只写 `authorization_subject_role` owner 表 | 升级行为变化            | FR-003     | M09、M11、M08      | migration 静态检查、真实 MySQL、README 审计  |
| MOD-005    | `@mango/auth` 与 System/Identity/Authorization/Org starter | 默认单租户登录、菜单和 demo 基线 | 条件渲染、Resource 调整、契约测试 | 前端继续调用现有登录接口；各 starter 只维护本模块声明 | 默认产品行为变化，公开 API 不变 | FR-004 | M08、M09、M10、M11、M12、M13 | 包构建、Resource 测试、冷库 API 与 Playwright |

## 3. 技术对象与状态模型

| 模型ID | 上游ID         | 模型职责                   | 标识            | 关系                                  | 状态编码                        | 审计或历史                | 归属或租户         | 一致性约束                                                          |
| ------ | -------------- | -------------------------- | --------------- | ------------------------------------- | ------------------------------- | ------------------------- | ------------------ | ------------------------------------------------------------------- |
| DM-001 | DR-001、FR-001 | 菜单树授权状态             | `menuId` 字符串 | parent/children 树                    | checked、halfChecked、unchecked | 保存由后端记录            | 当前角色与 appCode | 初始 checked 只含授权叶子；提交为 checked+halfChecked 去重          |
| DM-002 | DR-002、FR-002 | Identity 用户授权主体      | userId          | 用户对应租户成员与外部身份            | consistent/mismatched           | migration 与 `updated_at` | `tenant_id`        | `party_type=INTERNAL_ORG` 时 `party_id=CAST(tenant_id AS UNSIGNED)` |
| DM-003 | DR-002、FR-003 | Authorization 主体角色绑定 | binding id      | subject + role + tenant + app + party | canonical/duplicate/mismatched  | migration 保留最小 id     | `tenant_id`        | INTERNAL_ORG 目标唯一键中 partyId 等于 tenantId且只保留一行         |
| DM-004 | DR-004、FR-004 | 默认租户产品基线           | tenantId=1      | tenant、member、role、org、menu       | single-default                | Resource 版本与同步历史    | tenant 1           | 冷库只有芒果集团；demo 组织为 2 公司 4 部门；租户菜单停用           |

| 模型ID | 当前状态             | 触发      | 目标状态                 | 前置条件           | 副作用                        | 失败处理                                      | 上游ID           |
| ------ | -------------------- | --------- | ------------------------ | ------------------ | ----------------------------- | --------------------------------------------- | ---------------- |
| DM-001 | direct IDs loaded    | 树初始化  | checked/halfChecked 精确 | 菜单树已加载       | Vue tree store 更新           | 加载失败不开放保存                            | SAC-001          |
| DM-002 | mismatched           | 同步或 V6 | consistent               | 数值 tenantId      | Identity update 或 SQL update | 非数值同步失败；migration 只处理数值 tenantId | SAC-002、SAC-003 |
| DM-003 | duplicate/mismatched | V2        | canonical                | 表结构与唯一键存在 | 删除较大重复 id，再 update    | 任一 SQL 失败回滚该 migration                 | SAC-003          |
| DM-004 | resources discovered | 冷启动同步 | single-default           | 正式和 demo 声明可解析 | 写入租户 1 基线与 7 个组织节点 | 同步失败阻断启动；不删除既有租户              | SAC-004          |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口                        | 参与模块         | 处理顺序                                                                                      | 事务边界                                         | 状态变化                    | 幂等键                         | 并发策略                 | 外部失败与补偿                         | 用户可见结果           |
| ---------- | ---------- | ------------------------------- | ---------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------ | --------------------------- | ------------------------------ | ------------------------ | -------------------------------------- | ---------------------- |
| FLOW-001   | FR-001     | 角色页面分配权限弹窗            | MOD-001          | 并行加载菜单和直接 ID；提取树中授权叶子；setCheckedKeys；保存 checked 与 halfChecked 并集     | 单次后端 assignMenus 事务不变                    | DM-001 精确变化             | roleId + menuId                | 提交期间按钮 loading     | 请求失败不关闭弹窗、不提交空回退       | 半选祖先、精确保存     |
| FLOW-002   | FR-002     | `POST /notice/wecom/users/sync` | MOD-002、MOD-003 | 解析 tenant party；解析用户；创建或按需更新资料；无条件检查主体；维护组织关系、映射与外部身份 | Notice 单用户现有边界；Identity/Org 各自远程事务 | DM-002 -> consistent        | tenant + corpId + wecom userId | 复用同步映射与现有唯一键 | 任一步失败计入 failed，不伪成功        | 同步统计与后续授权有效 |
| FLOW-003   | FR-003     | Flyway module migration         | MOD-003、MOD-004 | Identity V6 更新数值租户错误主体；Authorization V2 先 delete duplicate 后 update mismatched   | 每模块 Flyway migration 事务能力                 | DM-002、DM-003 -> canonical | 表唯一键                       | 数据库 migration lock    | SQL 失败阻断升级；恢复备份后重试同版本 | 升级完成且错误数据清零 |
| FLOW-004   | FR-004     | 冷启动、登录与用户菜单加载      | MOD-005          | Resource 初始化租户 1 与精简组织；登录接口返回唯一租户；前端自动选中并隐藏选择器；菜单同步停用租户管理 | 沿用各 Resource handler 和登录请求事务边界       | DM-004 -> single-default    | Resource stable id 与 bizKey   | 沿用 Resource 同步锁      | 初始化失败阻断；登录查询失败提示且不提交 | 无需选择租户进入后台   |

## 5. API 与远程契约设计

| 接口ID  | 系统需求ID | 调用方            | 所属模块 | 入口类型                 | 方法与路径                      | Command Query或VO                                | 返回契约                 | 校验                              | 权限租户或数据权限           | 幂等分页或排序        | 错误码                       | 兼容策略           | 适用规范ruleId | 验证方式                        |
| ------- | ---------- | ----------------- | -------- | ------------------------ | ------------------------------- | ------------------------------------------------ | ------------------------ | --------------------------------- | ---------------------------- | --------------------- | ---------------------------- | ------------------ | -------------- | ------------------------------- |
| API-001 | FR-001     | RBAC 页面         | MOD-001  | 现有 HTTP                | POST /authorization/roles/menus | `RoleMenuAssignCommand`                          | R<Boolean>               | roleId 与 menuIds 沿用现有校验    | 当前登录与授权校验不变       | 前端提交去重集合      | 现有错误契约                 | 无协议变化         | M12            | 前端 request 断言、现有集成测试 |
| API-002 | FR-002     | RBAC 用户同步入口 | MOD-002  | 现有 HTTP                | POST /notice/wecom/users/sync   | `SyncWecomUsersCommand`、`WecomUserSyncResultVO` | R<WecomUserSyncResultVO> | 当前 tenantId 必须为正数          | `system:user:add` 与当前租户 | 映射与关系幂等        | `NOTICE_BUSINESS_ERROR`      | 请求和响应字段不变 | M12            | Notice 集成与本地 API 流程      |
| API-003 | FR-002     | Notice            | MOD-003  | 现有 HTTP/remote adapter | PUT /identity/users             | `UpdateIdentityUserCommand`                      | R<Boolean>               | update 仅在主体不一致或资料需变更 | 当前租户上下文透传           | 已一致时零额外 update | 现有 Identity 与 Notice 错误 | 无公开签名变化     | M11            | gateway 替身断言与集成测试      |
| API-004 | FR-004     | Auth 登录页        | MOD-005  | 现有 HTTP                | GET /system/tenant/login-options | `List<LoginTenantOptionVO>` | R<List> | 沿用启用状态校验 | 冷库返回租户 1 | 查询只读 | 现有登录租户错误 | 无协议变化 | M12 | 冷库 API 与浏览器响应断言 |
| API-005 | FR-004     | Auth 登录页        | MOD-005  | 现有 HTTP                | POST /auth/login-institutions | `AccountLoginInstitutionCommand`、`List<LoginTenantOptionVO>` | R<List> | 沿用账号成员校验 | 返回账号在租户 1 的成员上下文 | 查询只读 | 现有账号登录机构错误 | 无协议变化 | M12 | 冷库 API 与浏览器响应断言 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID   | 表或实体                     | 字段变化                      | 约束                                                                           | 索引           | 租户审计                                   | Mapper边界                    | 数据来源   | migration或回填                                      | 回滚或补偿                                     | 适用规范ruleId | 验证方式                                        |
| ---------- | -------------- | ---------------------------- | ----------------------------- | ------------------------------------------------------------------------------ | -------------- | ------------------------------------------ | ----------------------------- | ---------- | ---------------------------------------------------- | ---------------------------------------------- | -------------- | ----------------------------------------------- |
| DB-001     | DM-002、DR-002 | `identity_user`              | 无 DDL；修复 `party_id`       | 仅 `party_type='INTERNAL_ORG'` 且 `tenant_id` 为正整数字符串                   | 不变           | 使用同记录 tenant_id；update time 由表维护 | Identity owner migration      | 当前表数据 | V6 将 party_id 更新为无符号 tenant_id                | 执行前数据库备份；失败由 Flyway 回滚或恢复备份 | M11            | MySQL 错误/正确/重复执行数据断言                |
| DB-002     | DM-003、DR-002 | `authorization_subject_role` | 无 DDL；去重并修复 `party_id` | 目标唯一键按 subjectType、subjectId、roleId、tenantId、appCode、partyType 唯一 | 现有唯一键不变 | tenant_id bigint 直接作为 party_id         | Authorization owner migration | 当前表数据 | V2 自连接删除同目标键较大 id，再 update INTERNAL_ORG | 执行前数据库备份；失败由 Flyway 回滚或恢复备份 | M11            | MySQL canonical、两个错误部门、已有正确绑定场景 |
| DB-003     | DM-004、DR-004 | `sys_tenant`、`sys_org` 等现有表 | 无 DDL；减少 demo 声明 | 现有主键、租户唯一键和组织编码唯一键不变 | 不变 | 所有业务表仍使用 tenant_id 拦截 | 各 Resource handler 所属模块 | 正式与 demo Resource | 无 migration；任务专属演示库冷重建 | 不自动清理存量；本地失败时重建任务库 | M02、M11 | 冷库租户和组织计数、声明契约测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001 | 角色菜单最小授权 | 现有角色菜单管理权限 | 不变 | Authorization 分配接口 | 当前角色 app/tenant 不变 | 只提交页面展示树中的 ID | 半选祖先，失败不关闭 | 现有角色授权审计 | M10、M12、M13 | 组件、API 与浏览器断言 |
| SEC-002 | FR-002、FR-003 | 租户主体一致 | `system:user:add` 与现有登录权限 | 不变 | Notice 同步、Auth 登录、Authorization exact match | partyId 从当前 tenantId 唯一派生 | 部门 ID 只进入成员组织关系 | 同步错误明确 | migration 与同步日志 | M11、M12 | 双租户/重复同步/菜单与权限断言 |
| SEC-003 | FR-004 | 默认单租户产品态 | 现有登录和菜单权限 | `system:tenant` 不加入默认套餐 | Auth 登录与用户菜单加载 | tenantId 始终为 1，拦截器保持启用 | 公司和部门仍归属租户 1 | 不显示租户选择和管理入口 | 登录与 Resource 同步日志 | M09、M11、M12、M13 | 静态配置、冷库 API 和浏览器断言 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景                   | 触发条件                   | 错误码                | 异常类型          | 用户反馈           | 日志上下文                                     | 指标或告警  | 重试或补偿               | 敏感信息处理       |
| ---------- | ---------- | -------------------------- | -------------------------- | --------------------- | ----------------- | ------------------ | ---------------------------------------------- | ----------- | ------------------------ | ------------------ |
| ERR-001    | FR-001     | 菜单或角色 ID 请求失败     | 任一加载/保存接口异常      | 现有前端 API 错误     | Promise rejection | 现有消息并保持弹窗 | console 记录动作，不记录 token                 | NONE        | 修复接口后重试           | 不记录凭据         |
| ERR-002    | FR-002     | 租户 ID 非正数             | 企微同步进入主体解析       | NOTICE_BUSINESS_ERROR | Mango 业务异常    | 同步失败信息       | tenantId 与 wecom userId 上下文，不记录 secret | failedCount | 修正租户上下文后重试     | Corp secret 不记录 |
| ERR-003    | FR-003     | migration SQL 或唯一键失败 | 数据不满足预期或数据库异常 | Flyway failure        | 数据库异常        | 启动/升级失败      | migration 版本与数据库错误                     | 部署告警    | 恢复备份、修正数据后重试 | 不输出用户敏感字段 |
| ERR-004    | FR-004     | 默认租户查询或 Resource 初始化失败 | 冷库启动或登录加载租户 | 现有 Resource/Login 错误 | 现有异常类型 | 启动失败或登录错误提示 | 模块、Resource 与请求上下文，不记录凭据 | 启动与登录错误 | 修复声明或配置后重试 | 不记录密码和 token |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID             | 页面或动作   | 页面key或路由       | 区域与组件                    | 状态来源                                        | API依赖 | 权限或不可操作                        | 空加载或失败态               | 语义测试锚点                                                      | 复用判断                                     | 适用规范ruleId |
| ---------- | ---------------------- | ------------ | ------------------- | ----------------------------- | ----------------------------------------------- | ------- | ------------------------------------- | ---------------------------- | ----------------------------------------------------------------- | -------------------------------------------- | -------------- |
| UI-001     | PG-001、BT-001、FR-001 | 分配角色权限 | `system/role/index` | 现有 `el-dialog` 与 `el-tree` | menu tree、direct IDs、tree checked/halfChecked | API-001 | loading 与 submitLoading 禁止重复提交 | 沿用页面状态并确保失败不提交 | `data-surface=role.menu-assignment`、`data-action=role.menu.save` | 树状态函数留在 role 页面领域并导出纯函数测试 | M09、M10、M13  |
| UI-002     | PG-002、BT-002、FR-004 | 默认租户登录 | `/login` | 现有登录表单与租户选择器 | API-004、API-005 返回的 tenantOptions | API-004、API-005 | tenantOptions 数量大于 1 才渲染选择器 | 查询失败沿用错误反馈并阻止登录 | `.tenant-select`、`.login-btn` | 复用 `useMangoLoginFlow.applyTenantOptions`，不新增第二套状态 | M09、M12、M13 |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID       | 设计项ID                                     | 场景                                                   | 优先级 | 测试层级       | 自动化判断 | 测试数据                               | 权限或租户边界           | 稳定契约                                      | 执行入口                              | 证据                      | 失败处理                           | 适用规范ruleId |
| ---------- | ---------------- | -------------------------------------------- | ------------------------------------------------------ | ------ | -------------- | ---------- | -------------------------------------- | ------------------------ | --------------------------------------------- | ------------------------------------- | ------------------------- | ---------------------------------- | -------------- |
| TC-001     | SAC-001          | DEC-001、DM-001、FLOW-001、UI-001            | 父目录、部分叶子、未授权兄弟回显；无改动与增删保存     | P0     | 单元/组件      | AUTO       | 三层菜单树与字符串 ID                  | 当前角色                 | 叶子 hydration 与 checked+halfChecked 并集    | RBAC Vitest                           | 测试报告                  | 任一集合差异阻断                   | M10            |
| TC-002     | SAC-002          | DEC-002、DEC-003、FLOW-002、API-002、API-003 | 新建、更新、update disabled、unchanged 重复同步        | P0     | 集成           | AUTO       | tenant=1、department=10 的测试目录用户 | INTERNAL_ORG partyId=1   | Identity gateway create/update 与组织关系调用 | NoticeServiceIntegrationTest 定向执行 | Surefire                  | 任一错误主体或重复阻断             | M11、M12       |
| TC-003     | SAC-003          | DEC-004、DB-001、DB-002                      | 错误主体、正确绑定与多个错误部门并存；执行两次         | P0     | 真实数据库集成 | AUTO       | 隔离 MySQL schema                      | 单租户 owner 表          | migration SQL + 唯一键 + 二次 no-op           | 受控 MySQL 验证脚本/命令              | `.runtime/issue-918` 摘要 | SQL/计数差异阻断                   | M11            |
| TC-004     | SAC-001、SAC-002 | UI-001、SEC-001、SEC-002                     | 浏览器打开授权弹窗、保存请求体；同步用户登录菜单与 API | P0     | UI/E2E         | AUTO       | 隔离库测试角色、菜单、企微目录替身     | 租户 1，非管理员业务角色 | data anchors、request body、菜单和受保护 API  | mango-admin Playwright 定向用例       | evidence 与 trace         | console/network 或业务断言失败阻断 | M13            |
| TC-005     | SAC-004          | DEC-005、MOD-005、DM-004、FLOW-004、API-004、API-005、DB-003、SEC-003、ERR-004、UI-002 | 冷库初始化、唯一租户登录、菜单和 7 节点组织树 | P0 | 契约/集成/API/UI | AUTO | 任务专属空库与默认 admin | tenantId=1，拦截器启用 | Resource stable id、登录选项和菜单 code | Maven 契约测试、API、Playwright | test-results 与截图 | 任一数量、菜单或登录断言失败阻断 | M09、M10、M11、M12、M13 |

## 11. 兼容与已启用能力说明影响

| 影响ID  | 设计项ID                         | 影响对象               | 当前行为                 | 目标行为               | 兼容策略                          | 升级或补偿                   | 已启用能力说明                                          | 验证                        | 责任人   |
| ------- | -------------------------------- | ---------------------- | ------------------------ | ---------------------- | --------------------------------- | ---------------------------- | ------------------------------------------------------- | --------------------------- | -------- |
| IMP-001 | DEC-001、UI-001                  | `@mango/rbac` 消费项目 | 菜单父级可能扩大回显     | 精确回显并保存必要祖先 | API、页面 key 与操作入口不变      | 升级 npm 包                  | RBAC README 与能力地图                                  | M08 审计、包测试与浏览器    | Frontend |
| IMP-002 | DEC-002、DEC-003、DB-001、DB-002 | Mango Maven 消费项目   | 企微部门 ID 可能污染主体 | 同步与升级统一租户主体 | 公共 API 不变，历史数据自动归一化 | 升级 Maven 物料并执行 Flyway | Notice、Identity、Authorization、Auth README 与能力地图 | M08 审计、Maven/DB/API 验证 | Backend  |
| IMP-003 | DEC-005、UI-002、DB-003          | Mango 默认部署与 demo  | 多租户入口和第二租户默认可见 | 默认单租户产品态 | 内部租户表、上下文和 API 保留 | 冷库自动生效；存量演示库需受控重建 | System、Identity、Authorization、Org README | M08、Maven、API 与浏览器 | Frontend、Backend |

## 12. 技术追踪矩阵

| 上游ID                                                                   | 设计项ID                                                                        | 测试用例ID                     | 覆盖说明                                        |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------------- | ------------------------------ | ----------------------------------------------- |
| SC-001、SA-001、FR-001、UC-001、PG-001、BT-001、DR-001、NFR-001、SAC-001 | DEC-001、MOD-001、DM-001、FLOW-001、API-001、SEC-001、ERR-001、UI-001           | TC-001、TC-004                 | 覆盖角色菜单回显、保存、错误与 UI               |
| SC-002、SA-002、FR-002、UC-002、DR-002、DR-003、IR-001、NFR-002、SAC-002 | DEC-002、DEC-003、MOD-002、DM-002、FLOW-002、API-002、API-003、SEC-002、ERR-002 | TC-002、TC-004                 | 覆盖企微同步、主体、组织、登录授权与幂等        |
| FR-003、SAC-003                                                          | DEC-004、MOD-003、MOD-004、DM-003、FLOW-003、DB-001、DB-002、ERR-003            | TC-003                         | 覆盖 owner migration、去重、归一化和重复执行    |
| FR-001、FR-002、FR-003                                                   | IMP-001、IMP-002                                                                | TC-001、TC-002、TC-003、TC-004 | 覆盖 npm/Maven 消费兼容、升级说明和能力文档影响 |
| SC-003、SA-003、FR-004、UC-003、PG-002、BT-002、DR-004、NFR-003、SAC-004 | DEC-005、MOD-005、DM-004、FLOW-004、API-004、API-005、DB-003、SEC-003、ERR-004、UI-002 | TC-005 | 覆盖默认单租户产品态、内部租户兼容与冷库验收 |
| FR-004、SAC-004                                                          | IMP-003                                                                         | TC-005 | 覆盖消费者说明、冷库生效和存量数据边界 |

## 13. 阶段判定与审批

| 检查项           | 结果     | 证据                                                                                                                                                                                                                                                                                                       |
| ---------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 技术设计 checker | PASS     | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issue-918-rbac-wecom-party/technical-design.md`                                                                                                                                                                             |
| 生命周期 handoff | PASS     | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd mango-docs/designs/issue-918-rbac-wecom-party/business-requirements.md --srs mango-docs/designs/issue-918-rbac-wecom-party/system-requirements.md --tdd mango-docs/designs/issue-918-rbac-wecom-party/technical-design.md --risk L3 --through tdd` |
| 专项规范检查计划 | PASS     | M09 静态、M10 单元、M11 集成、M12 API、M13 UI、M15 GitHub Issue 回读；不启用与目标无关的 M14/M16                                                                                                                                                                                                           |
| 未关闭阻断数量   | 0        | NONE                                                                                                                                                                                                                                                                                                       |
| Tech Lead 审批   | APPROVED | `review/APPROVAL.md`                                                                                                                                                                                                                                                                                       |
