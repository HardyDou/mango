# Sprint 11: `mango-rbac` / `mango-system` 平台重模块边界收敛第一阶段

- 起始日期：2026-04-16
- 状态：待执行
- 所属任务：T11
- 关联总文档：`../mango-backend-architecture-boundary-refactor-master-plan.md`

---

## 1. 用户故事

作为 Mango 平台模块维护者和 AI Agent  
我想要 `mango-rbac` 与 `mango-system` 的职责边界更清晰  
以便用户、授权、配置、租户、审计、字典等能力不再继续混杂膨胀。

---

## 2. Sprint 交付目标

本 Sprint 只交付一件完整的事：

**完成 `mango-rbac` 与 `mango-system` 第一阶段边界收敛，消除明显错误归属，并为后续 bounded context 拆分建立稳定接口边界。**

---

## 3. 范围

### In Scope

- 清理 `mango-rbac` 中与授权无关的职责残留
- 清理 `mango-system` 中明显跨域的职责堆叠
- 明确 identity / authorization / config / tenant / audit / dict 的边界
- 修正文档、README、POM 中的领域命名漂移

### Out of Scope

- 一次性拆出全部新 Maven 模块
- `mango-auth` 的认证流程改造
- `mango-admin-app` 装配层收口

---

## 4. 可交付结果

1. `mango-rbac` / `mango-system` 收敛后的边界说明
2. 代码中的错误归属清理
3. 命名与文档一致性修正
4. 单元测试
5. 构建、检查与截图

---

## 5. 任务分解

### Task A: `mango-rbac` 边界收敛

- [ ] 清理与授权无关的职责残留
- [ ] 明确用户相关能力与授权相关能力的边界
- [ ] 修正文档中 `mango-permission` / `mango-rbac` 漂移

### Task B: `mango-system` 边界收敛

- [ ] 明确配置、租户、审计、字典、路由的边界
- [ ] 清理明显不应继续耦合在一起的职责

### Task C: 接口与依赖修正

- [ ] 修正受影响 API / Service / starter 的依赖关系
- [ ] 确保未引入新的跨域直接依赖

### Task D: 测试与验证

- [ ] 补充或修正 UT
- [ ] 执行 `mvn test`
- [ ] 执行 `mvn verify`
- [ ] 执行 `mvn mango:check`
- [ ] 执行 `mvn checkstyle:check`
- [ ] 执行 `mvn spotbugs:check`
- [ ] 执行 `mvn pmd:check`
- [ ] 输出关键接口与文档截图

---

## 6. 验收标准

- [ ] `mango-rbac` 中用户与授权职责不再继续混杂扩张
- [ ] `mango-system` 中配置、租户、审计、字典的边界已明确
- [ ] README / POM / 代码命名一致
- [ ] `mvn test` 通过
- [ ] `mvn verify` 通过
- [ ] `mvn mango:check` 通过
- [ ] `mvn checkstyle:check` 通过
- [ ] `mvn spotbugs:check` 通过
- [ ] `mvn pmd:check` 通过
- [ ] 有验证截图留痕
