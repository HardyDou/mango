# Mango Maven 1.0.21 到 1.0.22 Java API 升级

## 1. 适用范围

本文用于业务后端从 `io.mango:*:1.0.21` 升级到 `1.0.22`。这次发布保持主要 HTTP 路径和业务语义不变，但按 Mango API 分层规范调整了多项 Java 源码契约，因此旧业务源码可能在编译阶段报 `cannot find symbol`。

最常见的报错是：

```text
cannot find symbol: class IdentityUserInfo
```

它的直接替代类型是 `io.mango.identity.api.vo.IdentityUserInfoVO`。字段和 `IdentityUserApi` 的三个查询方法语义不变，只需要同步变量、泛型、方法返回值和 import。

## 2. 升级结论

- 业务代码应迁移到 1.0.22 的 Command、Query、VO、Api 和 support 扩展点，不恢复 1.0.21 旧类型。
- 不要在同一个 Maven reactor 中混用 Mango `1.0.21` 和 `1.0.22`，也不要给已删除类型自行复制同包名兼容类。
- 先只升级后端 Maven 版本并完成编译迁移，再升级 PMO、前端包和 CLI。
- Java 类型迁移本身不要求业务 SQL。已有数据库仍按 1.0.22 发布说明执行 Flyway，并检查文件模块的 `V2__default_file_access_mode_to_proxy.sql`。

## 3. 升级顺序

### 3.1 建立升级分支

记录当前可工作的 1.0.21 提交和数据库备份，然后在独立分支升级。先确认业务仓没有直接锁定不同版本的 Mango 子模块：

```bash
rg -n '<mango.version>|<version>1\.0\.21</version>|io\.mango' backend/pom.xml backend/**/pom.xml
```

将统一的 Mango Maven 版本改为 `1.0.22`，此时暂不升级前端和 CLI。

### 3.2 扫描旧符号

```bash
rg -n \
  'IdentityUserInfo\b|AuthUserInfo\b|TenantMemberInfo\b|TenantMemberOrgRelationInfo\b|CmsAdminApi\b|ResourceRegistryApi\b|io\.mango\.resource\.api\.(ResourceProvider|ResourceHandler)\b|io\.mango\.job\.api\.handler|TokenContextHolder\b|InternalApi\b|LoginApi\b|PublicApi\b' \
  backend --glob '*.java'
```

先完成下面的直接替换，再处理契约拆分项。每完成一个业务模块就执行该模块编译，最后执行完整 reactor 验证。

### 3.3 先处理 Identity

1. 替换 import：

```java
// 1.0.21
import io.mango.identity.api.vo.IdentityUserInfo;

// 1.0.22
import io.mango.identity.api.vo.IdentityUserInfoVO;
```

2. 同步变量和泛型。`IdentityUserApi` 的方法名不变：

```java
R<IdentityUserInfoVO> byName = identityUserApi.getUserInfo(username);
R<IdentityUserInfoVO> byId = identityUserApi.getUserInfoById(userId);
R<List<IdentityUserInfoVO>> recipients =
        identityUserApi.listUserInfosByTarget(query);
```

3. 如果业务实现了 `IdentityUserApi`、Feign 适配器或测试桩，三个覆盖方法的返回泛型也要一起改为 `IdentityUserInfoVO`。1.0.22 在 API 接口上补齐了 `@Valid`、`@NotNull` 和 `@NotBlank`；实现类保持这些参数约束一致。

`IdentityUserInfo` 到 `IdentityUserInfoVO` 的字段一一对应：`userId`、`username`、`nickname`、`realm`、`actorType`、`partyType`、`partyId`、`email`、`phone`、`avatar`、`status`。不需要字段转换器或数据库迁移。

## 4. 直接类型和包迁移

以下迁移可以按表直接修改 import、变量类型和泛型。业务代码仍要根据编译器提示检查构造器和新增校验约束。

