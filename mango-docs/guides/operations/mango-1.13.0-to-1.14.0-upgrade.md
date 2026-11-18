<!-- mango-release-upgrade-guide: generated -->
# Mango 1.13.0 → 1.14.0 升级指南

**适用对象：使用 Mango Maven 1.13.0 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：`v2026.11.18-maven-1.14.0-captcha-sms-provider-release`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：`1.13.0` → `1.14.0`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

`@mango/common@2.3.4`、`@mango/admin-extension@1.1.8`、`@mango/ai@1.2.8`、`@mango/auth@1.0.45`、`@mango/file@1.3.1`、`@mango/detail@1.1.14`、`@mango/grid-layout@1.0.34`、`@mango/grid-widgets@1.0.40`、`@mango/home@1.0.33`、`@mango/information@1.1.8`、`@mango/pmo@1.6.2`、`@mango/rbac@1.3.4`、`@mango/site-shell@2.0.6`、`@mango/system@1.1.6`、`@mango/admin-pages@1.3.4`、`@mango/calendar@1.0.61`、`@mango/cms@1.1.6`、`@mango/job@1.0.54`、`@mango/link@1.0.47`、`@mango/notice@1.0.67`、`@mango/numgen@1.0.62`、`@mango/payment@1.0.53`、`@mango/template@1.0.62`、`@mango/workflow@1.3.1`、`@mango/admin-shell@1.1.10`、`@mango/workflow-business-example@1.0.72`、`@mango/admin@2.0.12`、`@mango/cli@1.4.15`

| 包 | 升级前 | 升级后 |
|---|---:|---:|
| `@mango/common` | `2.3.3` | `2.3.4` |
| `@mango/admin-extension` | `1.1.7` | `1.1.8` |
| `@mango/ai` | `1.2.7` | `1.2.8` |
| `@mango/auth` | `1.0.44` | `1.0.45` |
| `@mango/file` | `1.3.0` | `1.3.1` |
| `@mango/detail` | `1.1.13` | `1.1.14` |
| `@mango/grid-layout` | `1.0.33` | `1.0.34` |
| `@mango/grid-widgets` | `1.0.39` | `1.0.40` |
| `@mango/home` | `1.0.32` | `1.0.33` |
| `@mango/information` | `1.1.7` | `1.1.8` |
| `@mango/pmo` | `1.6.1` | `1.6.2` |
| `@mango/rbac` | `1.3.3` | `1.3.4` |
| `@mango/site-shell` | `2.0.5` | `2.0.6` |
| `@mango/system` | `1.1.5` | `1.1.6` |
| `@mango/admin-pages` | `1.3.3` | `1.3.4` |
| `@mango/calendar` | `1.0.60` | `1.0.61` |
| `@mango/cms` | `1.1.5` | `1.1.6` |
| `@mango/job` | `1.0.53` | `1.0.54` |
| `@mango/link` | `1.0.46` | `1.0.47` |
| `@mango/notice` | `1.0.66` | `1.0.67` |
| `@mango/numgen` | `1.0.61` | `1.0.62` |
| `@mango/payment` | `1.0.52` | `1.0.53` |
| `@mango/template` | `1.0.61` | `1.0.62` |
| `@mango/workflow` | `1.3.0` | `1.3.1` |
| `@mango/admin-shell` | `1.1.9` | `1.1.10` |
| `@mango/workflow-business-example` | `1.0.71` | `1.0.72` |
| `@mango/admin` | `2.0.11` | `2.0.12` |
| `@mango/cli` | `1.4.14` | `1.4.15` |

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

### Fixed

