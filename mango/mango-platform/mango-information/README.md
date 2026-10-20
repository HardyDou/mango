# Mango 信息采集 使用说明

## 1. 概览

`mango-information` 提供独立的信息采集能力：授权人员在后台维护表单和采集项，访客通过托管表单页面或业务自建页面提交信息，运营人员处理当前表单的采集记录。模块独立于 CMS，不建立站点概念，采集数据不写入 CMS 文章、栏目或内容扩展字段。

业务开发者需要关注三件事：表单定义由后台维护并以 JSON 保存；采集记录保存提交值 JSON 与提交时的字段定义快照；公开提交由服务端确定租户归属，浏览器与托管页面不携带服务端 SK。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 表单管理 | 创建、配置、发布、停用、删除表单 | 后台页面 `/information/forms`；`GET /information/forms/page`、`POST /information/forms/save` |
| 表单设计 | 可视化设计采集项，规则由前端表单设计器产出 | 后台页面 `/#/information/forms/{id}/config`（复用 `form-create-designer`） |
| 表单详情与接入信息 | 查看编码、字段数、提交数、托管入口和服务端 SK | `GET /information/forms/detail` |
| 托管表单 | 访客直接填写并提交 | 页面 `/#/information/hosted?formCode=<code>`；`GET /information/open/forms/detail`、`POST /information/open/forms/submit` |
| 服务端接入 | 业务系统用当前表单专属 SK 代为提交 | `POST /information/open/forms/submit`（携带 `serverSecret`） |
| 公开文件上传 | 匿名托管表单先领票据再上传，文件保存到 mango-file | 票据 `POST /information/open/forms/{formCode}/upload-ticket`；上传 `POST .../files`（`X-Mango-Upload-Ticket` 头）；预览 `GET .../files/preview?id=` |
| 采集记录 | 独立菜单入口；固定列 + 当前表单动态采集项列 + 操作列 | 后台页面 `/information/submissions`；`GET /information/submissions/page` |
| 提交处理 | 更新为待处理、处理中、已完成、已关闭并写备注 | `PUT /information/submissions/process` |
| 导出 | 导出当前筛选结果，字段按原始值输出（不脱敏） | `GET /information/submissions/export` |

## 3. 接入方式

开发时依赖：

```xml
<dependency>
  <groupId>io.mango.platform.information</groupId>
  <artifactId>mango-information-api</artifactId>
</dependency>
```

部署时依赖（单体或业务应用聚合）：

```xml
<dependency>
  <groupId>io.mango.platform.information</groupId>
  <artifactId>mango-information-starter</artifactId>
</dependency>
```

前端管理页面：

```bash
pnpm add @mango/information
```

```ts
import { registerMangoInformationAdminPages } from '@mango/information/admin-pages';
import '@mango/information/style.css';

registerMangoInformationAdminPages();
```

已登记的 Mango 能力：

| Mango 能力 | 本模块使用位置 | 文档入口 |
|------------|----------------|----------|
| Persistence 持久化 | `InformationFormMapper`、`InformationSubmissionMapper`、`InformationSubmissionSnapshotMapper` | `mango/mango-infra/mango-infra-persistence/README.md` |
| Infra Context 上下文 | `InformationFormService`、`InformationSubmissionService` 的租户上下文 | `mango/mango-infra/mango-infra-context/README.md` |
| Resource Registry | `META-INF/mango/resources/information-common-menu.json`、`information-common-public-api.yml` | `mango/mango-platform/mango-resource/README.md` |
| Authorization ApiAccess | `InformationFormController`、`InformationSubmissionController`、`InformationPublicController` | `mango/mango-platform/mango-authorization/README.md` |

## 4. 配置说明

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `application.yml` | `mango.information.enabled` | `true` | 是否装配信息采集模块 | 关闭后模块自动配置不生效 | `InformationAutoConfiguration` |
| `application.yml` | `mango.persistence.flyway.modules.mango-information.enabled` | 未设置 | 是否执行信息采集迁移 | 未设置且 classpath 存在该模块迁移时启动失败并提示声明 | `PersistenceFlywayAutoConfiguration` |
| `application.yml` | `mango.information.upload.ticket-ttl-seconds` | `600` | 上传票据有效期（秒） | 过期后上传被拒，需重新领取票据 | `InformationFileService` |
| `application.yml` | `mango.information.upload.restrict` | `false` | 是否启用大小、扩展名与预览签名限制 | 首版关闭；置为 `true` 时限制立即生效 | `InformationFileService` |
| `application.yml` | `mango.information.upload.max-size-bytes` | `20971520` | 启用限制时的单文件大小上限 | 超出直接拒绝，不写文件记录 | `InformationFileService` |
| `application.yml` | `mango.information.upload.allowed-extensions` | 常见文档与图片类型 | 启用限制时允许的扩展名 | 不合规类型直接拒绝 | `InformationFileService` |
| `application.yml` | `mango.information.upload.sign-secret` | 模块默认值 | 票据与预览签名密钥 | 生产建议替换为独立密钥 | `InformationFileService` |
| `application.yml` | `mango.persistence.mybatis-plus.tenant.default-tenant-id` | `1` | 无租户上下文时的默认租户 | 匿名公开请求按默认租户解析表单，写入时由持久化层填充租户 | `PersistenceMybatisPlusAutoConfiguration` |

