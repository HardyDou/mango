# 详情骨架最大宽度配置标准交付记录

## 1. 元数据

- 任务 ID：detail-max-width-config-2026-10-14
- 交付模式：STANDARD
- 需求影响：L2 - 公共详情组件新增可观察的布局配置能力
- 方案风险：L1 - 仅影响组件样式、类型和文档，不改变数据、权限或路由
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：将 `MangoCollapseDetailPage` 的默认最大宽度从固定 CSS 值改为可配置属性。
- 成功条件：默认最大宽度为 `1280px`；消费方可传入 `px` 或百分比值；非法单位有明确错误；既有详情交互保持可用。
- 处理范围：详情组件 props、宽度样式、组件测试、README、changeset。
- 不处理范围：不修改旧 `MangoDetailPage`、宿主布局、业务页面和其它包的布局默认值。

## 3. 可观察系统要求

| ID     | 参与者或入口              | 输入或前置条件        | 预期行为                  | 失败语义       | 验收标准                      |
| ------ | ------------------------- | --------------------- | ------------------------- | -------------- | ----------------------------- |
| SR-001 | `MangoCollapseDetailPage` | 不传 `maxWidth`       | 根容器最大宽度为 `1280px` | 无             | 组件测试断言默认 inline style |
| SR-002 | `MangoCollapseDetailPage` | 传入 `960px` 或 `80%` | 根容器使用传入最大宽度    | 无             | 组件测试分别断言 px 和百分比  |
| SR-003 | `MangoCollapseDetailPage` | 传入无单位值          | 组件抛出明确配置错误      | 不渲染非法宽度 | 组件测试断言错误文案          |

## 4. 技术决定

| ID     | 对应要求               | 接口/数据/权限/兼容性决定                                                 | 影响路径                                                                | 回滚方式                                            |
| ------ | ---------------------- | ------------------------------------------------------------------------- | ----------------------------------------------------------------------- | --------------------------------------------------- |
| TD-001 | SR-001, SR-002         | 新增 `maxWidth?: string`，默认 `1280px`；仅接受 `px` 或百分比值           | `mango-ui/packages/detail/src/MangoCollapseDetailPage.types.ts`、`.vue` | 删除 prop、校验和 inline style，恢复组件 CSS 默认值 |
| TD-002 | SR-001, SR-002         | 通过根容器 inline style 设置 `max-width`，移动端继续由 `width: 100%` 适配 | `mango-ui/packages/detail/src/MangoCollapseDetailPage.vue`              | 回滚 TD-001                                         |
| TD-003 | SR-001, SR-002, SR-003 | 文档、测试和 patch changeset 与公共 API 同步                              | `README.md`、`src/__tests__`、`.changeset`                              | 删除对应文档和 changeset                            |

## 5. 实施清单

| ID     | 对应决定       | 顺序 | 改动路径                                                                        | 完成条件                         |
| ------ | -------------- | ---- | ------------------------------------------------------------------------------- | -------------------------------- |
| IM-001 | TD-001, TD-002 | 1    | `mango-ui/packages/detail/src/MangoCollapseDetailPage.vue`                      | 默认值、单位校验和宽度绑定完成   |
| IM-002 | TD-001         | 2    | `mango-ui/packages/detail/src/MangoCollapseDetailPage.types.ts`                 | 公共 props 类型导出完成          |
| IM-003 | TD-003         | 3    | `mango-ui/packages/detail/src/__tests__/MangoCollapseDetailPage.spec.ts`        | 默认、px、百分比和非法值覆盖完成 |
| IM-004 | TD-003         | 4    | `mango-ui/packages/detail/README.md`、`mango-ui/.changeset/detail-max-width.md` | API 文档和发布说明完成           |

## 6. 验收映射与结果

| 要求 ID                | 验证方式   | 命令或步骤                                                       | 结果                          | 证据                           |
| ---------------------- | ---------- | ---------------------------------------------------------------- | ----------------------------- | ------------------------------ |
| SR-001, SR-002, SR-003 | 组件测试   | `vitest run .../packages/detail/src/__tests__`                   | PASS，3 个测试文件、10 个用例 | 命令输出                       |
| SR-001, SR-002         | 生产构建   | `vite build --config packages/detail/vite.config.ts` + 类型生成  | PASS                          | `@mango/detail` dist 构建输出  |
| SR-001, SR-002         | 类型检查   | `vue-tsc --noEmit -p packages/detail/tsconfig.json`              | PASS                          | 命令退出码 0                   |
| SR-001, SR-002         | 静态检查   | ESLint、Stylelint、Prettier check                                | PASS                          | 命令退出码 0                   |
| SR-001, SR-002         | 包契约检查 | `node scripts/quality/check-component-contracts.mjs`             | PASS                          | `component contracts PASS`     |
| SR-001, SR-002         | 包导出检查 | `node scripts/check-package-exports.mjs --package=@mango/detail` | PASS                          | `Package export check passed.` |
| SR-001, SR-002         | 差异检查   | `git diff --check`                                               | PASS                          | 无 whitespace 错误             |

## 7. 例外与剩余风险

- 全仓 `check-package-exports.mjs` 仍会报告其它未构建包的历史 dist 缺失；本任务范围内的 `@mango/detail` 定向检查已通过。
- `pnpm install --offline` 因本地缓存缺少 `@changesets/cli` tarball 未完成；验证使用已安装工具的直接命令，不改变锁文件。
- 本次未执行浏览器人工验收；改动仅控制公共组件宽度属性，组件测试和生产构建已覆盖目标行为。
