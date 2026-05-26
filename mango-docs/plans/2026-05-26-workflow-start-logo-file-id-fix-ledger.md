# 审批中心发起流程 logo 文件 ID 修复交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| WSL-001 | 用户要求 | 流程 logo 未配置时不能加载 file 并触发 `参数类型错误: id` | 文件引用归一化只允许正整数文件 ID，空值和普通文本返回空 | `mango-ui/packages/file/src/api/file.ts` | 单元测试和构建验证 | DONE | `mango-ui/packages/file/src/api/__tests__/fileReferences.spec.ts` |
| WSL-002 | 用户要求 | 发起流程页面遇到非文件 ID logo 应正常展示 | 发起流程页通过 `normalizeFileId` 获取预览 ID，非文件 ID 自动走默认图标 | `mango-ui/packages/workflow/src/views/start-process/index.vue` | 代码审查与构建验证 | DONE | `mango-ui/packages/workflow/src/views/start-process/index.vue` |
| WSL-003 | 测试要求 | 补充缺陷回归验证 | 覆盖空值、`file`、Element 图标名、直接 URL、文件 token 和正整数 ID | `fileReferences.spec.ts` | `pnpm exec vitest run --config packages/workflow/vitest.config.ts packages/file/src/api/__tests__/fileReferences.spec.ts` | DONE | `mango-ui/packages/file/src/api/__tests__/fileReferences.spec.ts` |
| WSL-004 | 规范要求 | 交付前完成 PMO 台账检查 | 使用 delivery-contract-check 验证设计说明与台账 | 交付台账 | PMO 检查命令 | DONE | 本文件 |