## 5. API 与扩展

| 接口 | 方法 | 访问模式 | 说明 |
|------|------|----------|------|
| `/information/forms/page` | GET | LOGIN | 分页查询表单，支持 `keyword`、`status`、`page`、`size` |
| `/information/forms/detail` | GET | LOGIN | 表单详情，包含 `definitionJson`、`hostedPath` 与服务端 `serverSecret` |
| `/information/forms/save` | POST | LOGIN | 保存表单；`published=true` 时直接发布，否则保存草稿 |
| `/information/forms` | DELETE | LOGIN | 删除表单；已有采集记录时返回业务错误 |
| `/information/submissions/page` | GET | LOGIN | 分页查询采集记录，按 `formId`、`status`、`keyword` 过滤 |
| `/information/submissions/detail` | GET | LOGIN | 提交详情 |
| `/information/submissions/export` | GET | LOGIN | 导出当前筛选结果（CSV 文本） |
| `/information/submissions/process` | PUT | LOGIN | 更新处理状态与备注 |
| `/information/open/forms/detail` | GET | PUBLIC | 匿名查询已发布表单定义 |
| `/information/open/forms/submit` | POST | PUBLIC | 匿名提交；携带 `serverSecret` 时按服务端接入校验 |
| `/information/open/forms/{formCode}/upload-ticket` | POST | PUBLIC | 匿名领取短效上传票据，票据绑定表单编码并带有效期 |
| `/information/open/forms/{formCode}/files` | POST | PUBLIC | 匿名上传当前表单附件，必须携带有效票据；文件保存到 mango-file |
| `/information/open/forms/{formCode}/files/detail` | GET | PUBLIC | 匿名查询本表单已上传文件信息 |
| `/information/open/forms/{formCode}/files/preview` | GET | PUBLIC | 匿名读取文件内容；启用限制时要求表单签名 |

### 表单定义格式

`definitionJson` 是前端表单设计器产出的 form-create 规则数组，例如：

```json
[
  { "type": "input", "field": "name", "title": "姓名", "validate": [{ "required": true }] },
  { "type": "select", "field": "city", "title": "城市", "options": [{ "label": "北京", "value": "bj" }] },
  { "type": "elDivider", "title": "展示区" }
]
```

服务端按规则的 `field` / `title` / `type` 提取采集项：带 `field` 的规则参与必填校验与采集记录动态列；没有 `field` 的布局与展示规则（`elDivider`、`elAlert` 等）不参与校验，也不接受提交值。历史 `{fieldCode, fieldName, fieldType}` 形状仍被兼容。

控件支持范围与设计器一致，由前端 form-create 运行时渲染，包括输入、数字、选择、单选、多选、开关、评分、滑块、日期时间、颜色、级联、树形、上传、富文本、子表单、表格表单以及栅格/卡片/标签页等布局；`group`、`subForm`、`tableForm` 计为一个采集项，其子字段是行模板，不单独成为提交字段。多选与子表单等复杂值按 JSON 原样保存，采集记录列、详情和导出统一序列化为可读文本。

## 6. 用 Resource Registry 内置表单

业务模块可以把自己的采集表单随代码发布，由 Resource Registry 在启动时同步到 `information_form`，不需要人工在后台建表。

### 6.1 资源类型与幂等键

- 资源类型：`INFORMATION_FORM`
- 目标表：`information_form`
- 幂等键：`tenantId + formCode`；同一份声明重复同步只更新定义，不会产生重复表单
- 租户作用域：声明中的 `tenantId` 决定写入哪个租户，缺省为 `1`
- 主键：`targetId` 可选；不填时优先取资源声明 ID，取不到则自动生成

### 6.2 声明格式

放在模块自己的 `META-INF/mango/resources/*.yml`（或 `.json`）里，与菜单、API 资源同一套资源清单：

```yaml
mango:
  resource:
    schema-version: 1
    module-code: mango-example
    module-name: mango-example
    declarations:
      INFORMATION_FORM:
        - id: "2951400000000099001"
          version: 1
          biz-key: information.form.example-feedback
          name: 示例反馈表
          target-module: information
          fields:
            tenantId: { type: STRING, value: "1" }
            formCode: { type: STRING, value: example-feedback }
            formName: { type: STRING, value: 示例反馈表 }
            description: { type: STRING, value: 随代码发布的内置采集表单 }
            successMessage: { type: STRING, value: 提交成功 }
            status: { type: STRING, value: PUBLISHED }
            definitionJson:
              type: STRING
              value: |
                [
                  {"type":"input","field":"name","title":"姓名","validate":[{"required":true,"message":"请填写姓名"}]},
                  {"type":"select","field":"topic","title":"咨询类型","options":[{"label":"产品","value":"product"}]},
                  {"type":"rate","field":"score","title":"满意度","props":{"max":5}}
                ]
```

