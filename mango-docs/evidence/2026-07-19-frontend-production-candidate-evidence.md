# Mango 前端规范本地验证证据（2026-07-19）

## 1. 目的与结论

本记录证明 Vue 3、Element Plus、Vite 前端规范已经在 Mango 源码、可发布 npm tarball、CLI 生成业务项目和真实浏览器入口中落地，并给出业务开发者可复核的采用边界。规范采用单拥有者模式：机器门禁给出通过或阻断，专家复核提供缺陷证据，不增加独立人工审批链。

当前状态：`STANDARD_VALIDATED_LOCAL`。

该状态只表示本地工程基线验证完成，不表示 npm 包已发布、业务应用已部署或生产流量已经切换。`STANDARD_READY_FOR_ADOPTION` 仍需要远端 required check 与一个真实业务项目试点；`STANDARD_ENFORCED` 仍需要目标仓库启用强制门禁。

## 2. 精确候选身份

| 项目                       | 实际值                                                                    |
| -------------------------- | ------------------------------------------------------------------------- |
| 集成候选提交               | `7e4271002de7a3848e7fbb7c4d9c7796d3e7c79f`                                |
| 集成候选 Git tree          | `1cdd1a74dac758f1efb242d491ec5660dbe91fbf`                                |
| 已同步 origin/main         | `1694a7421`                                                               |
| 功能候选提交               | `47d257ed1c01c41df59c6854b5bb2cc5fd1874a9`                                |
| 功能候选 Git tree          | `cf0f6f96270f64c4a1d3da86518ffc0573e7e2f3`                                |
| 源码模式                   | `git-archive-exact-commit`                                                |
| 分支                       | `docs/frontend-standards-plan`                                            |
| 一键入口                   | `pnpm -C mango-ui check:standards-candidate`                              |
| 固定镜像                   | `mango/frontend-quality:node22-pnpm11.14`                                 |
| 镜像 identity              | `sha256:2a04ce0242088af26fd0b147318842ae55ef09bcc2f917126347ff3ac0d2cf30` |
| Node / pnpm                | `22.23.1` / `11.14.0`                                                     |
| 平台                       | `linux/arm64`                                                             |
| 候选 tarball               | 29 个精确版本                                                             |
| preparation report SHA-256 | `7756d2ebe3cc065eada2cf9aac214fb149625061228a7a2ba155c03d876c5c38`        |
| sealed report SHA-256      | `b8e7e953e95c4123f2b1810b0c223334fcf3e0aca447bb9206b2c6456f2d188c`        |
| browser contract SHA-256   | `74410b48a0679da01f86241a5c334dbd3f2414df202d5381b9d80035c4b70947`        |
| Playwright report SHA-256  | `f93db12cc0c60541193a76dec31161d2601ede6d091d3d96f384cef86f363828`        |
| 业务 lockfile SHA-256      | `e3c49cd810ce56fc395c1f951a4a7aa1e8df724346cb9386fa685e8e74e904b2`        |

过程报告位于忽略目录 `.runtime/frontend-quality/business-lab/`。准备报告逐包记录名称、版本和 tarball SHA-256；封闭报告引用准备报告摘要，并记录镜像、锁文件、命令、耗时、网络 canary、workspace 和最小 Shell 结果。

## 3. 已落地边界

- 根级工具链固定为 ESLint 10、Prettier 3、Stylelint 17、TypeScript 5.9、vue-tsc 3、Vite 7、Vitest 4 和 Playwright 1.61，版本唯一性由机器检查。
- 业务 API 位于 `packages/<module>-api`，只依赖中立 `HttpClient` 合同；Vue、Element Plus、Axios、路由和环境变量不得进入 API 包。业务 UI、页面、组件和显式 `style.css` 位于业务 UI 包。
- `@mango/http-client` 是 Axios 适配器。host 按运行时实例创建并注入客户端，页面不创建 Axios 实例；取消使用 `AbortSignal`，错误统一为 `HttpError`。
- 当前微前端实现为 Wujie，厂商 API 收口到 `@mango/app-runtime` adapter。业务层 Wujie 厂商引用已降为 0；单体和微前端复用同一业务包，开发配置不进入生产 dist。
- CMS 页面通过 host 注入的实例级客户端请求；同应用多实例缺少 `instanceId` 且身份存在歧义时 fail-closed。CMS 列表请求支持取消和过期结果隔离；缺少必需站点时展示空态，不再发送空 `siteId` 请求。
- CLI full preset 与 canonical starter 使用同一 API、组件、CSS、租户和注入合同；新增业务模块生成独立 `orders-api` 与 `orders`，请求实现不进入 Vue 页面。
- Playwright 业务 spec 禁止直接使用 Element Plus 内部 `.el-*`、位置式 `nth()`、固定等待 `waitForTimeout()` 和 `force: true`；框架细节集中在受测 adapter，业务断言使用 role、label 和稳定语义。
- `mango-release` Skill 只处理真实不可变制品、tag 和 GitHub Release；前端规范采用、lint/typecheck、目录治理与流量发布均为非触发场景。对应评测清单已增至 138 条并通过结构检查。

