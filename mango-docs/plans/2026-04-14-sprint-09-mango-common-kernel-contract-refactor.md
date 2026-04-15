# Sprint 09: `mango-common` 公共内核与契约收敛

- 起始日期：2026-04-14
- 状态：待执行
- 所属任务：T9
- 关联总文档：`../mango-backend-architecture-boundary-refactor-master-plan.md`

---

## 1. 用户故事

作为 Mango 后端模块开发者和 AI Agent  
我想要一个边界清晰、依赖最小的 `mango-common`  
以便新模块能够稳定复用公共契约，而不会被动引入 Web、安全或技术实现耦合。

---

## 2. Sprint 交付目标

本 Sprint 只交付一件完整的事：

**把 `mango-common` 收敛为“公共内核 + 公共契约”，并清理不应长期留在 `common` 的内容。**

### 目标解释

这里的“公共内核 + 公共契约”描述的是**职责目标**，不是本 Sprint 必须新增的 Maven 子模块名称。

本 Sprint 的默认交付方式是：

- 继续使用现有单模块 `mango-common`
- 在单模块内完成职责收敛、包结构整理、类迁移与引用修正
- 不要求在本 Sprint 内新增 `mango-common-kernel`、`mango-common-contract` 等子模块

如果后续决定把 `mango-common` 再拆成多个 Maven 子模块，必须单独立项，并提供新的模块拆分设计文档与迁移计划。

---

## 3. 范围

### In Scope

- 统一 `mango-common` 的职责定义
- 在**现有单模块** `mango-common` 内完成职责收敛与包结构整理
- 重构或替换 `BizException`
- 统一 `R<T>` 返回契约
- 重构分页模型命名与语义
- 删除 `BasePO` / `BaseVO`
- 迁出 `Base64Utils`
- 处理 `JacksonUtils`
- 处理废弃 `TokenContextHolder`
- 修复 `mango-common` 的 parent `relativePath` 风险
- 清理 `mango-common` 中已确认存在的 Checkstyle 异味
- 重构 `Require` 中重复且原始的校验逻辑

### Out of Scope

- 把 `mango-common` 拆成新的 Maven 子模块（例如 `mango-common-kernel`、`mango-common-contract`）
- `mango-infra-web` 去业务依赖
- `mango-infra-kv` 模块拆分
- `mango-rbac` / `mango-system` 大范围拆分
- `mango-auth` 边界收敛

---

## 4. 可交付结果

本 Sprint 完成后，必须交付：

1. `mango-common` 新的包结构或模块结构
2. 已清理的公共模型与异常模型代码
3. 受影响模块的引用修正
4. 单元测试
5. 集成测试或兼容性验证代码
6. 构建与规范检查结果
7. API / 关键行为验证截图

说明：

- “新的包结构或模块结构”在本 Sprint 中优先指**包结构整理**。
- 只有在实现阶段明确批准时，才允许升级为模块拆分；当前 Sprint 文档不默认要求模块拆分。

---

## 5. 任务分解

### Task A: 明确 `mango-common` 的职责边界

- [ ] 补充 `mango-common` README 或文档说明
- [ ] 明确哪些类保留，哪些类迁出，哪些类删除
- [ ] 显式修正 `mango-common` 的 parent `relativePath`，避免默认回退到 `../pom.xml`

### Task B: 收敛公共异常与返回契约

- [ ] 调整 `BizException`，移除构造函数日志副作用
- [ ] 统一 `R<T>` 的协议字段语义
- [ ] 修正受影响调用方

### Task C: 收敛公共模型

- [ ] 以更清晰命名替换 `PagePO` / `PageVO`
- [ ] 删除 `BasePO` / `BaseVO`
- [ ] 修正受影响模块引用
- [ ] 清理 `R`、`BizException`、`Require` 中已确认存在的魔术数字用法

### Task D: 清理不应长期留在 `common` 的内容

- [ ] 迁出 `Base64Utils` 至 `mango-infra-crypto`
- [ ] 删除或替换 `JacksonUtils`
- [ ] 处理废弃 `TokenContextHolder`
- [ ] 收敛 `@Log` / `LogType` 的长期归属，避免继续留在 `common`

### Task E: 处理 `mango-common` 的代码异味

- [ ] 清理 `mango-common` 中已确认存在的 Javadoc 缺失或不规范问题
- [ ] 清理明显过长或可读性差的表达式写法
- [ ] 重构 `Require` 中重复的空值、布尔、集合断言逻辑
- [ ] 统一默认业务错误码来源，避免在 `Require` 等基础类中散落硬编码 `400` / `500`

### Task F: 测试与验证

- [ ] 为异常、分页、返回模型补充 UT
- [ ] 为受影响的公共契约补充集成测试或兼容性验证
- [ ] 执行 `mvn test`
- [ ] 执行 `mvn verify`
- [ ] 执行 `mvn mango:check`
- [ ] 执行 `mvn checkstyle:check`
- [ ] 执行 `mvn spotbugs:check`
- [ ] 执行 `mvn pmd:check`
- [ ] 产出关键行为验证截图

---

## 6. 验收标准

- [ ] `mango-common` 不再承载明显属于 Web、安全、加密实现层的工具类
- [ ] `BasePO` / `BaseVO` 已从代码中移除
- [ ] `BizException` 不再在构造函数内记录日志
- [ ] 统一返回契约在后端代码中无二义性
- [ ] `mango-common` 已显式声明正确的 parent `relativePath`
- [ ] `Require` 的重复断言逻辑已完成收敛
- [ ] `mango-common` 中已确认存在的 Checkstyle 异味已被清理到可通过检查
- [ ] 单元测试覆盖率达到规范要求
- [ ] `mvn test` 通过
- [ ] `mvn verify` 通过
- [ ] `mvn mango:check` 通过
- [ ] `mvn checkstyle:check` 通过
- [ ] `mvn spotbugs:check` 通过
- [ ] `mvn pmd:check` 通过
- [ ] 有 API 或关键行为截图作为验证留痕

---

## 7. 风险与注意事项

- 不允许为了兼容而长期保留双套公共模型
- 不允许把迁出内容简单复制到别处后保留旧实现
- 所有变更必须优先保持调用方语义稳定
- 不允许把“公共内核 + 公共契约”误解为“本 Sprint 必须新建 `mango-common-kernel` 等子模块”
