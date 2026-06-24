# 系统 System

## 1. 概览

`mango-system` 是 Mango 的系统基础能力模块，向业务后台提供字典、系统配置、机构、行政区划、国际化、登录日志、操作日志和个人参数配置。

业务开发接入时主要关心三件事：

- 后端需要读取字典、配置、机构、行政区划、国际化或记录日志时，依赖 `mango-system-api`。
- 应用需要对外提供系统管理接口和初始化系统表时，启用 `mango-system-starter`。
- 管理后台需要系统管理页面或前端 API 封装时，使用 `@mango/system`。它属于 `admin-pages` 配套能力，不是官网、门户站点的通用页面组件。

## 2. 功能清单

| 能力 | 说明 | 常用入口 |
|------|------|----------|
| 字典 | 维护字典类型、字典数据，给表单下拉、筛选项、标签展示提供选项 | `DictApi`、`/system/dict`、`dictTypeApi`、`dictDataApi` |
| 系统配置 | 维护运行时配置键值和业务域配置控制面板，例如安全开关、业务开关、上传/邮件/短信分组参数 | `SysConfigApi`、`/system/config`、`configApi`、`SystemConfigPanel` |
| 机构 | 维护机构空间、机构类型、机构状态、机构套餐绑定，登录页读取启用机构选项 | `SysTenantApi`、`/system/tenant`、`tenantApi` |
| 行政区划 | 查询省市区树、子级区划、adcode 详情和启用区划 | `SysAreaApi`、`/system/area`、`areaApi` |
| 国际化 | 读取公开语言包、语言列表和指定国际化条目 | `SysI18nApi`、`/system/i18n` |
| 登录日志 | 查询、统计、清理登录日志 | `SysLoginLogApi`、`/system/log/login/*`、`loginLogApi` |
| 操作日志 | 查询、清理操作日志 | `SysOperationLogApi`、`/system/log/operation/*`、`operationLogApi` |
| 个人参数配置 | 按当前租户、当前用户保存页面偏好、筛选条件、提醒配置等个人配置 | `PersonalConfigApi`、`/system/personal-configs` |
| 机构初始化扩展 | 新建机构后触发各模块写入默认数据，删除机构前汇总依赖阻断原因 | `TenantProvisioner`、`TenantDependencyChecker`、`TenantPackageBindingHandler` |

## 3. 后端接入

开发业务代码时依赖 API 契约：

```xml
<dependency>
    <groupId>io.mango.platform.system</groupId>
    <artifactId>mango-system-api</artifactId>
</dependency>
```

应用需要提供系统管理接口、系统表 migration 和系统能力实现时，引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.system</groupId>
    <artifactId>mango-system-starter</artifactId>
