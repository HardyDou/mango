# Sprint 12: `mango-auth` 与 `mango-admin-app` 边界收口

- 起始日期：2026-04-17
- 状态：待执行
- 所属任务：T12
- 关联总文档：`../mango-backend-architecture-boundary-refactor-master-plan.md`

---

## 1. 用户故事

作为 Mango 认证模块和应用装配层维护者  
我想要 `mango-auth` 只负责认证，`mango-admin-app` 只负责装配  
以便认证域和装配域的职责稳定，后续单体/远程化演进更可控。

---

## 2. Sprint 交付目标

本 Sprint 只交付一件完整的事：

**收敛 `mango-auth` 的认证职责，并清理 `mango-admin-app` 的错误依赖与装配层职责。**

---

## 3. 范围

### In Scope

- 收敛 `mango-auth` 的认证职责
- 清理不应长期留在 `auth` 中的安全治理能力
- 清理 `mango-admin-app` 中错误或漂移的业务依赖
- 明确 `app` 层只承担装配和部署职责

### Out of Scope

- `mango-rbac` / `mango-system` 大规模拆分
- `mango-infra-kv` 完整拆分

---

## 4. 可交付结果

1. 收敛后的 `mango-auth` 代码
2. 清理后的 `mango-admin-app` 依赖与装配代码
3. 受影响模块适配代码
4. 单元测试 / 集成测试
5. 构建、检查与验证截图

---

## 5. 任务分解

### Task A: `mango-auth` 职责收敛

- [ ] 明确保留身份认证、token 生命周期、auth provider 能力
- [ ] 将不应继续留在 `auth` 中的治理能力迁离或解耦

### Task B: `mango-admin-app` 装配层清理

- [ ] 清理错误或漂移的业务依赖
- [ ] 明确 `app` 层只负责 starter 装配、profile、运行配置

### Task C: 受影响模块适配

- [ ] 修正自动配置和依赖注入
- [ ] 保证登录与核心管理端能力行为不回退

### Task D: 测试与验证

- [ ] 补充或修正 UT / 集成测试
- [ ] 执行 `mvn test`
- [ ] 执行 `mvn verify`
- [ ] 执行 `mvn mango:check`
- [ ] 执行 `mvn checkstyle:check`
- [ ] 执行 `mvn spotbugs:check`
- [ ] 执行 `mvn pmd:check`
- [ ] 输出关键认证流程截图

---

## 6. 验收标准

- [ ] `mango-auth` 仅保留认证核心职责
- [ ] `mango-admin-app` 不再承载错误的业务实现职责
- [ ] 依赖关系与模块命名一致
- [ ] `mvn test` 通过
- [ ] `mvn verify` 通过
- [ ] `mvn mango:check` 通过
- [ ] `mvn checkstyle:check` 通过
- [ ] `mvn spotbugs:check` 通过
- [ ] `mvn pmd:check` 通过
- [ ] 有关键行为截图留痕
