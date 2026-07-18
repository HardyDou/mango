# 前端组件契约登记交付证据（2026-07-19）

## 1. 结论

- PR-0E 已为现有公开 Vue 组件建立机器可读契约登记，18 个组件 package、195 个公开 Vue export 全部完成精确分类，覆盖率为 100%。
- 当前 195 个公开组件均按存量 `legacy` 登记，C3（平台公共组件）和 C4（可发布业务组件）毕业数均为 0；本次没有用“已登记”替代“已毕业”。
- 统一根门禁在固定 Node/pnpm 容器中退出码为 0，组件登记 checker、反向 fixture、全 workspace 测试、构建、package export 和独立消费者验证均通过。
- PR-0E 达到阶段提交条件，但 Mango 前端整体仍存在静态质量、性能、依赖、浏览器运行和真实部署缺口，结论仍为“未生产毕业”。

## 2. 固定环境

| 项目 | 实际值 |
| --- | --- |
| 容器镜像 | `mango/frontend-quality:node22-pnpm11.14` |
| 镜像 SHA-256 | `2a04ce0242088af26fd0b147318842ae55ef09bcc2f917126347ff3ac0d2cf30` |
| Node | 22.23.1 |
| pnpm | 11.14.0 |
| 组件登记 Owner | `Frontend Standards Owner` |
| 消费 registry | `http://nexus.inner.yunxinbaokeji.com/repository/npm-group/` |

最终 inventory SHA-256 为 `df88657a991ab9c57867fe2552204014c286ed9e408dfb3f88459cf32b624955`，架构报告 SHA-256 为 `757ffa2705848fedbf04005e19ec17b46541b209003ce5d2771c82adf95d090e`。

## 3. 登记结果

| 指标 | 结果 |
| --- | ---: |
| workspace | 37（9 apps、28 packages） |
| 前端源码文件 | 745 |
| 组件候选 | 257 |
| 组件契约登记文件 | 18 |
| 公开 Vue export | 195 |
| 已分类公开 Vue export | 195 |
| 登记覆盖率 | 100% |
| C3 已毕业 | 0 |
| C4 已毕业 | 0 |
| legacy 存量 | 195 |

checker 以 package `exports` 为真实公开面，要求登记项与公开 export 一一对应，禁止漏登、重复、陈旧路径和模糊通配分类。组件源码 export 必须能映射到 `mangoArchitecture.sourceExports`，样式入口必须能映射到 `mangoArchitecture.nonCodeExports`。

C3/C4 的四轴合同为强制字段：API、style、test、distribution/mode。C3 只允许私有 workspace 分发；C4 必须具备 npm 分发合同，且不得把管理端壳层登记为可发布业务组件。文档、测试、样式和消费模式任一不完整时均不得从 `legacy` 晋级。

相对 `origin/main` 的基线只允许 `legacy` 数量持平或下降，禁止增加。本分支是主线尚无组件登记文件时的首次引导，因此 checker 明确输出 `BOOTSTRAP`；合并后后续变更进入正常 ratchet。

## 4. 自动验证

| 门禁 | 结果 |
| --- | --- |
| 根级 `pnpm check` | 退出码 0 |
| 组件契约 checker fixture | 6/6 通过 |
| 全部质量门禁 fixture | 35/35 通过 |
| workspace 单元与合同测试 | 399/399 通过 |
| 本批合计测试 | 434/434 通过 |
| 37 workspace production build | 通过 |
| package exports / tarball 边界 | 通过 |
| 独立业务消费者 `vue-tsc` | 通过 |
| 独立业务消费者 Vite production build | 通过，2448 modules |

独立消费者由 CLI 在临时目录生成，不加入 Mango workspace。28 个 Mango package 使用本地 tarball 安装，26 个公开类型合同进入 `vue-tsc`；通过 Nexus `npm-group` 解析第三方依赖后，类型检查和生产构建均通过。

另执行了一次新隔离卷、Docker `--network none` 的根级检查。静态、测试、构建与 pack 阶段均可执行，但生成消费者仍按默认公共 registry 解析元数据，安装阶段失败。该尝试不计为通过，也不证明全新业务环境可离线重建；预热 store、冻结解析结果、网络 canary 和独立 full-preset sandbox 属于 PR-0F Business Lab 的验收范围。

## 5. 当前质量与阻断项

| 指标 | 当前值 | 判定 |
| --- | ---: | --- |
| ESLint fatal | 0 | 通过 |
| ESLint error / warning | 232 / 903 | ratchet 通过，strict 未通过 |
| Prettier 不一致文件 | 586 | ratchet 通过，strict 未通过 |
| Stylelint error / warning | 935 / 0 | ratchet 通过，strict 未通过 |
| typecheck 失败 workspace | 25/32 | ratchet 通过，strict 未通过 |
| TypeScript diagnostics | 784 | ratchet 通过，strict 未通过 |

消费者构建最大的 `mango-platform` chunk 为 2642.30 kB（gzip 851.53 kB）。同时仍报告 Sass `@import` 弃用、PURE annotation、动态/静态 import 混用、循环 manual chunk、空 chunk 和多个 deprecated dependency；生成消费者仍使用已停止维护的 vue-i18n 9.2.2。

本批没有改变公开 API、组件运行行为、CSS 加载顺序或部署行为。它只把当前公开组件事实纳入可审计门禁，为后续按包补齐 C3/C4 四轴合同和清理存量提供机械边界。

## 6. 阶段判定

PR-0E：`READY`，可以在当前任务分支提交。

整体生产毕业：`NOT READY`。至少还需完成 PR-0F full-preset Business Lab、严格静态债务清理、真实浏览器与后端联调、单体/微前端双模式、真实 Nexus 发布候选、灰度部署和回滚演练，才能进入最终生产毕业判定。