</dependency>
```

常用 Java API：

| API | 用法 |
|-----|------|
| `DictApi` | 字典类型、字典数据、字典选项读取和维护。业务表单通常调用 `getOptions(typeCode)`。 |
| `SysConfigApi` | 系统配置列表、详情、创建、修改、删除、按配置键取值。业务开关通常调用 `getValue(configKey)`，布尔和整数配置可调用 `getBooleanValue(configKey, defaultValue)`、`getIntegerValue(configKey, defaultValue)`。 |
| `SysTenantApi` | 机构列表、详情、新增、修改、删除和状态调整。 |
| `SysAreaApi` | 行政区划树、子级、详情、adcode、启用区划读取。 |
| `SysI18nApi` | 国际化语言包、语言列表、国际化键读取。 |
| `SysLoginLogApi` | 登录日志分页、详情、记录、清理、统计。 |
| `SysOperationLogApi` | 操作日志分页、详情、记录、清理。 |
| `PersonalConfigApi` | 当前用户个人参数配置查询、保存、删除。 |

机构相关扩展点：

| 扩展点 | 什么时候实现 |
|--------|--------------|
| `TenantProvisioner` | 模块需要在新机构创建后初始化自己的默认数据。 |
| `TenantDependencyChecker` | 模块有机构内业务数据，删除机构前需要阻止误删。 |
| `TenantPackageBindingHandler` | 机构绑定套餐后，需要同步默认角色、菜单或授权关系。 |

## 4. 前端接入

管理后台使用 `@mango/system`：

```ts
import {
  DictView,
  ConfigView,
  TenantView,
  AreaView,
  LoginLogView,
  OperationLogView,
  dictDataApi,
  dictTypeApi,
  configApi,
  paramApi,
  tenantApi,
  areaApi,
  loginLogApi,
  operationLogApi,
} from '@mango/system';
import '@mango/system/style.css';
```

`@mango/system` 的定位：

| 标识 | 说明 |
|------|------|
| `admin-pages` | `DictView`、`ConfigView`、`TenantView`、`AreaView`、`LoginLogView`、`OperationLogView` 等管理后台页面。 |
| `business-component` | `ParticipantSelector`、`DomainSelector`、`DomainSideTree` 可被后台业务页面复用，但仍依赖 Mango 后台请求和上下文。 |
| `api-client` | `dictTypeApi`、`dictDataApi`、`configApi`、`paramApi`、`tenantApi`、`areaApi`、`loginLogApi`、`operationLogApi` 等请求封装。 |

前端包依赖 `vue`、`vue-router`、`element-plus`，并复用 `@mango/common` 的请求封装、`@mango/rbac` 的后台上下文和 `@mango/api-schema` 的类型。

## 5. 快速开始

1. 业务模块需要读取字典或配置时，在后端引入 `mango-system-api`。
2. 宿主应用需要提供系统管理能力时，引入 `mango-system-starter`，启动后执行 `system` migration。
3. 字典类字段先在 `sys_dict_type` 登记类型，再在 `sys_dict_data` 登记选项，业务通过 `DictApi.getOptions(typeCode)` 或 `GET /system/dict/data/options` 读取。
4. 运行时开关、阈值、文案等放入 `sys_config`，业务通过 `SysConfigApi.getValue(configKey)` 读取；需要后台控制面板展示时补充 `domainCode`、`valueType`、`optionSource`、`dictType`、`options`、`editable` 等元数据。
5. 管理后台接入 `@mango/system` 页面或 API 封装，并在菜单资源中绑定对应页面和权限码。
6. 新建机构时必须选择 `packageId`，系统会触发机构初始化扩展点，并把机构绑定到套餐。
7. 平台默认菜单和按钮权限来自 `system-common-menu.json` 的 `AUTH_MENU` 声明，不再通过 Flyway 菜单 DML 初始化。

## 6. 配置说明

`mango-system` 当前没有专属 `@ConfigurationProperties`。运行时可维护的配置来自数据库表 `sys_config`。

默认初始化的系统配置：

| 配置键 | 默认值 | 类型 | 含义 |
|--------|--------|------|------|
| `sys.index.skinName` | `skin-blue` | `SYSTEM` | 默认皮肤名称。 |
| `sys.account.captchaEnabled` | `true` | `SECURITY` | 账号登录验证码开关。 |
| `sys.account.registerEnabled` | `false` | `SECURITY` | 注册开关。 |
| `sys.login.lockCount` | `5` | `SECURITY` | 登录锁定次数。 |

系统配置支持按 `domainCode` 归类。字典类型也支持 `domainCode`，用于把公共字典、文件、模板、通知、编号、日历等配置分到不同业务域。

系统配置控制面板元数据：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `domainCode` | `COMMON` | 业务域编码，必须和 `mango-domain` 中启用业务域一致。 |
| `valueType` | `STRING` | 配置值展示与编辑类型：`BOOLEAN`、`STRING`、`NUMBER`、`RADIO`、`SELECT`、`MULTI_SELECT`、`DATE`、`DATE_RANGE`。 |
| `groupCode` | 空 | 配置分组编码，用于业务域内二次分组。 |
| `groupName` | 空 | 配置分组名称。 |
| `defaultValue` | 空 | 默认值。 |
| `options` | 空 | 自定义选项 JSON 数组，例如 `[{"label":"加急","value":"urgent"}]`。 |
| `optionSource` | `CUSTOM` | 选项来源：`CUSTOM` 使用 `options`，`DICT` 使用 `dictType` 读取字典。 |
| `dictType` | 空 | 绑定字典类型，仅 `optionSource=DICT` 时使用。 |
| `editable` | `true` | 是否允许在控制面板修改配置值。 |
| `editableReason` | 空 | 不可编辑时展示的原因。 |

`MULTI_SELECT` 和 `DATE_RANGE` 的配置值使用 JSON 字符串数组，例如 `["site","email"]`、`["2026-06-01","2026-06-23"]`。

### 6.1 业务模块接入配置面板

业务模块要在自己的后台页面展示配置面板时，按业务域组织配置：

1. 在 `mango-domain` 中登记并启用业务域，例如 `ORDER`、`CRM`、`SETTLEMENT`。业务域编码是前端 `SystemConfigPanel.domainCodes` 和后端 `sys_config.domain_code` 的连接点。
2. 在 `sys_config` 中维护本业务域配置。人工维护入口是后台 `参数配置` 页面；模块默认配置通过 `mango-resource` 的 `SYSTEM_CONFIG` 声明注入。
3. 配置定义放在 `参数配置` 页面维护：配置名称、参数键、业务域、展示类型、默认值、可选值、绑定字典、是否可编辑和配置介绍。
4. 业务前端页面嵌入 `SystemConfigPanel` 并传入业务域编码。普通业务页面默认只读；运营控制台需要改当前值时传 `readonly=false`。
5. 业务后端读取配置仍使用 `SysConfigApi`，例如 `getValue(configKey)`、`getBooleanValue(configKey, defaultValue)`、`getIntegerValue(configKey, defaultValue)`。前端面板只是维护当前值的入口，不替代业务后端校验。

示例配置：

| 业务域 | 配置键 | valueType | 说明 |
|--------|--------|-----------|------|
| `NOTICE` | `notice.center.enabled` | `BOOLEAN` | 通知中心总开关。 |
| `NOTICE` | `notice.retry.maxTimes` | `NUMBER` | 第三方渠道失败重试次数。 |
| `NOTICE` | `notice.channels.default` | `MULTI_SELECT` | 默认通知渠道，可绑定 `notice_channel` 字典。 |
| `WORKFLOW` | `workflow.task.defaultPriority` | `SELECT` | 默认审批优先级，可绑定 `workflow_task_priority` 字典。 |
| `CMS` | `cms.publish.channels.default` | `MULTI_SELECT` | 默认发布渠道，可绑定 `cms_publish_channel` 字典。 |

## 7. 资源注入

系统默认字典、系统默认配置和默认国际化文案通过 `mango-resource` 注入，不在 Flyway 中写业务配置数据。字典和配置支持资源文件，国际化默认文案由 `SystemI18nMessageResourceProvider` 声明。

资源文件放在：

```text
mango-system-starter/src/main/resources/META-INF/mango/resources/system-common-dict.yml
mango-system-starter/src/main/resources/META-INF/mango/resources/system-common-config.yml
mango-system-starter/src/main/resources/META-INF/mango/resources/system-common-menu.json
```

`system-common-menu.json` 声明系统管理菜单、行政区划菜单和 `system:area:*` 等按钮权限。按钮权限未显式配置 `packageCodes` 时会继承父菜单套餐；显式空数组表示不加入任何套餐。

### 7.1 SYSTEM_DICT

`SYSTEM_DICT` 落库到 `sys_dict_type` 和 `sys_dict_data`，按 `dictType` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `system.dict.authorization-role-type`。 |
| `target-module` | `STRING` | 是 | 固定为 `system`。 |
| `typeId` | `LONG` | 否 | 字典类型稳定 ID，不填时使用资源 ID。 |
| `dictType` | `STRING` | 是 | 字典类型编码，全局唯一。 |
| `dictName` | `STRING` | 是 | 字典类型名称。 |
| `domainCode` | `STRING` | 否 | 业务域编码，默认 `COMMON`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 禁用，默认 `1`。 |
| `remark` | `STRING` | 否 | 备注。 |
| `items` | `OBJECT[]` | 是 | 字典项列表，每项支持 `id`、`label`、`value`、`sort`、`status`、`remark`。 |

### 7.2 SYSTEM_CONFIG

`SYSTEM_CONFIG` 落库到 `sys_config`，按 `configKey` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `system.config.account-captcha-enabled`。 |
| `target-module` | `STRING` | 是 | 固定为 `system`。 |
| `configId` | `LONG` | 否 | 系统参数稳定 ID，不填时使用资源 ID。 |
| `configKey` | `STRING` | 是 | 系统参数 Key，全局唯一。 |
| `configValue` | `STRING` | 是 | 系统参数值。 |
| `configName` | `STRING` | 是 | 系统参数名称。 |
| `type` | `STRING` | 否 | `SYSTEM`、`BUSINESS`、`SECURITY`、`FEATURE`，默认 `SYSTEM`。 |
| `domainCode` | `STRING` | 否 | 业务域编码，默认 `COMMON`。 |
| `valueType` | `STRING` | 否 | 配置值展示与编辑类型，默认 `STRING`。 |
| `groupCode` | `STRING` | 否 | 配置分组编码。 |
| `groupName` | `STRING` | 否 | 配置分组名称。 |
| `defaultValue` | `STRING` | 否 | 默认值。 |
| `options` | `STRING` | 否 | 选项列表，JSON 字符串。 |
| `optionSource` | `STRING` | 否 | 选项来源：`CUSTOM`、`DICT`，默认 `CUSTOM`。 |
| `dictType` | `STRING` | 否 | 绑定字典类型，`optionSource=DICT` 时用于加载字典项。 |
| `editable` | `BOOLEAN` | 否 | 是否可编辑，默认 `true`。 |
| `editableReason` | `STRING` | 否 | 不可编辑原因。 |
| `sort` | `INT` | 否 | 排序号，默认 `0`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 禁用，默认 `1`。 |
| `remark` | `STRING` | 否 | 备注。 |

### 7.3 I18N_MESSAGE

`I18N_MESSAGE` 落库到 `sys_i18n`，按 `name` 合并更新。业务模块需要注入国际化文案时，只依赖 `mango-system-api` 和 `mango-resource-api`，使用 `I18nMessageResourceDeclarations.message(...)` 在本模块 starter 中实现 `ResourceProvider`。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `system.i18n.common.submit`。 |
| `target-module` | `STRING` | 是 | 固定为 `system`。 |
| `i18nId` | `LONG` | 否 | 国际化条目稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `STRING` | 否 | 当前 `sys_i18n` 未按租户隔离，默认 `1`，仅用于资源声明兼容。 |
| `name` | `STRING` | 是 | 国际化 Key，全局唯一。 |
| `zhCn` | `STRING` | 是 | 中文文案。 |
| `en` | `STRING` | 是 | 英文文案。 |
| `description` | `STRING` | 否 | 文案说明。 |

`sys_i18n` 当前没有 `status` 字段，资源禁用时会退化为物理删除；如果后续需要后台人工禁用，应先给 `sys_i18n` 增加状态字段。

## 8. API 与扩展

常用 HTTP 接口：

| 能力 | 接口 | 权限 |
|------|------|------|
| 字典类型列表 | `GET /system/dict/type/list` | `system:dict:type:list` |
| 字典类型详情 | `GET /system/dict/type/detail` | `system:dict:type:query` |
| 新增字典类型 | `POST /system/dict/type` | `system:dict:type:add` |
| 修改字典类型 | `PUT /system/dict/type` | `system:dict:type:edit` |
| 删除字典类型 | `DELETE /system/dict/type` | `system:dict:type:delete` |
| 字典数据列表 | `GET /system/dict/data/list` | `system:dict:data:list` |
| 字典数据详情 | `GET /system/dict/data/detail` | `system:dict:data:query` |
| 新增字典数据 | `POST /system/dict/data` | `system:dict:data:add` |
| 修改字典数据 | `PUT /system/dict/data` | `system:dict:data:edit` |
| 删除字典数据 | `DELETE /system/dict/data` | `system:dict:data:delete` |
| 字典选项 | `GET /system/dict/data/options` | 登录可访问 |
| 系统配置列表 | `GET /system/config/list` | `system:config:list` |
| 系统配置详情 | `GET /system/config/detail` | `system:config:query` |
| 新增系统配置 | `POST /system/config` | `system:config:add` |
| 修改系统配置 | `PUT /system/config` | `system:config:edit` |
| 删除系统配置 | `DELETE /system/config` | `system:config:delete` |
| 修改配置值 | `PUT /system/config/value` | `system:config:edit` |
| 配置分组 | `GET /system/config/groups` | `system:config:list` |
| 配置展示类型 | `GET /system/config/value-types` | `system:config:list` |
| 机构列表 | `GET /system/tenant/list` | `system:tenant:list` |
| 登录机构选项 | `GET /system/tenant/login-options` | 公开接口 |
| 机构详情 | `GET /system/tenant/detail` | `system:tenant:query` |
| 新增机构 | `POST /system/tenant` | `system:tenant:add` |
| 修改机构 | `PUT /system/tenant` | `system:tenant:edit` |
| 删除机构 | `DELETE /system/tenant` | `system:tenant:delete` |
| 修改机构状态 | `PUT /system/tenant/status` | `system:tenant:edit` |
| 行政区划树 | `GET /system/area/tree` | 登录可访问 |
| 行政区划详情 | `GET /system/area/detail` | `system:area:query` |
| 按 adcode 查询区划 | `GET /system/area/adcode` | 登录可访问 |
| 下级行政区划 | `GET /system/area/children` | 登录可访问 |
| 启用行政区划 | `GET /system/area/active` | 登录可访问 |
| 新增行政区划 | `POST /system/area` | `system:area:add` |
| 修改行政区划 | `PUT /system/area` | `system:area:edit` |
| 删除行政区划 | `DELETE /system/area` | `system:area:delete` |
| 公开国际化语言包 | `GET /system/i18n/public` | 公开接口 |
| 指定语言语言包 | `GET /system/i18n/public/lang` | 公开接口 |
| 支持语言列表 | `GET /system/i18n/languages` | 公开接口 |
| 按键名查询国际化 | `GET /system/i18n/public/name` | 公开接口 |
| 前端语言包 | `GET /system/i18n` | 公开接口 |
| 个人配置列表 | `GET /system/personal-configs` | 登录可访问 |
| 个人配置值 | `GET /system/personal-configs/value` | 登录可访问 |
| 保存个人配置 | `POST /system/personal-configs` | 登录可访问 |
| 删除个人配置 | `DELETE /system/personal-configs` | 登录可访问 |
| 登录日志列表 | `GET /system/log/login/list` | `system:log:login:list` |
| 登录日志详情 | `GET /system/log/login/detail` | `system:log:login:query` |
| 清理登录日志 | `DELETE /system/log/login/clean` | `system:log:login:delete` |
| 登录日志统计 | `GET /system/log/login/statistics` | `system:log:login:query` |
| 操作日志列表 | `GET /system/log/operation/list` | `system:log:operation:list` |
| 操作日志详情 | `GET /system/log/operation/detail` | `system:log:operation:query` |
| 清理操作日志 | `DELETE /system/log/operation/clean` | `system:log:operation:delete` |

常用入参和返回字段：

| 对象 | 必填字段 | 常用返回字段 |
|------|----------|--------------|
| `DictTypePo` | `dictType`、`dictName` | `id`、`dictType`、`dictName`、`domainCode`、`status`、`remark` |
| `DictDataPo` | `dictLabel`、`dictValue`、`dictType` | `id`、`dictType`、`dictLabel`、`dictValue`、`sort`、`status` |
| `DictOptionVO` | 无 | `label`、`value` |
| `SysConfigPo` | `configKey`、`configValue`、`configName`、`type` | `id`、`configKey`、`configValue`、`configName`、`type`、`domainCode`、`valueType`、`options`、`editable`、`status` |
| `SysTenantPo` | `tenantName`、`tenantCode`、`packageId`、`status` | `id`、`tenantName`、`tenantCode`、`institutionType`、`packageId`、`capabilityCodes`、`status` |
| `SysArea` | 新增时传 `pid`、`name` | `id`、`pid`、`name`、`letter`、`adcode`、`location`、`areaSort`、`areaStatus`、`areaType`、`hot`、`cityCode`、`tenantId` |
| `SysI18n` | 无 | `id`、`name`、`zhCn`、`en`、`description` |
| `SavePersonalConfigCommand` | `groupCode`、`bizType`、`configKey`、`configValue` | `id`、`tenantId`、`userId`、`groupCode`、`bizType`、`configKey`、`configValue`、`valueType` |
| `SysLoginLogPo` | 记录时按调用方传入 | `id`、`tenantId`、`userId`、`username`、`loginType`、`ip`、`location`、`browser`、`os`、`status`、`msg`、`loginTime` |
| `SysOperationLogPo` | 记录时按调用方传入 | `id`、`tenantId`、`userId`、`username`、`module`、`operation`、`method`、`url`、`status`、`errorMsg`、`duration`、`operateTime` |

## 9. 数据与初始化

Flyway 路径：`mango-system-core/src/main/resources/db/migration/system`。

核心表：

| 表 | 用途 |
|----|------|
| `sys_dict_type` | 字典类型，包含 `dict_type`、`dict_name`、`domain_code`、`status`。 |
| `sys_dict_data` | 字典数据，包含 `dict_type`、`dict_label`、`dict_value`、`sort`、`status`。 |
| `sys_config` | 系统配置，包含 `config_key`、`config_value`、`config_name`、`type`、`domain_code`、`value_type`、`options`、`editable`、`status`。 |
| `sys_tenant` | 机构，包含机构编码、名称、类型、套餐、能力编码、状态和联系人信息。 |
| `sys_login_log` | 登录日志。 |
| `sys_operation_log` | 操作日志。 |
| `sys_area` | 行政区划。 |
| `sys_i18n` | 国际化条目。 |
| `sys_personal_config` | 当前用户个人配置。 |

初始化内容：

- `V1__init_system.sql` 创建系统基础表，并初始化默认机构、行政区划和国际化表。
- `V5__personal_config.sql` 创建 `sys_personal_config`。
- `V6__retire_route_management.sql` 删除历史 `system_route_type` 字典并移除 `sys_route_conf`。
- `V7__dict_domain.sql` 给 `sys_dict_type` 增加 `domain_code`。
- `V8__config_domain.sql` 给 `sys_config` 增加 `domain_code`。
- `V9__dict_domain_seed_classification.sql` 按字典编码前缀给模板、文件、流程、通知、编号、日历等字典补充业务域。

系统默认字典和系统默认配置通过 `mango-resource` 注入，资源文件是 `system-common-dict.yml` 和 `system-common-config.yml`。

菜单、按钮和 API 权限不是在 `mango-system` 自己的 SQL 中维护，统一由 `mango-authorization` 的资源采集和 manifest 入库能力维护。`mango-system-starter` 的 `module.properties` 只登记模块扫描信息：

```properties
module-name=mango-system
module-path=/system
```

机构初始化发生在新增机构接口成功写入 `sys_tenant` 后。系统模块会设置当前机构上下文，然后调用所有已注册的 `TenantProvisioner`；删除机构前会调用所有 `TenantDependencyChecker`，任一模块返回阻断原因都会禁止删除。

## 10. 管理入口

后端权限码：

| 菜单能力 | 权限码 |
|----------|--------|
| 字典类型 | `system:dict:type:list`、`system:dict:type:query`、`system:dict:type:add`、`system:dict:type:edit`、`system:dict:type:delete` |
| 字典数据 | `system:dict:data:list`、`system:dict:data:query`、`system:dict:data:add`、`system:dict:data:edit`、`system:dict:data:delete` |
| 系统配置 | `system:config:list`、`system:config:query`、`system:config:add`、`system:config:edit`、`system:config:delete` |
| 机构 | `system:tenant:list`、`system:tenant:query`、`system:tenant:add`、`system:tenant:edit`、`system:tenant:delete` |
| 行政区划 | `system:area:query`、`system:area:add`、`system:area:edit`、`system:area:delete` |
| 登录日志 | `system:log:login:list`、`system:log:login:query`、`system:log:login:delete` |
| 操作日志 | `system:log:operation:list`、`system:log:operation:query`、`system:log:operation:delete` |

前端页面导出：

| 页面 | 导出 |
|------|------|
| 字典管理 | `DictView` |
| 系统配置 | `ConfigView` |
| 机构管理 | `TenantView` |
| 行政区划 | `AreaView` |
| 登录日志 | `LoginLogView` |
| 操作日志 | `OperationLogView` |
| 公共路径 | `PublicPathView` |
| 业务域 | `DomainView` |
| 系统事件 | `SystemEventView` |

`PublicPathView`、`DomainView`、`SystemEventView` 使用的是其他后端能力的接口封装，接入这些页面时要同时确认对应后端模块已启用。

## 11. 问题排查

- 字典选项为空：先查 `sys_dict_type` 是否存在对应 `dict_type`，再查 `sys_dict_data` 是否有启用数据；如果按业务域过滤，还要确认 `domain_code`。
- 系统配置读不到：确认 `sys_config.config_key` 是否存在且状态启用；业务侧读取时不要把 YAML 配置和数据库运行时配置混用。
- 新建机构失败：检查 `tenantName`、`tenantCode`、`packageId`、`status` 是否传入；`status` 必须符合 `institution_status`。
- 新建机构后菜单或默认数据不完整：确认对应模块是否实现 `TenantProvisioner`，机构套餐是否能被 `TenantPackageBindingHandler` 正常绑定。
- 删除机构被阻止：查看返回消息中的阻断原因；这些原因来自各模块的 `TenantDependencyChecker`。
- 登录页没有机构选项：确认机构状态为启用，并检查 `GET /system/tenant/login-options` 是否可访问。
- 行政区划树层级不对：检查 `GET /system/area/tree` 的 `type` 参数，默认只返回省级。
- 个人配置串用户：个人配置按当前租户和当前用户保存，排查请求头里的租户上下文和登录态。

## 12. 相关文档

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [文档资产边界](../../../mango-pmo/rules/06-document-assets.md)