| 1.0.21 类型 | 1.0.22 类型 | 迁移说明 |
|---|---|---|
| `io.mango.identity.api.vo.IdentityUserInfo` | `io.mango.identity.api.vo.IdentityUserInfoVO` | 字段和查询语义不变 |
| `io.mango.identity.api.vo.AuthUserInfo` | `io.mango.identity.api.vo.AuthUserVO` | 认证事实 VO 改名 |
| `io.mango.identity.api.vo.TenantMemberInfo` | `io.mango.identity.api.vo.TenantMemberVO` | 成员事实 VO 改名 |
| `io.mango.identity.api.vo.TenantMemberOrgRelationInfo` | `io.mango.identity.api.vo.TenantMemberOrgRelationVO` | 组织关系 VO 改名，按新字段重新编译 |
| `io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult` | `io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse` | 行为验证码校验返回对象改名 |
| `io.mango.access.api.auth.AccessPrincipal` | `io.mango.access.api.vo.AccessPrincipalVO` | Access 返回模型归入 `vo` |
| `io.mango.access.api.auth.AccessResult` | `io.mango.access.api.vo.AccessResultVO` | Access 返回模型归入 `vo` |
| `io.mango.access.api.auth.AccessContextValidationResult` | `io.mango.access.api.vo.AccessContextValidationResultVO` | Access 校验结果归入 `vo` |
| `io.mango.authorization.api.AuthorizationSnapshot` | `io.mango.authorization.api.vo.AuthorizationSnapshotVO` | 授权快照 VO 化 |
| `io.mango.authorization.api.SecurityContext` | `io.mango.authorization.api.vo.SecurityContextVO` | 通过 `ISecurityContextProvider.currentContext()` 获取 |
| `io.mango.authorization.api.SecurityPrincipal` | `io.mango.authorization.api.vo.SecurityPrincipalVO` | Spring Security principal 类型替换 |
| `io.mango.org.api.entity.SysOrg` | `io.mango.org.api.vo.SysOrgVO` | API 不再返回 Entity |
| `io.mango.area.api.entity.SysArea` | `io.mango.area.api.vo.SysAreaVO` | 区域 API 不再返回 Entity；树节点使用 `SysAreaTreeNodeVO` |
| `io.mango.home.api.vo.HomeTemplateAuthorizationItem` | `io.mango.home.api.command.HomeTemplateAuthorizationCommand` | 该对象是写入参数，不再作为 VO |
| `io.mango.template.api.command.TemplateVariableDefinition` | `io.mango.template.api.command.TemplateVariableCommand` | 模板变量写入模型改名 |
| `io.mango.auth.api.AuthCode` | `io.mango.auth.api.enums.AuthCode` | 枚举移动到 `enums` |
| `io.mango.captcha.api.CaptchaCode` | `io.mango.captcha.api.enums.CaptchaCode` | 枚举移动到 `enums` |
| `io.mango.file.preview.api.FilePreviewCode` | `io.mango.file.preview.api.enums.FilePreviewCode` | 枚举移动到 `enums` |
| `io.mango.system.api.SystemCode` | `io.mango.system.api.enums.SystemCode` | 枚举移动到 `enums` |
| `io.mango.template.api.TemplateCode` | `io.mango.template.api.enums.TemplateCode` | 枚举移动到 `enums` |
| `InternalApi` / `LoginApi` / `PublicApi` | `InternalAccess` / `LoginAccess` / `PublicAccess` | 包仍为 `io.mango.authorization.api.annotation` |

## 5. 需要调整调用方式的契约

### 5.1 Authorization 和 token 上下文

- `DeleteRoleDataScopeCommand` 已删除，调用 `DataScopeApi.deleteRoleScope(roleId, resourceCode)`。
- `TokenContextHolder` 已从 authorization API 删除。普通入口请求到 Feign 的 token 由 `mango-infra-feign-starter` 自动捕获和透传，业务代码删除手工 `set/clear`。
- 只有自定义线程边界确实需要显式传播时，依赖 `mango-infra-context-api` 并使用 `MangoContextHolder` 的 token 能力，同时保证 `finally` 清理；不要重新实现 ThreadLocal。
- `AuthorizationResourceTypes` 已删除。资源声明使用 YAML/JSON 中的 `AUTH_ROLE`、`AUTH_MENU` 等资源类型；实现 Java Resource 扩展点时改用 `io.mango.resource.support.ResourceTypes`。

### 5.2 Calendar 查询对象

| 1.0.21 | 1.0.22 | 字段差异 |
|---|---|---|
| `AddWorkdaysCommand` | `AddWorkdaysQuery` | `includeSource` 从 `boolean` 改为非空 `Boolean` |
| `BatchCheckWorkdayCommand` / `BatchCheckWorkdayQuery` | `BatchCheckWorkdayRequest` | 日期最多 366 个且元素不能为 null |
| `BetweenWorkdaysCommand` | `CountWorkdaysQuery` | `includeBoundary` 拆为 `includeStart` 和 `includeEnd` |
| `CalendarDateCommand` | `CalendarDateQuery` | 字段语义不变 |

