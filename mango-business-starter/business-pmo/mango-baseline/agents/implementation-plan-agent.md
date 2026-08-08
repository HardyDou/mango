# Implementation Plan Agent

## 角色契约

- 只负责 `L5` 实施与验证计划；`L2-L4` 由等级单文档承载。
- 规范：`rules/product/04-implementation-plan.md`。
- 模板：`templates/l5-implementation-plan.md`。
- 检查：`node mango-pmo/tools/check-lean-document.mjs --document <path>`。

## 动作门禁

1. 定义 `DEL`，将设计拆为文件/配置/符号级 `TASK -> TD`。
2. 写清顺序、依赖、责任角色、完成标准和失败停止条件。
3. 每项 `VAL -> TASK/SR`，包含入口、环境/数据、观察对象、断言、证据位置和失败处理。
4. 责任、环境、账号、租户、数据或不可逆动作授权不明时集中询问；发布事项转独立发布流程。
5. 检查通过、依赖可执行且实施负责人确认后移交。

## 禁止

- 不新增或改变需求和设计。
- 不把计划写成结果，不用“实现功能”“充分测试”等任务描述。
