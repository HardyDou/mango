# Sprint 06: mango-infra-security 新建

- 起始日期：2026-04-08
- 状态：待执行
- 所属任务：T6
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## 背景

`mango-common/permission/` 目录下有 Spring AOP 耦合代码（`@Aspect` / `@Component` / 静态 `IPermissionService`），需移入 `mango-infra-security` 新模块。

> 参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 1.6 框架分层原则 + Section 三 `mango-infra-security`

---

## 待移出文件

```
mango-common/src/main/java/io/mango/common/permission/
├── PermAspect.java              ← @Aspect @Component，依赖 Spring AOP → 移入 infra-security
├── IPermissionService.java      ← 接口，零依赖 → 可保留 common
└── PermissionServiceImpl.java  ← @Service → 移入（依赖 IPermissionService）
```

**注意**：`PermAspect` 中有静态 `IPermissionService` 字段（setter 注入），需确认是否已改为 `@Autowired` 注入。

---

## 4 层结构

```
mango-infra-security/
├── mango-infra-security-api/        ← 接口定义（IPermissionService）
├── mango-infra-security-core/        ← 实现（PermissionServiceImpl）
├── mango-infra-security-starter/     ← @Configuration + @Aspect + SPI 注入
└── mango-infra-security-starter-remote/ ← 预留（微服务时）
```

---

## 约束

- `PermAspect` 必须留在 `-starter`，因为 `@Aspect` 依赖 Spring AOP
- `IPermissionService` 接口可留在 `common`（零依赖）
- 静态字段注入需改为实例注入
- SPI 注入使用 `@ConditionalOnProperty`

---

## 实施步骤

- [ ] 1. 创建 `mango-infra-security/` 4 层目录结构
- [ ] 2. 移入 `PermAspect` 和 `PermissionServiceImpl`
- [ ] 3. 在 `-api` 中定义接口（如需独立）
- [ ] 4. 在 `-starter` 中配置 `@Aspect` + SPI 注入
- [ ] 5. 修正 `mango-common/permission/` 只保留接口定义
- [ ] 6. 搜索全项目 import，修正包引用

---

## 参考

- T1 commit：`c0b7df41`（T1 的 4 层结构可复用）
- 接口设计参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 三