### 5.3 CMS 管理 API

`CmsAdminApi` 已删除并按职责拆成 11 个管理 API。原业务按调用的方法注入对应 API：

| 管理能力 | 1.0.22 API |
|---|---|
| 内容分类 | `CmsContentCategoryApi` |
| 内容标签 | `CmsContentTagApi` |
| 站点 | `CmsSiteAdminApi` |
| 站点栏目 | `CmsSiteCategoryApi` |
| 内容 | `CmsContentApi` |
| 内容发布 | `CmsContentPublishApi` |
| 导航 | `CmsNavigationApi` |
| Banner | `CmsBannerApi` |
| 广告 | `CmsAdvertisementApi` |
| 广告投放 | `CmsAdDeliveryApi` |
| 站点配置 | `CmsSiteSettingApi` |

只远程调用时继续依赖 `mango-cms-starter-remote`。该 starter 会自动注册上述 FeignClient，不需要业务应用手工 `@EnableFeignClients`。公开站点读取继续使用 `CmsSiteApi`。

### 5.4 Job 本地处理器

Job handler 是 JVM 内扩展点，已从 API 包迁移到 support 包：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-support</artifactId>
</dependency>
```

```java
import io.mango.job.support.handler.MangoJobHandleContext;
import io.mango.job.support.handler.MangoJobHandleResult;
import io.mango.job.support.handler.MangoJobHandler;
```

`SaveMangoJobDefinitionCommand` 拆为 `CreateMangoJobDefinitionCommand` 和 `UpdateMangoJobDefinitionCommand`；`SaveMangoJobAlarmRuleCommand` 拆为 `CreateMangoJobAlarmRuleCommand` 和 `UpdateMangoJobAlarmRuleCommand`。`MangoJobNoticeBizTypes` 不再是业务公共 API，业务自定义通知应声明自己的 biz type 和消息模板，不能依赖 `mango-job-core` 常量。

### 5.5 Resource 声明和处理器

1. HTTP 注册调用：`ResourceRegistryApi` 改为 `ResourceDeclarationApi`。
2. 本地扩展点和模型从 `mango-resource-api` 移到 `mango-resource-support`：

```text
io.mango.resource.api.ResourceProvider        -> io.mango.resource.support.ResourceProvider
io.mango.resource.api.ResourceHandler         -> io.mango.resource.support.ResourceHandler
io.mango.resource.api.ResourceTargetDispatcher -> io.mango.resource.support.ResourceTargetDispatcher
io.mango.resource.api.ResourceTypes           -> io.mango.resource.support.ResourceTypes
io.mango.resource.api.builder.*               -> io.mango.resource.support.builder.*
io.mango.resource.api.model.*                 -> io.mango.resource.support.model.*
```

其中 `ResourceDeclarationBuilder`、`ResourceFields`、`ResourceDeclaration`、`ResourceField`、`ResourceHandlerSpec` 和 `ResourceSyncResult` 类名不变，只把包前缀从 `io.mango.resource.api` 改为 `io.mango.resource.support`。

实现 Provider、Handler 或声明构造器的模块增加 `mango-resource-support` 依赖。最终应用按拓扑选择：

- 单体：`mango-resource-starter` + `mango-resource-sync-starter`。
- 普通微服务：`mango-resource-starter-remote` + `mango-resource-sync-starter`。
- 只调用 HTTP Command/Query/VO：保留 `mango-resource-api`。

### 5.6 Org 和 System

- `CreateOrgCommand` / `UpdateOrgCommand` 改为 `CreateSysOrgCommand` / `UpdateSysOrgCommand`。
- `SysOrgApi` 返回 `SysOrgVO`，业务代码不能继续接收 `SysOrg` Entity。
- 区域写入使用 `SaveAreaCommand`，普通读取使用 `SysAreaVO`，树形读取使用 `SysAreaTreeNodeVO`。
- `DictDataPo`、`DictTypePo`、`SysConfigPo`、`SysTenantPo` 作为写入参数时分别改为 `SaveDictDataCommand`、`SaveDictTypeCommand`、`SaveSysConfigCommand`、`SaveSysTenantCommand`；读取结果分别使用对应 VO。
- `UpdateConfigValueCommand` 改为 `SysConfigApi.updateValue(id, value)`。
- `UpdateTenantStatusCommand` 改为 `SysTenantApi.updateStatus(id, status)`。
- `TenantProvisionContext` 改为 `TenantProvisionCommand`。
- `SysLoginLogPo` / `SysOperationLogPo` 的查询结果改为 `SysLoginLogVO` / `SysOperationLogVO`；写日志改用 `LoginLogRecorder`、`RecordLoginLogCommand` 和 `RecordOperationLogCommand`，不再调用查询 API 的 `record` 方法。
- `SysI18nApi` 不再返回 `SysI18n` Entity、旧 `SysI18nVO` 或 `Map<String,Object>`。语言包读取使用 `publicInfo()`、`publicInfoByLang(lang)`、`languages()`、`getByName(name)`、`i18n(lang)` 及 `I18nLanguagePackVO`、`I18nEntryVO`、`SysI18nMessageVO`。
- `I18nMessageResourceDeclarations` 已删除。内置国际化文案改为 `META-INF/mango/resources/` 下的 `I18N_MESSAGE` Resource 声明。

### 5.7 Workflow 1.0.20 方法校验兼容

Issue [#511](https://github.com/HardyDou/mango/issues/511) 的 `HV000151` 已从 Maven 1.0.21 起修复，1.0.22 也包含该修复。Workflow 参数约束只由 API 接口声明，Controller 实现继承约束，不再重复或改变约束配置。

从 1.0.20 直接升级到 1.0.22 的业务应用应在统一升级所有 `mango-workflow-*` 依赖后删除针对 `WorkflowProcessController` 的 `MethodValidationExcludeFilter` 临时规避，再执行一次经过 Spring 方法校验代理的流程发起测试。不要保留排除规则作为长期配置，否则后续接口参数校验不会生效。

## 6. 编译和验证

先用编译器收敛所有旧符号：

```bash
mvn -f backend/pom.xml -DskipTests compile
```

再次执行旧符号扫描，结果应为空：

```bash
rg -n \
  'io\.mango\.identity\.api\.vo\.IdentityUserInfo\b|io\.mango\.job\.api\.handler|io\.mango\.resource\.api\.(ResourceProvider|ResourceHandler|ResourceTypes)\b|CmsAdminApi\b|ResourceRegistryApi\b|TokenContextHolder\b' \
  backend --glob '*.java'