- 短信验证码发送失败不再静默返回 `200` 加空 `data`：调用方收到业务错误码 `2410`，验证码缓存不写入，日志记录 `provider`、`templateCode`、缺失配置项键名与第三方错误码，手机号打码。
- 全仓 Maven 模块的 Surefire 绑定统一到 3.2.5 后暴露的 8 个历史用例失败已修正根因（依赖清单缺项、MyBatis-Plus lambda 缓存未初始化、H2 时间精度、资源声明历史枚举别名、SM4 测试密钥缺失、MySQL 并发用例环境门控、通知模板物理回收语义、文件预览线程时序与 JVM 临时文件竞态），没有用跳过或 `@Disabled` 掩盖。
- `mango-pmo` 能力文档门禁不再把测试源码算作能力变更。

### Added

- `mango-captcha-starter` 内置阿里云短信通道：`AliyunSmsProvider`、`AliyunSmsSender`、`AliyunSmsMessage`，按 `mango.captcha.sms.provider=aliyun` 装配，读取 `access-key-id`、`access-key-secret`、`sign-name`，可选 `endpoint`（默认 `dysmsapi.aliyuncs.com`）与 `region`（默认 `cn-hangzhou`）。
- `mango-captcha-starter` 内置腾讯云短信通道：`TencentSmsProvider`、`TencentSmsSender`、`TencentSmsMessage`，按 `mango.captcha.sms.provider=tencent` 装配，读取 `secret-id`、`secret-key`、`sign-name`、`sdk-app-id`，可选 `region`（默认 `ap-guangzhou`）、`endpoint`（默认 `sms.tencentcloudapi.com`）与 `country-code`（默认 `+86`）。
- `mango.captcha.sms.template-code` 与 `mango.captcha.sms.aliyun.*`、`mango.captcha.sms.tencent.*` 配置项；`CaptchaCode` 新增 `CAPTCHA_SEND_FAILED`（`2410`）。
- `mango-pmo` 新增 `tools/check-surefire-versions.mjs` 门禁与配套用例，拒绝模块把 Surefire 钉到 3.0.0 以下或在根 POM 与 `mango-parent` 之外自行声明版本。

### Changed

- 短信验证码失败语义：`POST /auth/captcha/send` 从 `200` 加空 `data` 变为业务错误码 `2410`，`CaptchaService` 不再 `return null`。
- `mango-captcha-starter` 打包带入 `com.aliyun:dysmsapi20170525` 与 `com.tencentcloudapi:tencentcloud-sdk-java-sms`；两个依赖版本由 `mango-bom` 统一管理，本批次未改 BOM。
- `SmsCaptcha.vue` 只在后端返回非空 key 时提示「验证码已发送」并启动倒计时，其余情况提示发送失败且不再抛出未处理拒绝。
- Maven 全仓 Surefire 统一为 3.2.5，模块测试从“多数模块 `Tests run: 0`”变为真实执行；CI `java` 作业新增 `Enforce unified Surefire version baseline` 与 `Run tests for directly modified modules` 两步，`workflow_contract` 作业改用普通 `test` 生命周期。
- `@mango/pmo` 升到 `1.6.2`，业务基线投影同步刷新；`@mango/cli` 由版本矩阵自动升到 `1.4.15`。

## 3. 升级前检查

1. 确认数据库备份可恢复，尤其是 Resource Registry、Flyway history、Bootstrap receipt 和业务目标表。
2. 阅读当前版本对应的模块 README、能力地图和本文引用的专项指南。
3. 检查业务项目是否引用本批次删除或收紧的 API、配置、枚举、菜单或前端导出。
4. 记录当前 Maven、npm、CLI、PMO 版本以及构建锁文件；不要混用不同发布批次。
5. 先在预发布数据库执行只读检查，再进入维护窗口。

## 4. 依赖升级

### Maven

将业务项目 `backend/pom.xml` 的 Mango 版本锁定为：

```xml
<mango.version>1.14.0</mango.version>
```

同时确认 `io.mango:mango-docs-bundle:1.14.0` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


