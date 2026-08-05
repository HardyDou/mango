# 前端开发流程

## 1. 适用范围

- 本文件只约束前端开发。
- 通用流程以 [00-dev-flow.md](../00-dev-flow.md) 为准。

## 2. 开发前

先从 `business-pmo/mango-baseline/code-templates/index.json`（Mango 源仓使用 `mango-pmo/code-templates/index.json`）选择适用代码 baseline。package 结构、API context、导出入口、页面骨架、样式入口和测试结构以模板为准；旧页面只用于确认业务字段、接口和兼容事实。

必须明确：

- 页面或模块目标
- 范围
- 路由变化
- 接口变化
- 状态管理变化
- 测试范围

## 3. 开发中

- 优先复用 `mango-ui` 现有包和组件。
- 新增 package、页面和组件从选中的代码 baseline 渲染，不复制旧页面目录后再改名。
- 列表、独立详情、独立表单和标准弹框按对应页面规范使用 `@mango/common` 当前公共骨架，不从存量页面复制旧布局。
- 页面私有能力留在页面目录。
- 可复用能力下沉到对应 `packages/*`。
- 新增公共能力必须补导出。
- 不在基座和公共包维护两份相同实现。
- 触及旧文件时，修复同一组件或局部逻辑内行为不变且能由当前验证覆盖的违规；涉及公开 props、事件、路由、权限或跨包 API 时拆为独立治理项。

## 4. 提交前验证

至少执行与改动范围对应的检查：

- `pnpm lint`
- `pnpm build`
- `pnpm test`
- `pnpm playwright test`
- `node business-pmo/mango-baseline/tools/check-frontend-page-baseline.mjs --base <base> --head <head>`（业务项目页面变更）

## 5. 提交要求

提交说明必须写清：

- 改动页面或模块
- 接口影响
- 验证结果
- 遗留问题

## 6. 禁止事项

- 没有范围就直接改页面
- 没有验证就提交
- 直接复制已有页面再长期保留重复实现
- 在规范文件里写过时视觉常量