### 6.3 字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `formCode` | 是 | 表单编码，租户内唯一，托管地址按它访问 |
| `formName` | 是 | 表单名称 |
| `definitionJson` | 是 | form-create 规则数组 JSON，与表单设计器产物同构；至少含一个带 `field` 的采集项 |
| `tenantId` | 否 | 写入的租户，默认 `1` |
| `targetId` | 否 | 表单主键，默认取资源声明 ID 或自动生成 |
| `description` | 否 | 备注 |
| `successMessage` | 否 | 提交成功提示，默认「提交成功」 |
| `status` | 否 | `DRAFT`、`PUBLISHED`、`DISABLED`，默认 `PUBLISHED` |

### 6.4 行为与边界

- 新增时自动生成服务端 SK；后续同步只覆盖定义与元信息，**不会重置 SK**，避免影响已接入的服务端调用。
- 声明更新即覆盖：内置表单的定义以资源声明为准，后台对内置表单的字段修改会在下次同步被覆盖；需要人工维护的表单不要用资源声明，直接在后台创建。
- 停用：资源声明被置为停用或删除时，处理器把表单状态改为 `DISABLED`（`disable`）或删除记录（`delete`）。
- 定义非法（缺少必填字段、`definitionJson` 不是数组或没有可提交采集项）时同步失败并阻断启动，避免带病发布。

## 7. 数据与初始化

| 表 | 说明 |
|----|------|
| `information_form` | 表单主数据、字段定义 JSON、状态与服务端 SK 摘要；`tenant_id + form_code` 唯一 |
| `information_submission` | 采集记录、提交值 JSON、来源页面、幂等键与处理状态；`tenant_id + form_id + idempotency_key` 唯一 |
| `information_submission_snapshot` | 提交时的字段定义快照，`submission_id` 唯一 |

迁移脚本：`mango-information-core/src/main/resources/db/migration/mango-information/V1__init_mango_information.sql`。

资源声明：

- 菜单：`mango-information-starter/src/main/resources/META-INF/mango/resources/information-common-menu.json`
- 公开 API：`mango-information-starter/src/main/resources/META-INF/mango/resources/information-common-public-api.yml`

## 8. 失败语义与边界

- 浏览器提交和托管表单不携带服务端 SK；服务端接入必须使用当前表单专属 SK。
- 客户端不得覆盖租户归属；租户由服务端按解析到的表单确定。
- 校验失败不形成采集记录；幂等键命中时返回同一记录ID。
- 已关闭的采集记录不可继续处理。
- 已发布表单才能被公开查询和提交；停用或草稿状态返回业务错误。

## 9. 管理入口

后台菜单由 `information-common-menu.json` 注册，页面由 `@mango/information` 的 `registerMangoInformationAdminPages()` 注册。管理接口使用 `information:form:*`、`information:submission:*` 权限资源；公开接口由 `information-common-public-api.yml` 的 `API_RESOURCE` 声明 `accessMode=PUBLIC` 放行，不要求登录。

匿名访问只有这一个判定入口：请求由 `ApiResourceAuthorizationManager` 按 `authorization_api_resource` 匹配，资源未匹配时按 `LOGIN` 处理并返回 401。应用必须装配 `mango-authorization-resource-access-starter`（`mango-admin-starter` 已包含）并完成 Resource Registry 同步；`mango.auth.security.permit-paths` 不需要手工配置 `/information/open/**`。业务应用仍应由自身登录、租户和角色配置决定哪些管理人员可见菜单。

## 10. 快速开始

1. 引入 `mango-information-starter`，并按部署配置启用 `mango.information.enabled`。
2. 显式确认 `mango.persistence.flyway.modules.mango-information.enabled`，启动完成后检查三张信息采集表已创建。
3. 前端注册信息采集页面并加载 `@mango/information/style.css`，确认菜单与权限资源同步成功。
4. 创建并发布一份表单，使用托管地址提交一条记录，再在采集记录页面处理、导出并核对字段快照。
5. 需要业务系统代提交时，从表单详情取得专属 SK，仅在服务端请求中发送 `serverSecret`；浏览器和托管页面不得暴露该值。

## 11. 问题排查

- 菜单或页面 404：确认 `information-common-menu.json` 已同步、前端 registrar 已执行且页面 key 与菜单 `component` 一致。
- 启动提示迁移未声明：检查 `mango.persistence.flyway.modules.mango-information.enabled` 与 starter/classpath 配置。
- 公开表单不存在：确认表单编码、租户解析和状态为 `PUBLISHED`；匿名请求按默认租户解析。
- 提交被拒：检查字段定义、幂等键、表单状态和服务端 SK；公开浏览器提交不应携带 SK。
- 上传失败：确认先领取与表单编码绑定的短效票据，检查票据请求头、文件存储配置和票据有效期。
- 业务接口未返回数据：检查当前登录人的菜单/API 权限与租户上下文，不要用前端菜单显示代替后端授权检查。

## 12. 相关文档

- [能力地图](../../../mango-docs/capabilities/README.md)
- [信息采集前端包](../../../mango-ui/packages/information/README.md)
- [文件能力](../mango-file/README.md)
- [资源注册](../mango-resource/README.md)
- [授权能力](../mango-authorization/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
