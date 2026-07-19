# Mango 前端生产候选证据（2026-07-19）

## 1. 目的与判定

本证据确认 Vue 3、Element Plus、Vite 前端规范已经形成可供 Mango 自身和业务项目共同执行的本地生产候选。规范以单拥有者模式治理，机器门禁负责判定，专家复核只提供缺陷证据，不增加人工审批链。

当前状态：`PRODUCTION_CANDIDATE_LOCAL`。

当前不能标记为 `PRODUCTION_GRADUATED`。远端 required check、合并、Nexus 精确版本回读、授权环境真实流量灰度和故障回退尚无事实证据；本地测试不能替代这些外部动作。

## 2. 候选身份

| 项目                       | 实际值                                                                    |
| -------------------------- | ------------------------------------------------------------------------- |
| 被签名的功能提交           | `339fa12271e4d4a5914f437083fd00b57d5f4f28`                                |
| 分支                       | `docs/frontend-standards-plan`                                            |
| 一键入口                   | `pnpm -C mango-ui check:production-candidate`                             |
| 固定镜像                   | `mango/frontend-quality:node22-pnpm11.14`                                 |
| 镜像 identity              | `sha256:2a04ce0242088af26fd0b147318842ae55ef09bcc2f917126347ff3ac0d2cf30` |
| Node / pnpm                | `22.23.1` / `11.14.0`                                                     |
| 平台                       | `linux/arm64`                                                             |
| 发布候选                   | 29 个精确版本 npm tarball                                                 |
| preparation report SHA-256 | `6ad3a3f09104ca131bf7430e85617d15e0ed029c6b5fd1f332beff5809f5b147`        |
| sealed report SHA-256      | `4c265e7fe5301f11714062e5db6b4eb43af2014e5208b007aafbc8c3d714e14a`        |
| 业务 lockfile SHA-256      | `cdc1be8bfae39ac273360ce389bcd0e702930b0eb420f11febd466576349e79b`        |

准备报告逐包记录名称、版本和 tarball SHA-256。容器通过 Git worktree common-dir 只读挂载获得真实提交，项目、package store、tarball 和报告全部写入 Docker named volume；通过后只把报告复制回仓库，避免宿主绑定挂载的文件系统语义污染结论。

## 3. 规范落地范围

- 根级工具链固定为 ESLint 10、Prettier 3、Stylelint 17、TypeScript 5.9、vue-tsc 3、Vite 7、Vitest 4 和 Playwright 1.61，版本唯一性由机器检查。
- 业务 API 放在独立 `packages/<module>-api`，只依赖 `@mango/api-schema` 的中立 `HttpClient`；页面、Vue、Element Plus、Axios、路由和环境变量不得进入 API 包。
- `@mango/http-client` 是 Axios 1.18.1 的 FE1 适配器。host 为每个应用实例创建并注入客户端，业务页面不创建 Axios 实例；取消使用 `AbortSignal`，错误统一为 `HttpError`。
- 业务 UI 包拥有页面、私有组件、公共业务组件和显式 `style.css`。页面私有样式使用 scoped/module；跨页面样式随包导出，主题 token 归平台主题层。
- 当前微前端实现为 Wujie，厂商 API 受限在 `@mango/app-runtime` adapter。实例以 `instanceId` 隔离，单体和微前端使用同一业务包，开发运行配置与发布制品分离。
- CMS 真实页面通过 `useCmsApi()` 获取 host 注入的实例级客户端；`api/` 保持无 Vue 依赖，注入和缓存归 `composables/`。独立运行时延迟创建本地客户端，微前端运行时直接复用传入客户端。
- CLI full preset 和 canonical starter 使用同一 API、组件、CSS、租户和注入合同；新增业务模块默认生成 API 包与 UI 包，不把请求代码塞入 Vue 页面。

业务引用链固定为：

```text
host/runtime
  -> createMangoHttpClient(baseUrl, token, tenant, trace)
  -> registerBusinessPages(client)
  -> UI api-context
  -> createBusinessApi(HttpClient)
  -> relative endpoint
```

## 4. 自动验证结果

| 范围                            | 结果                                                                      |
| ------------------------------- | ------------------------------------------------------------------------- |
| 38 workspace / 29 package build | 全部生产构建通过                                                          |
| 架构图                          | 38/38 metadata；error 0；存量 exception 26、SCC 3                         |
| 边界合同                        | 新增违规 0；存量由 21 降至 19                                             |
| 组件合同                        | 18 registry；195 个公开 Vue export 全覆盖；新增 legacy 0                  |
| HTTP adapter                    | 12 个 mock 合同 + 3 个真实 Axios/本地 HTTP 集成测试通过                   |
| Wujie instance runtime          | 4 个配置身份、重复身份拒绝和精确 destroy 隔离测试通过                     |
| admin-shell runtime             | 46 个单元/合同测试通过                                                    |
| CMS 请求边界                    | 3 个双实例 Token/租户头隔离、真实 Axios、本地 HTTP 和取消合同测试通过     |
| 质量 checker                    | 59 个正向、反向和 fail-closed fixture 通过                                |
| package consumer                | 29 包 pack；独立安装；27 个公开类型合同；vue-tsc 与 production build 通过 |
| release rollout                 | 健康样本 `promote`；越线样本 `rollback`                                   |
| CLI/starter                     | full/custom/add/module/PMO、canonical template 和 19 个 CLI Node 测试通过 |

