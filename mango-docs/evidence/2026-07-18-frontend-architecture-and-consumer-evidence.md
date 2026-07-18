# 前端架构图与发布消费交付证据（2026-07-18）

## 1. 验收结论

- PR-0D 已把 37 个前端 workspace 的角色、层级、领域、Owner、源码入口和非代码导出登记为机器可读事实，元数据覆盖率为 100%。
- 根级 `pnpm check` 已在 Node 22.23.1、pnpm 11.14.0、Docker `--network none` 的封闭副本中连续执行完成，退出码为 0。
- 发布消费验证不再止于声明文件检查：28 个公开 Mango package 从真实本地 tarball 安装到独立 custom 业务项目，26 个带类型合同的 package 均由 `vue-tsc` 解析，所选业务组合同时通过 Vite production build。
- PR-0D 的架构与发布消费边界已达到阶段交付条件。静态门禁当前按汇总指标阻止债务增长，并不代表每条诊断身份已固化；Mango 前端整体也未达到 strict 零诊断，不能把本结论解释为整体已完成生产准入。

## 2. 执行环境

| 项目         | 实际值                                                                                         |
| ------------ | ---------------------------------------------------------------------------------------------- |
| 宿主         | Docker `mango/frontend-quality:node22`                                                         |
| Node         | 22.23.1                                                                                        |
| pnpm         | 11.14.0                                                                                        |
| 网络         | 预置 Corepack、依赖内容及 registry 元数据缓存后，最终根门禁和消费者验证均使用 `--network none` |
| 根安装       | frozen lockfile；既有 store 离线复用，下载 0                                                   |
| 业务消费者   | CLI 生成的独立 custom monolith 项目；不属于 Mango pnpm workspace                               |
| 源码边界     | 只安装 28 个 `pnpm pack` tarball，不挂载 Mango package 源码或源码 alias                        |
| 临时过程日志 | `/tmp/mango-pr0d-final.cGWEfb/repo/.runtime/final-check-v5.log`，不纳入版本控制                |

## 3. 架构图结果

| 指标                 |    结果 |
| -------------------- | ------: |
| workspace / metadata | 37 / 37 |
| manifest edges       |     161 |
| source runtime edges |     108 |
| contract edges       |      38 |
| tooling edges        |       1 |
| 精确方向例外         |      26 |
| 存量 SCC             |       3 |
| 新增架构错误         |       0 |

架构报告 SHA-256 为 `757ffa2705848fedbf04005e19ec17b46541b209003ce5d2771c82adf95d090e`。三条 SCC 记录描述同一组 `@mango/admin-pages`、`@mango/file`、`@mango/system` 的不同观察面：

| 图             | SCC SHA-256                                                        |
| -------------- | ------------------------------------------------------------------ |
| manifest       | `58bd0316a929807ce7e5f531783237f908da0eafaca63328680c51a1bba2de6e` |
| source-runtime | `70c37d6be5cdee6a4b1410ca10a8623e8515302a2b48527d42f07eb1b2e48419` |
| combined       | `1d20a9de3f7982999c5b10f7cb8d53c26ba8990ec92682e108f89a462bac26e3` |

例外均要求 Owner、ADR-FE-005 依据、证据路径和 2027-01-18 到期日。`sourceExports` 的 4 个存量 wildcard 同样校验严格日期和到期状态，并以 `origin/main` 的既有 package + export key 为上限，禁止新增 wildcard、扩大 source pattern 或延长期限。checker 对零输入、未知角色、坏 export、反向依赖、package 指向 app、runtime-only devDependency、tooling 环、非字面量动态 import、报告非确定性和 SCC 扩张均提供反向测试。

## 4. 发布后业务开发引用场景

业务开发者只应消费 package 的显式 `exports`：

- 运行代码使用根入口或公开 subpath，例如 `@mango/common`、`@mango/file`、`@mango/link-openapi`，不得引用 `src/**` 或 Mango 仓库 alias。
- 页面或组件包的运行 CSS 使用唯一公开入口 `@mango/<package>/style.css`；纯 API 或类型包不虚构样式入口。
- Vue、Element Plus、Pinia、Router、i18n 等宿主依赖按 package 的 dependency/peer contract 安装；业务项目不依赖 Mango 源码模式或私有 Vite alias 才能构建。
- 业务包 API 放在 package 的 `src/api`、`src/services`、`src/composables` 等职责目录，通过显式 export 暴露；Vue 页面只调用 service/composable，不内联跨页面请求实现。
- 单体与微应用都消费同一 package 合同；部署形态差异由 runtime config 和 app adapter 处理，不在业务页面复制两套 API/CSS。

最终消费者验证完成以下闭环：

1. 构建全部公开 package。
2. 对每个 package 执行真实 pack，检查运行库不泄漏源码、所有条件 exports/types/style 文件存在，并验证通配符 JS 与类型声明的子路径集合一致；CLI 按其脚手架合同保留 `src` 和 templates。
3. 用 CLI 在空目录生成独立业务前端。
4. 把 28 个 Mango 依赖全部替换为 tarball 坐标并执行离线冷安装；解析 349 个依赖，复用 298 个，网络下载 0。
5. 为 26 个带类型合同的 Mango package 生成公开入口 smoke，并执行 `vue-tsc -p tsconfig.app.json --noEmit`。
6. 执行业务项目 `pnpm run build`，处理 2448 个模块并生成 production dist，最终复跑耗时 23.35 秒。

