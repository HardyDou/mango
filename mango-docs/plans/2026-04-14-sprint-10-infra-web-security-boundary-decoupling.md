# Sprint 10: `infra-web` / `infra-security` 边界去业务依赖

- 起始日期：2026-04-15
- 状态：待执行
- 所属任务：T10
- 关联总文档：`../mango-backend-architecture-boundary-refactor-master-plan.md`

---

## 1. 用户故事

作为 Mango 基础设施模块维护者和 AI Agent  
我想要 `infra-web` 与 `infra-security` 只依赖公共抽象而不依赖平台业务模型  
以便基础设施层可以稳定复用，并保持真正的分层隔离。

---

## 2. Sprint 交付目标

本 Sprint 只交付一件完整的事：

**去除 `infra-web` / `infra-security` 中对平台业务模型的耦合，并明确 Web、安全、上下文三类基础能力的归属。**

---

## 3. 范围

### In Scope

- 清理 `mango-infra-web` 对 `mango-rbac-api` 的直接依赖
- 明确 `mango-infra-security` 的保留职责
- 收口 token / permission / context 的基础抽象归属
- 修正受影响的自动配置与调用方

### Out of Scope

- `mango-infra-kv` 大规模拆分
- `mango-rbac` / `mango-system` 业务拆分
- `mango-auth` 认证流程重构

---

## 4. 可交付结果

1. `infra-web` 去业务依赖后的代码
2. `infra-security` 边界收敛后的代码
3. 受影响模块的依赖与自动配置修正
4. 单元测试 / 集成测试
5. 构建、检查与验证截图

---

## 5. 任务分解

### Task A: `infra-web` 去业务依赖

- [ ] 移除 `mango-infra-web` 对 `mango-rbac-api` 的直接依赖
- [ ] 仅保留 Web 基础配置、异常处理、通用 filter
- [ ] 使用抽象接口替代业务模型依赖

### Task B: `infra-security` 收敛基础能力

- [ ] 明确保留 token 与 permission 基础抽象
- [ ] 处理通用 token context 的归属
- [ ] 禁止新增登录业务和 RBAC 业务逻辑到 `infra-security`

### Task C: 受影响模块适配

- [ ] 修正 `auth`、`rbac`、`app` 的依赖引用
- [ ] 校验自动配置加载顺序和 Bean 注入关系

### Task D: 测试与验证

- [ ] 补充 UT / 集成测试
- [ ] 执行 `mvn test`
- [ ] 执行 `mvn verify`
- [ ] 执行 `mvn mango:check`
- [ ] 执行 `mvn checkstyle:check`
- [ ] 执行 `mvn spotbugs:check`
- [ ] 执行 `mvn pmd:check`
- [ ] 输出关键请求链路截图

---

## 6. 验收标准

- [ ] `mango-infra-web` 不再直接依赖平台业务 API
- [ ] `mango-infra-security` 只保留安全基础能力
- [ ] 关键调用链 Bean 注入关系正确
- [ ] `mvn test` 通过
- [ ] `mvn verify` 通过
- [ ] `mvn mango:check` 通过
- [ ] `mvn checkstyle:check` 通过
- [ ] `mvn spotbugs:check` 通过
- [ ] `mvn pmd:check` 通过
- [ ] 有关键行为截图留痕