```

然后执行业务仓完整质量入口：

```bash
mvn -f backend/pom.xml verify
```

如果业务仓启用了 `no-new-violations`，基线报告应来自同一仓库提交，并使用仓库相对路径或稳定 fingerprint。已发布的 `mango-maven-plugin:1.0.22` 对跨 worktree 绝对路径基线存在已知误报，见 [Issue #588](https://github.com/HardyDou/mango/issues/588)。仓库 `main` 已修复该问题，修复会进入后续 Maven 版本；使用 1.0.22 验证时，可以在业务仓忽略的 `.runtime/` 中临时归一化基线文件路径后复跑。临时基线、issue fingerprint 和规则配置保持在业务仓当前受控版本。

## 7. 数据库和部署

1. 备份现有数据库，不删除生产数据，不用全新 V1 替代历史升级。
2. 启动前执行 Flyway validate/migrate，检查各模块 schema history 没有 checksum mismatch。
3. 确认文件模块 `V2__default_file_access_mode_to_proxy.sql` 已执行；有意使用直连文件地址的部署需保留显式配置。
4. Resource Registry 服务先就绪，再发布声明来源服务；来源服务的 sync starter 会对未完成注册重试。
5. 所有后端服务统一使用 1.0.22 后，再升级 PMO、前端依赖和项目内 CLI。
6. 验收至少覆盖登录、租户选择、菜单与权限、业务模块实际使用的 Identity/CMS/Job/Resource/System 调用，以及启动日志中的 Flyway 和 Resource 同步结果。

## 8. 回滚

- Java 编译迁移提交可以整体回滚到升级前业务提交并恢复 `mango.version=1.0.21`。
- 如果 1.0.22 migration 已执行，不直接删除 Flyway history 或手工回退表结构；先恢复应用版本并按模块 migration 影响制定数据库恢复动作。
- 回滚仍使用已发布的公共 API 和既有质量门禁，不复制已删除的 `io.mango.*` 类，也不改为依赖 `*-core`。
