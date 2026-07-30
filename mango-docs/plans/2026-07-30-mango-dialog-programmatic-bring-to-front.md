# 标准交付记录

> MangoDialog 编程式置顶

## 1. 元数据

- 任务 ID：MANGO-DIALOG-BRING-TO-FRONT-20260730
- 交付模式：STANDARD
- 需求影响：L2 - 为 `@mango/common` 新增公共 Vue 组件实例契约 `MangoDialogExpose`。
- 方案风险：L1 - 复用现有层级算法，仅增加生命周期安全包装与类型导出。
- 最终风险：L2
- 工作区决策：CREATE；`codex/mango-dialog-bring-to-front` / `D:\Project\mango-worktrees\mango-dialog-bring-to-front`

## 2. 目标与范围

- 目标：父组件可通过类型安全的组件 ref 调用 `bringToFront()`，在再次点击已打开资源时把对应 `MangoDialog` 提升到其它实例之上。
- 成功条件：双实例可往返置顶；关闭中、已关闭和销毁后的调用不抛异常；打开与点击自动置顶及现有窗口交互保持兼容。
- 处理范围：`MangoDialog` 公开实例类型、组件实现、组件测试、真实包消费者类型检查、Common README 和能力地图。
- 不处理范围：不修改业务组件，不维护业务侧全局 z-index，不改变 Element Plus 其它弹框，不在本功能 PR 中升级或发布 npm 版本。

## 3. 可观察系统要求

| ID    | 参与者或入口             | 输入或前置条件                       | 预期行为                                                     | 失败语义                                   | 验收标准                                           |
| ----- | ------------------------ | ------------------------------------ | ------------------------------------------------------------ | ------------------------------------------ | -------------------------------------------------- |
| AR-01 | TypeScript 消费者        | 从 `@mango/common` 导入公开类型      | 可声明 `ref<MangoDialogExpose \| null>` 并调用公开方法       | 类型未导出或声明调用报错                   | 真实 tarball 消费者 `vue-tsc` 通过                 |
| AR-02 | 同时打开两个 MangoDialog | 两个实例均完成 `open`                | 初始层级不同，调用任一实例后其层级高于另一个，并可往返切换   | 方法无效或层级未超过其它实例               | 双实例自动化测试断言每次调用后的 z-index           |
| AR-03 | 已关闭或销毁实例的调用方 | 未打开、关闭中、已关闭或持有旧引用   | 调用安全忽略，不修改层级且不抛异常                           | 抛错、重新激活或错误提升已关实例           | 组件生命周期自动化测试覆盖全部状态                 |
| AR-04 | 现有 MangoDialog 消费者  | 使用打开、点击、拖拽、缩放等既有能力 | 按下标题、内容或 footer 任意区域均置顶，内部控件功能不受影响 | 仅顶部有效、内部控件事件失效或既有交互回归 | 全区域 pointerdown 与内部按钮 click 自动化测试通过 |

## 4. 技术决定

| ID    | 对应要求 | 接口/生命周期/兼容性决定                                                                                                        | 影响路径                                                   | 回滚方式                       |
| ----- | -------- | ------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- | ------------------------------ |
| TD-01 | AR-01    | 导出 `MangoDialogExpose`，以 `defineExpose<MangoDialogExpose>` 暴露无参数、无返回值的 `bringToFront()`                          | `MangoDialog/types.ts`、组件入口、`@mango/common` 包根入口 | 移除公开类型及 expose          |
| TD-02 | AR-02    | 公开方法直接复用 `useDialogWindow()` 已有 `bringToFront`，继续使用 Element Plus 动态 z-index 分配                               | `MangoDialog/index.vue`                                    | 移除公开包装，保留内部自动置顶 |
| TD-03 | AR-03    | 以 `modelValue` 及 closing/closed/unmounted 状态保护公开调用；初始即显示时不依赖 `open` 事件时序，关闭开始后立即失活            | `MangoDialog/index.vue`                                    | 移除生命周期守卫               |
| TD-04 | AR-04    | 内容与 footer 在捕获阶段仅申请新层级，不阻止默认行为或事件传播；标题、拖拽和缩放继续复用既有交互入口，不新增 DOM 查询或固定层级 | `MangoDialog/index.vue`、既有测试                          | 仅回滚本次新增契约与测试       |
| TD-05 | AR-04    | Common 的 Vite/Vitest 联合配置从 `vitest/config` 获取 `defineConfig`，使锁定工具链能识别 `test` 字段                            | `mango-ui/packages/common/vite.config.ts`                  | 恢复原导入                     |