该流程发现并修复了四类真实发布问题：`@mango/admin-shell/style.css` 指向不存在的产物；`@mango/common` 的 realtime 通配符缺少 `index.d.ts`；生成项目声明 pnpm 却调用 npm；`pnpm exec vite -- build` 实际启动开发服务器而非生产构建。公开样式入口现指向 `./dist/admin-shell.css`，声明生成器补齐通配符类型对称性，生成模板统一调用 `pnpm exec vite build`。`package-exports:check` 同时递归检查相对 stylesheet import，后续同类问题会在 pack 或消费者构建前失败。

## 5. 自动验证结果

| 门禁                                      | 结果                                          |
| ----------------------------------------- | --------------------------------------------- |
| 37 workspace production build             | 通过                                          |
| 架构图与 ratchet fixture                  | 18/18 通过                                    |
| inventory、发布边界与静态 ratchet fixture | 11/11 通过                                    |
| workspace 单元与合同测试                  | 399/399 通过                                  |
| CLI 测试                                  | 19/19 通过，包含无 `ps` 真实进程 restart 回归 |
| 总测试                                    | 428/428 通过                                  |
| package exports                           | 通过                                          |
| 28 tarball 安装、26 个公开类型合同解析    | 通过                                          |
| custom 业务组合 production build          | 通过                                          |
| Admin 样式聚合                            | 18 个 package style export 通过               |
| Admin module style governance             | 12 个 official module 通过                    |

最终 inventory 为 37 个 workspace、745 个源码文件、257 个组件候选、195 个公开 Vue export，公开 export inventory 覆盖 100%；inventory SHA-256 为 `df88657a991ab9c57867fe2552204014c286ed9e408dfb3f88459cf32b624955`。

## 6. 当前前端代码质量

| 指标                      |    当前值 | 投产判定                        |
| ------------------------- | --------: | ------------------------------- |
| ESLint fatal              |         0 | 通过                            |
| ESLint error / warning    | 232 / 903 | ratchet 通过，strict 未通过     |
| Prettier 不一致文件       |       586 | ratchet 通过，strict 未通过     |
| Stylelint parse error     |         0 | 通过                            |
| Stylelint error / warning |   935 / 0 | ratchet 通过，strict 未通过     |
| typecheck 失败 workspace  |     25/32 | ratchet 通过，strict 未通过     |
| TypeScript diagnostics    |       784 | 比上一证据减少 5，strict 未通过 |

仍存在 Sass `@import` 弃用、VueUse PURE 注释、动态/静态混合 import、manual chunk 环和 500KB/1200KB/1500KB 以上 chunk 警告。消费者中最大的 `mango-platform` chunk 为 2642.30 kB（gzip 851.53 kB）。它们当前是 PR-0D 已知非阻断债务，应在后续样式迁移、依赖和性能批次中按指标清理。

## 7. 范围外与剩余风险

- 本证据证明本地发布候选 tarball 的静态、类型和 production build 消费合同，不包含真实 registry 写入；发布、推广和生产部署仍需要独立授权。
- 本次消费者是 custom monolith 项目；full preset Business Lab、真实后端登录、浏览器 E2E、Wujie 多实例、微应用独立部署、灰度和回滚仍按设计中的 PR-0F、PR-1J 和 Phase 3 执行。
- `@mango/admin-pages`、`@mango/file`、`@mango/system` 存量环尚未拆除，只能在精确 SCC 内不扩张；到期前必须完成 Phase 2 解环或登记新的架构决定。
- 生成消费者仍使用停止维护的 vue-i18n 9.2.2，并报告部分 deprecated 间接依赖；升级属于独立兼容批次。

## 8. 业务开发交接

| 输出对象                 | 交接内容                                                    | 入口                                                    | 失败处理                                         | 状态                            |
| ------------------------ | ----------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------ | ------------------------------- |
| Mango package 维护者     | metadata、exports、style 和边界门禁                         | `pnpm architecture:check`、`pnpm package-exports:check` | 禁止新增例外；修复导出或依赖方向                 | PR-0D READY                     |
| 业务前端开发者           | 只从公开 package/subpath/style 入口引用 API、组件和 CSS     | package README、能力地图                                | 深导入或构建失败先核对 exports，不回退源码 alias | 阶段可用；待 PR-0F Business Lab |
| 发布执行者               | 发布前以 tarball 运行 consumer typecheck + production build | `pnpm package-consumer:typecheck -- --reuse-build`      | 任一安装、类型或构建失败即阻断发布               | 门禁可用；真实发布未授权        |
| Frontend Standards Owner | 管理 26 条方向例外和 3 条 SCC 的到期清理                    | `architecture-exceptions.json`、ADR-FE-005              | 例外过期或环扩张立即失败                         | PR-0D READY                     |