业务引用链为：

```text
host/runtime
  -> createMangoHttpClient(baseUrl, token, tenant, trace)
  -> app.provide(MANGO_HTTP_CLIENT_KEY, client)
  -> UI composable injects the current app client
  -> per-client WeakMap cache
  -> createBusinessApi(HttpClient)
  -> relative endpoint

registerBusinessPages()
  -> route/component metadata only; carries no client state
```

## 4. Mango 自身代码验证

| 范围                      | 结果                                                                                      |
| ------------------------- | ----------------------------------------------------------------------------------------- |
| 38 workspace / 29 package | 全部生产构建通过                                                                          |
| 架构图                    | 38/38 metadata；error 0；存量 exception 26、SCC 3                                         |
| 前端边界                  | 新增违规 0；存量 12：API 5、CSS 1、分层 6、微前端厂商引用 0                               |
| 组件合同                  | 18 registry；195 个公开 Vue export 全覆盖；新增 legacy 0                                  |
| 运行配置                  | 75 个 dist 文件检查通过，无开发 runtime config 泄漏                                       |
| CMS 单测                  | 2 files / 4 tests 通过                                                                    |
| PMO / Skill               | quick validator 通过；138 条 Skill 评测清单通过；业务 baseline 138 文件机械同步并回查通过 |
| package consumer          | 29 包 pack；干净安装；公开类型检查与 production build 通过                                |
| 质量门禁单测              | 73/73 通过；覆盖静态棘轮、E2E 选择器治理、边界、组件、运行配置和候选身份                  |
| CLI / starter             | CLI 21/21；full project、add module、PMO 安装与业务包生成通过                             |

固定容器同时执行了全仓质量 checker 单测和所有声明了 `test` 的 workspace 测试。历史债务由精确 identity 棘轮约束；基线只能来自 `origin/main` 或既定锚点，任务分支后续提交不能抬高基线，本次结果没有通过修改基线放宽。集成候选先合并 `origin/main@1694a7421`，再从提交 `7e4271002` 的精确 Git archive 重新执行一键入口并通过，证明结果不依赖合并前的旧主分支快照。

## 5. 真实浏览器与 Wujie 验证

项目 Mango CLI 使用独立 workspace `mango_001`、后端 `18001` 和数据库 `mango_dev_mango_frontend_standards_plan_001` 启动真实 monolith 后端，健康检查 `http://127.0.0.1:18001/actuator/health` 通过。Chrome/Playwright 执行 `mango-admin-shell` 完整套件，结果为 8/8：

1. CMS 资源管理、状态流转、发布关系和站点详情；
2. 内容运营所有主要按钮及失败请求/console error 断言；
3. Wujie hybrid 下 RBAC 与业务子应用远端装载；
4. monolith 本地渲染且不加载远端应用；
5. 远端应用损坏时显示可操作错误；
6. entry 缺失时不错误回退到其它微应用；
7. 非法 runtime mode 回退本地渲染并提供诊断；
8. 微应用未授权事件由 Shell 正确处理。

Playwright JSON 报告绑定提交 `47d257ed1c01c41df59c6854b5bb2cc5fd1874a9` 和 tree `cf0f6f96270f64c4a1d3da86518ffc0573e7e2f3`，统计为 expected 8、unexpected 0、flaky 0、skipped 0。验证结束后 Playwright 关闭 Shell 及子前端，Mango CLI 停止后端。

## 6. 封闭业务开发环境

候选从精确 Git archive 构建 29 个 tarball，再从打包的 `@mango/cli@1.0.84` 创建 full preset，并生成 `orders-api` 与 `orders`。业务项目不读取 Mango workspace 源码。

封闭阶段使用 `--network none`、`--cap-drop ALL`、`no-new-privileges`、独立 HOME 和离线 pnpm store：

| 断言                                    | 结果                                                |
| --------------------------------------- | --------------------------------------------------- |
| offline frozen install                  | 524/524 复用，下载 0，2.197 s                       |
| DNS / HTTPS canary                      | `EAI_AGAIN` / `ENETUNREACH`，均被网络层阻断         |
| 成功外部连接                            | 0                                                   |
| workspace/source/宿主路径泄漏           | 0                                                   |
| format / ESLint / Stylelint / typecheck | 全部退出码 0                                        |
| 业务单测                                | 2 files / 4 tests 通过                              |
| production build / 聚合 check           | 全部退出码 0                                        |
| 最小 Shell                              | CLI 初始化、启动、HTTP 200（831 bytes）、停止均通过 |