## 5. 实施清单

| ID    | 对应决定    | 顺序 | 改动路径                                                                                         | 完成条件                                          |
| ----- | ----------- | ---- | ------------------------------------------------------------------------------------------------ | ------------------------------------------------- |
| IM-01 | TD-01~TD-03 | 1    | `mango-ui/packages/common/components/MangoDialog/{types.ts,index.ts,index.vue}`、包根 `index.ts` | 公开类型与安全实例方法可消费                      |
| IM-02 | AR-02~AR-04 | 2    | `MangoDialog/__tests__/MangoDialog.spec.ts`                                                      | 层级往返、全区域点击、内部控件及关闭/销毁安全通过 |
| IM-03 | AR-01       | 3    | `mango-ui/scripts/check-package-consumer-typecheck.mjs`                                          | 真实打包消费者可声明并调用实例                    |
| IM-04 | TD-01~TD-04 | 4    | Common README、能力地图                                                                          | 公共能力、边界和示例同步                          |
| IM-05 | TD-05       | 5    | `mango-ui/packages/common/vite.config.ts`                                                        | Common 定向 `vue-tsc` 可执行                      |

## 6. 验收映射与结果

| 要求 ID      | 验证方式              | 命令或步骤                                                                                               | 结果 | 证据                                                                                                         |
| ------------ | --------------------- | -------------------------------------------------------------------------------------------------------- | ---- | ------------------------------------------------------------------------------------------------------------ |
| AR-02~AR-04  | M10 Common 单测       | `pnpm -C mango-ui --filter @mango/common test`                                                           | 通过 | 22 个测试文件、287 条用例通过，包含双实例层级往返、全区域置顶、内部按钮事件、关闭/销毁安全及既有拖拽缩放回归 |
| AR-01、AR-04 | M09 类型与构建        | Common `vue-tsc`、`pnpm -C mango-ui --filter @mango/common build`                                        | 通过 | 定向 `vue-tsc` 无诊断；Vite 构建及类型声明生成通过，根声明导出 `MangoDialogExpose`                           |
| AR-04        | M09 全 workspace 构建 | `pnpm -C mango-ui build`                                                                                 | 通过 | 38 个 workspace 项目全部完成构建；存在既有 chunk 体积 warning，无构建错误                                    |
| AR-01        | M09 包导出与消费者    | `pnpm -C mango-ui package-exports:check`、`pnpm -C mango-ui package-consumer:typecheck -- --reuse-build` | 通过 | 包导出完整；29 个本地 tarball 临时消费者 `vue-tsc` 与生产构建通过                                            |
| AR-01~AR-04  | PMO 测试质量门禁      | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`                                         | 通过 | `Test quality PASS: 1 file(s)`                                                                               |
| TD-04        | 文档与变更静态检查    | README 审计、组件契约检查、定向 ESLint、`git diff --check`                                               | 通过 | 两项 README 审计、195 个公开 Vue 导出契约、ESLint 0 error（测试 stub 5 warning）及 diff 检查通过             |

## 7. 例外与剩余风险

- 浏览器手工交互验证不作为本次公共实例 API 的独立实现项；拖拽、缩放和点击置顶由既有组件回归测试覆盖。
- 版本策略：本功能 PR 只交付源码、公开类型、测试与能力说明，不修改 `@mango/common` 版本或 CLI release lock；npm 升版与发布留到独立、明确授权的 release batch。
- 用户已授权本任务 commit、push 和创建 PR；未授权 npm 发布，本任务不执行发布。
- 全 workspace 和真实消费者生产构建保留既有大 chunk、循环 chunk 与空 editor chunk warning；本次未改变打包分块策略，未观察到新增构建错误。
