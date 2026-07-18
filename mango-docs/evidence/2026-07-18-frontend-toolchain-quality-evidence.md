# 前端工具链与代码质量交付证据（2026-07-18）

## 1. 结论

- PR-0C 根工具链、统一命令、存量债务棘轮、构建出口和本地制品消费者验证已具备提交条件。
- 当前分支在 Node 22.23.1、pnpm 11.14.0、Docker `--network none` 环境执行根级 `pnpm check`，退出码为 0。
- 该结果证明“存量允许保持、任何新增债务失败”的候选投产门禁可用，不代表 Mango 前端已经达到全量零诊断。
- 严格零债务投产状态仍未达到；类型、样式、格式和 ESLint 存量必须按设计的 Phase 2/3 逐包清理。

## 2. 固定环境

| 项目 | 实际值 |
| --- | --- |
| Node | 22.23.1 |
| pnpm | 11.14.0 |
| ESLint | 10.7.0 |
| Prettier | 3.9.5 |
| Stylelint | 17.14.0 |
| TypeScript | 5.9.3 |
| vue-tsc | 3.3.7 |
| Vite | 7.3.6 |
| Vitest | 4.1.10 |
| Playwright | 1.61.1 |
| 网络 | Docker `--network none`；DNS、HTTPS canary 均被拒绝 |

12 个治理工具包均只解析到一个认证版本。冻结 lockfile 的冷安装已在独立 pnpm store 中通过。

## 3. 当前代码事实

| 指标 | 实际值 |
| --- | ---: |
| workspace | 37（9 apps、28 packages） |
| 前端源码文件 | 745 |
| 组件候选 | 257 |
| 公开 Vue export | 195 |
| 公开 Vue export inventory 覆盖 | 100% |
| registrar | 35 |
| widget metadata | 19 |
| 测试文件 | 120 |
| dynamic import | 248 |

最终 inventory SHA-256 为 `409b2a0ab4398d8d9a9464b726c5fae0dd382a827c30169cb04576b366252ae8`。

## 4. 质量结果

| 门禁 | 当前结果 | 判定 |
| --- | ---: | --- |
| ESLint fatal | 0 | 通过 |
| ESLint error | 232 | ratchet 通过；strict 未通过 |
| ESLint warning | 903 | 比登记基线减少 1；strict 未通过 |
| Prettier 不一致文件 | 589 | ratchet 通过；strict 未通过 |
| Stylelint parse error | 0 | 通过 |
| Stylelint error | 935 | ratchet 通过；strict 未通过 |
| typecheck 失败 workspace | 25/32 | ratchet 通过；strict 未通过 |
| TypeScript diagnostics | 789 | ratchet 通过；strict 未通过 |

旧 ESLint 8.57.1 在相同 Mango 语料中产生 5 fatal、95 errors、12,711 warnings；最终 ESLint 10 候选为 0 fatal、232 errors、904 warnings。warning 减少约 92.9%，error 增至约 2.44 倍，均落在设计预期区间；新增 error 是缺陷规则暴露结果，已进入棘轮基线。

## 5. 自动验证

封闭根级验证顺序：

```text
quality inventory
toolchain unique-version check
ESLint / Prettier / Stylelint / vue-tsc ratchet
unit tests
admin style aggregation
37 workspace production builds
package export check
local tarball consumer typecheck
```

结果：

- 质量 runner 测试 8 个通过。
- 现有 workspace 单元/合同测试 399 个通过；合计 407 个测试通过。
- 37/37 workspace production build 通过。
- 28 个本地 Mango tarball 的 export、声明文件和 source 泄漏检查通过。
- CLI 生成独立 custom 业务消费者；断网安装解析 347 个依赖、复用 296 个、下载 0；随后 `vue-tsc` 通过。
- PMO test-quality 检查 20 个文件通过；workspace layout 检查通过；`git diff --check` 通过。

## 6. 本次发现并修复的问题

- Vite 7 与无实际顶层 await 的 `vite-plugin-top-level-await` 组合导致管理端构建失败；移除不必要插件后通过。
- 17 个 package 声明 `./style.css`，实际构建却输出包名 CSS；统一 `cssFileName: 'style'` 后 package export 检查通过。
- 生成项目未固定 pnpm，Corepack 在断网时查询 `pnpm/latest`；模板现固定 `pnpm@11.14.0`。
- 业务模板仍使用 Vite 4、vue-tsc 2 等旧范围；已对齐当前认证的 Vite 7、plugin-vue 6、vue-tsc 3、Playwright 1.61 和 Node 22 类型。
- 两个 E2E 分支使用恒真断言掩盖未知页面状态；已改为明确失败，PMO 测试质量检查通过。

## 7. 尚未毕业的投产项

- 全量 strict 静态质量仍有上述存量债务，不能宣称零诊断。
- 当前封闭消费者证明 local tarball、离线安装和类型消费边界；它是 custom preset，不替代设计中 PR-0F 的 full-preset Business Lab 毕业证据。
- 真实后端登录、浏览器 E2E、monolith/microservice、Wujie 生命周期与独立部署合同不在本次根工具链验证范围，需按 PR-0F、PR-1J 和后续批次执行。
- 构建存在 Sass `@import` 弃用、VueUse PURE 注释、动态/静态混合导入和大 chunk 警告；当前非阻断，但必须进入后续性能和依赖清债批次。
- 生成消费者仍使用已停止维护的 vue-i18n 9.2.2；运行时升级必须作为独立兼容批次验证，不与工具链提交混改。

因此，本次可提交并用于“禁止新增债务”的候选投产门禁；Mango 前端整体的严格投产结论仍为“未毕业”。