Chrome 随后从生成项目的 Vite 服务加载 `orders` Vue 页面模块和 `orders-api` 工厂，精确校验 create、update、delete、page、detail 五个操作的方法、相对 URL、query、body 与返回结构；console error 和 request failure 均为 0。浏览器契约截图 SHA-256 为 `51b9ca7f658d83b8de0023c6aa064a8ccb1974ced3ca47a85fe73092ca38c04f`。

封闭工程使用 workspace `mango_001`、前端端口 `30001` 和数据库名 `mango_dev_frontend_standards_business_lab_001`。网络接口只有 `lo`。

候选演进中先后发现 CLI 生成的实例隔离测试触发 3 条 `vue/one-component-per-file` 告警、跨宿主机和容器的 typecheck 绝对路径 identity 不一致、浏览器阶段误触发 pnpm 依赖状态自检，以及 CMS 广告编辑用例将 Element Plus 的 teleported 下拉框关联到错误 listbox。前三项通过修正模板或门禁实现解决；最后一项通过读取当前 combobox 的 `aria-controls` 并按可见 role option 选择修复，同时把业务 spec 对框架内部选择器和脆弱等待的禁令加入机器门禁。功能候选 `47d257ed1` 完成真实后端 Playwright 8/8；同步最新主分支后，集成候选 `7e4271002` 重新完成固定容器、29 包冷消费、封闭 Business Lab 与浏览器合同验证。两轮均未通过放宽规则或修改基线绕过问题。

## 7. 验收证据台账

| 台账 ID   | 用例 ID | 页面/接口                                                    | 功能点                                         | 测试数据                                         | 关键断言                                                                           | UI/交互检查                                                         | console/network 结果                                                                     | 截图/trace/日志                                                                  | 结论 |
| --------- | ------- | ------------------------------------------------------------ | ---------------------------------------------- | ------------------------------------------------ | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | ---- |
| FE-AC-001 | TC-001  | 生成项目 `orders` 页面；`/runtime-config.json`；`orders-api` | 独立业务 API 包、UI 包和五类请求合同           | 新建订单、更新订单、`SO-1`、分页查询参数         | create/update/delete/page/detail 的方法、相对 URL、query、body 与返回结构逐项匹配  | Chrome 动态加载生成的 Vue 页面模块；合同状态页显示五类 API 断言通过 | console error 0；request failure 0；运行配置请求成功                                     | `历史验收图片已清理（可从 Git 历史恢复）`；browser contract SHA-256 `74410b...0947` | PASS |
| FE-AC-002 | TC-002  | `/cms/*` 资源管理与站点详情                                  | 资源创建、编辑、状态流转、发布关系、详情和清理 | 带唯一时间戳的站点、栏目、内容和媒体记录         | 新建结果可查询，编辑字段持久化，状态与发布关系按操作变化，末尾清理测试记录         | 列表、表单、状态动作、发布动作和详情页面逐步操作并留存终态          | Playwright status `passed`；受测 CMS 响应满足断言，未出现未预期请求失败                  | `历史验收图片已清理（可从 Git 历史恢复）`；Playwright report SHA-256 `f93db1...3828`               | PASS |
| FE-AC-003 | TC-003  | CMS 内容运营页                                               | 主要操作按钮、上传和失败可观测性               | 唯一前缀 `CMS_BUTTONS` 数据；PNG 与 MP4 测试文件 | 70 余项按钮结果均为预期；上传回显匹配；API `>=400` 与 console error 集合为空       | 搜索、筛选、新建、编辑、状态、发布、上传、预览和清理均实际触发      | console error 0；API `>=400` 0；Playwright status `passed`                               | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                                    | PASS |
| FE-AC-004 | TC-004  | hybrid runtime：menu-package、workflow、cms                  | Wujie 远端装载、RBAC 和业务内容组合            | hybrid runtime registry 与已授权菜单             | 三个目标路由标记为 `MICRO_ROUTE`，remote host 与业务内容分别匹配，目标资源实际装载 | Shell 菜单切换后逐一显示正确微应用内容，没有串用其它应用入口        | 目标 remote resource、runtime metadata 与业务 smoke 断言通过；Playwright status `passed` | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                                    | PASS |
| FE-AC-005 | TC-005  | monolith runtime                                             | 单体本地渲染与远端资源隔离                     | `runtimeMode=monolith`                           | 目标路由标记为 `LOCAL_ROUTE`，远端资源列表严格等于空数组                           | Shell 切换目标菜单后展示本地业务页面                                | remote resource 数量 0；本地 runtime metadata 断言通过；Playwright status `passed`       | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                                  | PASS |
| FE-AC-006 | TC-006  | hybrid runtime 损坏 remote entry                             | 微应用装载失败的可操作反馈                     | 指向不可装载 entry 的测试 runtime registry       | 错误信息包含目标单元、entry 与重试动作，不静默吞掉装载失败                         | 页面显示明确错误状态，重试控件可见且目标身份可识别                  | 预期 remote failure 被错误边界捕获；Playwright status `passed`                           | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                             | PASS |
| FE-AC-007 | TC-007  | hybrid runtime 缺失 entry                                    | 缺失配置诊断与错误应用隔离                     | 删除目标应用 entry 的 runtime registry           | 显示缺失 entry 诊断，且不回退或串用另一微应用                                      | 错误区展示目标应用身份与配置缺失信息                                | 未发起其它应用 entry 替代请求；Playwright status `passed`                                | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                             | PASS |
| FE-AC-008 | TC-008  | 非法 runtime mode                                            | 非法配置降级和诊断                             | 不受支持的 runtime mode 值                       | 降级到 `LOCAL_ROUTE`，诊断包含非法值，远端资源列表严格等于空数组                   | 本地业务内容继续可操作，诊断信息可定位配置问题                      | remote resource 数量 0；降级 metadata 断言通过；Playwright status `passed`               | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                              | PASS |
| FE-AC-009 | TC-009  | 微应用未授权事件与 Shell 登录页                              | 跨应用鉴权失效处理                             | 预置 `MANGO_TOKEN` 后触发 unauthorized event     | Shell 跳转登录页并删除 `MANGO_TOKEN`，未授权状态不留在业务页面                     | 事件触发后浏览器地址和登录界面同步切换                              | token 清理与目标地址断言通过；Playwright status `passed`                                 | `历史验收图片已清理（可从 Git 历史恢复）`；对应 test attachment                              | PASS |

