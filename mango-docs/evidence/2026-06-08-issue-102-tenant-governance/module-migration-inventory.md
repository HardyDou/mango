# Issue 102 模块 tenantId 迁移清单

## 范围

- 扫描模块：system、authorization、calendar、workflow、job。
- 扫描目标：显式读取 `MangoContextHolder.tenantId()`、手工 `setTenantId`、Wrapper 中手写 `tenantId` 条件、跨租户/初始化场景。
- 本清单是本次治理证据，不作为长期规范源；长期规则见 `mango-pmo/rules/backend/07-persistence.md`。

## 例外表基线

`mango.persistence.mybatis-plus.tenant.excluded-tables` 当前默认覆盖：

- 迁移历史：`flyway_schema_history*`、`databasechangelog`、`databasechangeloglock`
- 基础设施：`kv_record`、`infra_kv_entry`
- 平台全局元数据：`sys_tenant`、`sys_config`、`sys_dict_type`、`sys_dict_data`、`sys_area`
- 授权与前端元数据：`authorization_api_resource`、`authorization_permission`、`authorization_menu`、`authorization_app`、`authorization_app_login_context`、`authorization_app_module`、`frontend_app_registry`、`frontend_menu_runtime_config`、`frontend_module_runtime_strategy`
- 身份与租户成员关系：`identity_user`、`tenant_member`、`tenant_member_org`

`mango.persistence.schema-validation.excluded-tables` 是结构校验例外，不等同于租户过滤例外。

## 模块清单

| 模块 | 典型位置 | 分类 | 迁移建议 |
|---|---|---|---|
| system | `PersonalConfigService` 手工 `setTenantId` 和 `eq(SysPersonalConfigEntity::getTenantId, tenantId)` | 普通租户表重复过滤/填充 | 后续迁移到自动填充和自动过滤；保留用户维度条件。 |
| system | `OperationLogAspect`、`SysLogServiceImpl` 日志 tenant 写入/查询 | 日志记录与运营查询 | 日志实体如果纳入租户表，写入可改为上下文自动填充；运营跨租户查询需显式建模，不走普通 CRUD。 |
| system | `SysTenantServiceImpl` 创建租户后切换上下文 | 租户初始化 | 必要显式上下文场景，保留并补充租户初始化链路测试。 |
| authorization | `RoleServiceImpl`、`SubjectAuthorityServiceImpl`、`MenuServiceImpl` 的 query tenant 参数与角色/菜单绑定 | 授权边界/跨租户查询 | 授权查询可保留显式 tenant 作为授权模型入参；普通角色表查询中的重复 `eq tenantId` 后续拆分清理。 |
| authorization | `AuthorizationTenantProvisioner`、`TenantMenuPackageBindingHandler` 手工设置角色、角色菜单 tenant | 租户初始化/默认授权 | 必要显式建模场景；继续要求上下文或 provision 参数明确，不能依赖隐式默认租户。 |
| authorization | `authorization_menu`、`authorization_app*`、`frontend_*` | 全局/运行时元数据例外表 | 继续放在 `excluded-tables`；访问必须由授权服务控制范围。 |
| calendar | `CalendarAdminServiceImpl` 中 calendar/day 手工 tenant 条件和 `setTenantId` | 普通租户表重复过滤/填充 | 后续优先清理 `Calendar`/`CalendarDay` 普通 CRUD 的手工 tenant 条件；保留 code/year 等业务唯一性条件。 |
| calendar | `CalendarServiceImpl` 对 `CalendarDay` cache key 解析后 `setTenantId` | 缓存反序列化/领域值恢复 | 不是普通插入路径；保留为缓存值对象恢复，必要时改名区分持久化实体写入。 |
| workflow | category、definition、template service 创建时 `setTenantId(resolveTenantId())` | 普通租户表重复填充，含模板发布例外 | 普通创建改由自动填充；发布到目标租户、样例初始化属于跨租户/初始化场景，必须显式建模并恢复上下文。 |
| workflow | runtime/apply/process/task service 中任务、记录、表单实例 `setTenantId(currentTenantId())` | 运行时实体重复填充 | 后续按普通运行时表迁移到自动填充；跨租户审批或代办授权必须独立建模。 |
| workflow | `WorkflowCandidateGroupProvider`、`WorkflowAssigneeResolver` 查询 `tenant_member*` | 例外表授权查询 | `tenant_member`、`tenant_member_org` 是例外表，必须继续显式传入租户或上下文校验。 |
| job | definition/alarm/query service 普通 Wrapper 中 `eq tenantId` 和创建时 `setTenantId` | 普通租户表重复过滤/填充 | 面向 API 的普通 CRUD 后续清理；保留 VO 输出 tenantId 用于任务诊断。 |
| job | native runtime、engine sync、worker registry 使用 definition/command tenantId | 任务/异步上下文 | 必要显式上下文场景。Worker 注册、远程执行、调度线程必须在进入 Mapper 前恢复 `MangoContextHolder`。 |
| job | log chunk/index、instance/attempt/cursor 从 definition/instance 复制 tenantId | 异步派生记录 | 后续优先改成进入 runtime 时恢复上下文，由审计填充普通插入；跨租户调度仍需显式 scope。 |

## 后续拆分建议

1. 先治理 system/calendar 的普通 CRUD 重复 tenant 条件，范围小、风险低。
2. 再治理 workflow/job 的普通创建与查询；异步上下文恢复单独建测试。
3. authorization 保持最后处理，因为它包含授权模型、全局菜单元数据和租户初始化混合场景。
4. 每次迁移都必须补集成测试，证明 BaseMapper/Wrapper/XML/分页路径没有手工 tenant 条件仍保持租户隔离。
