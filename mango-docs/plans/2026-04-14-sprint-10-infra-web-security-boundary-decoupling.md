# Sprint 10: `infra-web` / `infra-security` 边界去业务依赖

- 起始日期：2026-04-15
- 状态：已完成（代码、文档与 focused 验证完成）
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

- [x] 移除 `mango-infra-web` 对 `mango-rbac-api` 的直接依赖
- [x] 仅保留 Web 基础配置、异常处理、通用 filter
- [x] 使用抽象接口替代业务模型依赖

### Task B: `infra-security` 收敛基础能力

- [x] 明确保留 token 与 permission 基础抽象
- [x] 处理通用 token context 的归属
- [x] 禁止新增登录业务和 RBAC 业务逻辑到 `infra-security`

### Task C: 受影响模块适配

- [x] 修正 `auth`、`rbac`、`app` 的依赖引用
- [x] 校验自动配置加载顺序和 Bean 注入关系

### Task D: 测试与验证

- [x] 补充 UT / 集成测试
- [x] 执行 focused `mvn test`
- [x] 执行 focused `mvn verify`
- [ ] 执行 `mvn mango:check`
- [x] 通过 `verify` 生命周期覆盖 `checkstyle`
- [x] 通过 `verify` 生命周期覆盖 `spotbugs`
- [x] 通过 `verify` 生命周期覆盖 `pmd`
- [ ] 输出关键请求链路截图

---

## 6. 验收标准

- [x] `mango-infra-web` 不再直接依赖平台业务 API
- [x] `mango-infra-security` 只保留安全基础能力
- [x] 关键调用链 Bean 注入关系正确
- [x] focused `mvn test` 通过
- [x] focused `mvn verify` 通过
- [ ] `mvn mango:check` 通过
- [x] `checkstyle` 随 `verify` 通过
- [x] `spotbugs` 随 `verify` 通过
- [x] `pmd` 随 `verify` 通过
- [ ] 有关键行为截图留痕

## 7. 当前完成说明

已完成的代码与文档收口：

- `mango-infra-web` 通过 `IInternalPathProvider` 抽象解耦 RBAC 业务模型
- `mango-rbac-starter` 负责桥接 `ISysPublicPathService` 到 `infra-web`
- `SysMenuController` 不再依赖 Spring Security 上下文，改为使用 `TokenContextHolder + ITokenService`
- `mango-infra-security` 保持 token / permission / context 的基础职责，不承载登录或 RBAC 业务实现
- 补充了 `InternalCallFilter` 的 focused UT，覆盖非内部路径放行、内部路径拒绝、dev 模式放行三类边界行为
- 修正 `infra-security`、`rbac`、`gateway` 相关事实源中的 `mango-permission` 旧命名漂移

## 8. Validation Notes

在 `mango/` 下执行的 focused 验证命令：

```bash
mvn -pl mango-infra/mango-infra-web,\
mango-infra/mango-infra-security/mango-infra-security-api,\
mango-infra/mango-infra-security/mango-infra-security-core,\
mango-infra/mango-infra-security/mango-infra-security-starter,\
mango-infra/mango-gateway/mango-gateway-api,\
mango-platform/mango-rbac/mango-rbac-api,\
mango-platform/mango-rbac/mango-rbac-core,\
mango-platform/mango-rbac/mango-rbac-starter,\
mango-app/mango-admin-app \
  -am \
  -Dtest=InternalCallFilterTest,DefaultPermissionServiceImplTest,JjwtTokenServiceImplTest,JjwtTokenServiceImplBlacklistTest,PermAspectTest,SysPublicPathServiceImplTest,SysMenuServiceImplTest,SysMenuGroupServiceImplTest,SysUserServiceImplTest,SysRoleServiceImplTest,RbacPermissionCheckerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test -q

mvn -pl mango-infra/mango-infra-web,\
mango-infra/mango-infra-security/mango-infra-security-api,\
mango-infra/mango-infra-security/mango-infra-security-core,\
mango-infra/mango-infra-security/mango-infra-security-starter,\
mango-infra/mango-gateway/mango-gateway-api,\
mango-platform/mango-rbac/mango-rbac-api,\
mango-platform/mango-rbac/mango-rbac-core,\
mango-platform/mango-rbac/mango-rbac-starter,\
mango-app/mango-admin-app \
  -am -DskipTests verify -q
```

结果：

- focused `test` 通过
- focused `verify` 通过
- `verify` 已覆盖对应模块的 `checkstyle` / `spotbugs` / `pmd`

当前剩余阻塞：

- `mvn mango:check` 在当前仓库中通过完整坐标执行时命中 `mango-maven-plugin` 的 `NullPointerException`
- 关键请求链路截图尚未补采