`@mango/common@2.3.4`、`@mango/admin-extension@1.1.8`、`@mango/ai@1.2.8`、`@mango/auth@1.0.45`、`@mango/file@1.3.1`、`@mango/detail@1.1.14`、`@mango/grid-layout@1.0.34`、`@mango/grid-widgets@1.0.40`、`@mango/home@1.0.33`、`@mango/information@1.1.8`、`@mango/pmo@1.6.2`、`@mango/rbac@1.3.4`、`@mango/site-shell@2.0.6`、`@mango/system@1.1.6`、`@mango/admin-pages@1.3.4`、`@mango/calendar@1.0.61`、`@mango/cms@1.1.6`、`@mango/job@1.0.54`、`@mango/link@1.0.47`、`@mango/notice@1.0.67`、`@mango/numgen@1.0.62`、`@mango/payment@1.0.53`、`@mango/template@1.0.62`、`@mango/workflow@1.3.1`、`@mango/admin-shell@1.1.10`、`@mango/workflow-business-example@1.0.72`、`@mango/admin@2.0.12`、`@mango/cli@1.4.15`

```bash
pnpm install
pnpm exec mango pmo check --locked
pnpm exec mango docs pull
```

CLI 生成项目使用本批次的 `@mango/cli`；不要用旧的全局 CLI 覆盖项目内锁定版本。

## 5. 数据库与启动验证

1. 备份后启动一次升级候选，等待 Flyway 与 Resource Bootstrap 完成。
2. 确认所有新增迁移成功，Bootstrap receipt 最终为 `FINALIZED`。
3. 对 Resource Registry 检查唯一键、同步模式、归属字段和冲突诊断。
4. 对业务模块执行本文第 2 节涉及的专项验证；迁移失败或身份冲突时停止，不要通过手工修改 Flyway history 绕过。

## 6. 业务回归

- 后端 API、配置、权限、菜单、初始化数据和租户边界。
- 前端构建、路由、页面注册、公开站点和已变更的 npm 导出。
- CLI 生成项目、PMO baseline、docs bundle 和 clean consumer。
- 对本批次 release notes 的 `Fixed`、`Added`、`Changed` 项逐项保留验证证据。

## 7. 回滚边界

- 已发布的 Maven/npm 坐标不可覆盖，只能发布新版本或回退消费者依赖。
- 数据库迁移是否可逆以对应模块 README 为准；不可逆数据操作需要在升级前完成归档或备份。
- 代码回退不等于数据回滚；保留升级前数据库快照、Bootstrap receipt、迁移日志和前端构建产物。

## 8. Agent 执行清单

| 步骤 | 动作 | 通过判据 | 停止条件 |
|---|---|---|---|
| 1 | 读取能力地图、模块 README、本文和专项升级指南 | 当前事实源与目标版本一致 | 文档冲突，先报告，不套用历史路径 |
| 2 | 盘点 API、配置、枚举、前端导出和数据库迁移影响 | 形成命中清单 | 发现破坏性引用未给出迁移方案 |
| 3 | 备份数据库和构建锁文件 | 可恢复证据已保存 | 无可验证备份 |
| 4 | 升级 Maven/npm/CLI 全闭包 | 依赖解析、构建与锁文件通过 | peer 冲突、坐标解析失败或混批次 |
| 5 | 执行迁移并启动 Bootstrap | Flyway 成功，receipt=FINALIZED | 迁移失败、冲突、ownership 未证明项未处理 |
| 6 | 执行 API、前端、CLI 和业务回归 | release notes 变更项均有证据 | 任一关键回归失败 |
| 7 | 保存升级证据并决定放行 | 命令、版本、日志、结果齐全 | 缺证据不要宣告完成 |

## 9. 关联记录

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/v2026.11.18-maven-1.14.0-captcha-sms-provider-release>
- 发布说明：仓库根目录 `CHANGELOG.md` 的本批次章节。
- 计划文件：
  - Release plan：`mango-ui/.changeset/release-plan.json`
  - Catalog：`mango-catalog/catalog.lock.json`
- 版本事实源：`mango-ui/packages/mango-cli/release-versions.json`
