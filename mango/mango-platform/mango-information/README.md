# Mango 信息采集中心 使用说明

## 1. 概览

`mango-information` 提供独立的信息采集能力：授权人员在后台维护表单和采集项，访客通过托管表单页面或业务自建页面提交信息，运营人员处理当前表单的提交记录。模块独立于 CMS，不建立站点概念，采集数据不写入 CMS 文章、栏目或内容扩展字段。

业务开发者需要关注三件事：表单定义由后台维护并以 JSON 保存；提交记录保存提交值 JSON 与提交时的字段定义快照；公开提交由服务端确定租户归属，浏览器与托管页面不携带服务端 SK。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 表单管理 | 创建、配置、发布、停用、删除表单 | 后台页面 `/information/forms`；`GET /information/forms/page`、`POST /information/forms/save` |
| 表单设计 | 可视化设计采集项，规则由前端表单设计器产出 | 后台页面 `/#/information/forms/{id}/config`（复用 `form-create-designer`） |
| 表单详情与接入信息 | 查看编码、字段数、提交数、托管入口和服务端 SK | `GET /information/forms/detail` |
| 托管表单 | 访客直接填写并提交 | 页面 `/#/information/hosted?formCode=<code>`；`GET /information/open/forms/detail`、`POST /information/open/forms/submit` |
| 服务端接入 | 业务系统用当前表单专属 SK 代为提交 | `POST /information/open/forms/submit`（携带 `serverSecret`） |
| 提交记录 | 独立菜单入口；固定列 + 当前表单动态采集项列 + 操作列 | 后台页面 `/information/submissions`；`GET /information/submissions/page` |
| 提交处理 | 更新为待处理、处理中、已完成、已关闭并写备注 | `PUT /information/submissions/process` |
| 导出 | 导出当前筛选结果，手机号与邮箱默认脱敏 | `GET /information/submissions/export` |

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
| `application.yml` | `mango.persistence.mybatis-plus.tenant.default-tenant-id` | `1` | 无租户上下文时的默认租户 | 匿名公开请求按默认租户解析表单，写入时由持久化层填充租户 | `PersistenceMybatisPlusAutoConfiguration` |

## 5. API 与扩展

| 接口 | 方法 | 访问模式 | 说明 |
|------|------|----------|------|
| `/information/forms/page` | GET | LOGIN | 分页查询表单，支持 `keyword`、`status`、`page`、`size` |
| `/information/forms/detail` | GET | LOGIN | 表单详情，包含 `definitionJson`、`hostedPath` 与服务端 `serverSecret` |
| `/information/forms/save` | POST | LOGIN | 保存表单；`published=true` 时直接发布，否则保存草稿 |
| `/information/forms` | DELETE | LOGIN | 删除表单；已有提交记录时返回业务错误 |
| `/information/submissions/page` | GET | LOGIN | 分页查询提交记录，按 `formId`、`status`、`keyword` 过滤 |
| `/information/submissions/detail` | GET | LOGIN | 提交详情 |
| `/information/submissions/export` | GET | LOGIN | 导出当前筛选结果（CSV 文本） |
| `/information/submissions/process` | PUT | LOGIN | 更新处理状态与备注 |
| `/information/open/forms/detail` | GET | PUBLIC | 匿名查询已发布表单定义 |
| `/information/open/forms/submit` | POST | PUBLIC | 匿名提交；携带 `serverSecret` 时按服务端接入校验 |

### 表单定义格式

`definitionJson` 是前端表单设计器产出的 form-create 规则数组，例如：

```json
[
  { "type": "input", "field": "name", "title": "姓名", "validate": [{ "required": true }] },
  { "type": "select", "field": "city", "title": "城市", "options": [{ "label": "北京", "value": "bj" }] },
  { "type": "elDivider", "title": "展示区" }
]
```

服务端按规则的 `field` / `title` / `type` 提取采集项：带 `field` 的规则参与必填校验与提交记录动态列；没有 `field` 的布局与展示规则（`elDivider`、`elAlert`、`text` 等）不参与校验，也不接受提交值。历史 `{fieldCode, fieldName, fieldType}` 形状仍被兼容。

## 6. 数据与初始化

| 表 | 说明 |
|----|------|
| `information_form` | 表单主数据、字段定义 JSON、状态与服务端 SK 摘要；`tenant_id + form_code` 唯一 |
| `information_submission` | 提交记录、提交值 JSON、来源页面、幂等键与处理状态；`tenant_id + form_id + idempotency_key` 唯一 |
| `information_submission_snapshot` | 提交时的字段定义快照，`submission_id` 唯一 |

迁移脚本：`mango-information-core/src/main/resources/db/migration/mango-information/V1__init_mango_information.sql`。

资源声明：

- 菜单：`mango-information-starter/src/main/resources/META-INF/mango/resources/information-common-menu.json`
- 公开 API：`mango-information-starter/src/main/resources/META-INF/mango/resources/information-common-public-api.yml`

## 7. 失败语义与边界

- 浏览器提交和托管表单不携带服务端 SK；服务端接入必须使用当前表单专属 SK。
- 客户端不得覆盖租户归属；租户由服务端按解析到的表单确定。
- 校验失败不形成提交记录；幂等键命中时返回同一提交编号。
- 已关闭的提交记录不可继续处理。
- 已发布表单才能被公开查询和提交；停用或草稿状态返回业务错误。