较早候选曾使用后端 `18001` 和 Chromium 执行 `runtime-composition.spec.ts`，6/6 通过；但此后实例身份和 CMS runtime 请求链发生了功能修改，因此该结果不作为当前提交的浏览器签名证据。当前提交已由 runtime/CMS 单测、全部生产构建、tarball 消费和封闭最小 Shell 覆盖；真实 monolith/Wujie 浏览器复验明确保留为生产毕业条件，避免用旧结果冒充当前结果。

## 5. 封闭业务开发环境

CLI 从本次打出的 `@mango/cli@1.0.84` 创建 full preset，再生成 `orders-api` 与 `orders` 业务包。29 个 Mango 依赖全部重写到本地 tarball，未复制或链接 Mango 源码。

封闭容器使用 `--network none`、`--cap-drop ALL`、`no-new-privileges`、独立 HOME 和离线 pnpm store。最终报告：

| 断言                                    | 结果                                          |
| --------------------------------------- | --------------------------------------------- |
| offline frozen install                  | 524/524 复用，下载 0，2.04 s                  |
| DNS / HTTPS canary                      | `EAI_AGAIN` / `ENETUNREACH`，均被网络层阻断   |
| 成功外部连接                            | 0                                             |
| workspace/source/宿主路径泄漏           | 0                                             |
| format / ESLint / Stylelint / typecheck | 全部零错误通过                                |
| 业务单测                                | 1 file / 2 tests 通过                         |
| production build                        | 独立 build 与聚合 check 均通过                |
| workspace 与最小 shell                  | 初始化成功；HTTP 200；831 bytes；进程正常停止 |

封闭工程使用 workspace `mango_001`、前端端口 `30001` 和隔离数据库名 `mango_dev_frontend_standards_business_lab_001`。验证结束后没有残留监听进程或候选 Docker volume。

## 6. 当前代码质量

门禁当前采用“存量精确棘轮、修改和新增代码严格阻断”模式，不是零债务模式：

| 指标                     | 当前值 | 已登记上限 | 趋势    |
| ------------------------ | -----: | ---------: | ------- |
| ESLint fatal             |      0 |          0 | 达标    |
| ESLint error             |    232 |        232 | 持平    |
| ESLint warning           |    903 |        904 | 减少 1  |
| Prettier 不一致文件      |    571 |        589 | 减少 18 |
| Stylelint error          |    935 |        935 | 持平    |
| typecheck 失败 workspace |     25 |         25 | 持平    |
| TypeScript diagnostics   |    787 |        789 | 减少 2  |
| 前端边界 identity        |     19 |         21 | 减少 2  |

19 个边界存量由 API 5、CSS 1、页面/组件分层 6、微前端厂商引用 7 组成。组件侧仍有 195 个 legacy export，当前完成新 C4 分类的组件数为 0；架构仍有 26 个例外和 3 个历史 SCC。

性能和依赖债务也未清零：主应用最大 JS 为 2.25 MB（gzip 675 KB）；业务消费者聚合块为 2.69 MB（gzip 866 KB）；full Business Lab 聚合块为 5.70 MB（gzip 1.83 MB），CSS 约 850 KB。构建仍有 Sass `@import` 弃用、循环 manual chunk、VueUse annotation 和超大 chunk 警告；依赖树仍包含停止维护的 `vue-i18n@9.2.2` 及若干 deprecated 间接依赖。

因此当前质量结论是：新增代码治理和可消费制品合同已达到本地生产候选要求，历史静态质量与性能债务尚未达到零债务毕业要求。

## 7. 灰度与回退

机器合同固定流量阶段为 internal 0% -> canary 5% -> limited 25% -> general 100%。每阶段检查 JavaScript 错误率、API 5xx 增量、刷新失败率、微应用 mount 失败率、白屏和消费者门禁。

本地演练已证明健康输入返回 `promote`，`microfrontendMountFailureRatePercent=0.2` 超过 `0.1` 阈值时返回 `rollback`。这只是决策器与回退矩阵验证，不是假装真实生产流量已执行。

回退顺序为：停止放量；runtime registry/entry 恢复稳定制品；29 包精确 pin 回上一稳定锁；移除旧批次不存在的新包；保留不可变稳定资产并清入口缓存；重跑 full gate、单体和 Wujie smoke。本批没有数据库迁移，不执行数据回退。

## 8. 生产毕业剩余条件

只有以下事实全部出现，状态才能改为 `PRODUCTION_GRADUATED`：

1. 当前分支推送后远端 required check 全部通过并合并；本任务不要求人工会签。
2. 29 个 npm 精确版本从 Nexus hosted 发布后，再从 group 仓逐包回读并在干净消费者安装。
3. 授权部署环境执行真实 monolith 与 Wujie 浏览器 smoke，验证登录、租户、请求取消、刷新、样式、重复挂载和卸载副作用。
4. 真实 5% / 25% 灰度满足观察窗，并至少完成一次故障注入回退；恢复后指标、版本锁和 full gate 正常。

在获得上述外部授权和证据前，本分支应保持 `PRODUCTION_CANDIDATE_LOCAL`，不得使用“已投产”或“生产毕业”表述。