截图目录为 `mango-docs/evidence/2026-07-19-frontend-production-candidate/`。所有 Playwright 用例均绑定第 2 节的功能候选；报告统计 expected 8、unexpected 0、flaky 0。

## 8. 当前代码质量

当前执行“存量精确棘轮、修改和新增代码严格阻断”，不是零债务：

| 指标                     | 当前值 | 结论                        |
| ------------------------ | -----: | --------------------------- |
| ESLint fatal             |      0 | 达标                        |
| ESLint error             |    232 | 存量持平                    |
| ESLint warning           |    893 | 存量持平                    |
| Prettier 检查文件        |    564 | 全部符合格式                |
| Stylelint error          |    935 | 存量持平                    |
| typecheck 失败 workspace |     25 | 存量持平                    |
| TypeScript diagnostics   |    787 | 未超过基线                  |
| 前端边界 identity        |     12 | 新增 0；Wujie 厂商引用 0    |
| legacy Vue export        |    195 | 已全量登记，C4 新分类仍为 0 |

性能债务未清零：主应用最大 JS 约 2.25 MB（gzip 675 KB），tarball 消费者聚合块约 2.69 MB（gzip 866 KB），full Business Lab 聚合块约 5.70 MB（gzip 1.83 MB），CSS 约 850 KB。Sass `@import` 弃用、循环 manual chunk、VueUse annotation、超大 chunk、`vue-i18n@9.2.2` 和若干间接弃用依赖仍是后续优化对象，但没有被误报为本次新增违规。

## 9. 采用与发布边界

规范采用按 `pilot -> affected -> repository` 扩大代码和消费者覆盖，不对应 5% / 25% 用户流量。依赖恢复矩阵验证候选包可精确 pin 到上一稳定版本，并不代表生产部署回滚。

本地 `STANDARD_VALIDATED_LOCAL` 已满足。下一状态的外部事实为：

1. 推送任务分支并由远端 required check 验证同一提交；
2. 至少一个真实业务项目按公开 API、样式和 runtime 配置完成试点；
3. 目标仓库启用规范 required check 后，才能标记 `STANDARD_ENFORCED`。

当前任务已经进入真实发布准备，但本证据仍只声明本地候选状态。Nexus 发布、tag 和 GitHub Release 必须在精确版本矩阵、目标 registry 与 tag 获得当次明确授权，并且候选通过 required check 合并到 `main` 后执行；业务部署、生产流量灰度和故障注入不属于前端代码规范发布范围。
