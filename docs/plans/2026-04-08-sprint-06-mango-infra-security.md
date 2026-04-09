# Sprint 06: mango-infra-security 新建（JWT 合并）

- 起始日期：2026-04-08
- 状态：🔄 处理中
- 所属任务：T6
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## 背景

**问题**：`mango-infra-security` 只有 `PermAspect`（太薄）。JWT 管理分散在：
- `mango-auth-core/util/AuthJwtUtil`
- `mango-gateway-core/util/JwtUtil`

两处重复，且 `AuthServiceImpl` (343行) 违反单一职责。

**解决**：JWT 管理合并到 `mango-infra-security`，作为 Layer 1 基础设施。

> T5（`mango-infra-crypto`）已单独完成（commit `1330f0b6`），不在本 sprint 范围。

---

## 本次完成结构

```
mango-infra/mango-infra-security/
├── mango-infra-security-api/
│   └── src/main/java/io/mango/infra/security/api/
│       ├── IPermissionService.java         ← (已存在)
│       └── ITokenService.java              ← 新增
│
├── mango-infra-security-core/
│   └── src/main/java/io/mango/infra/security/core/impl/
│       ├── DefaultPermissionServiceImpl.java  ← (已存在)
│       └── JjwtTokenServiceImpl.java          ← 新增
│
├── mango-infra-security-starter/
│   └── src/main/java/io/mango/infra/security/starter/
│       ├── aspect/PermAspect.java           ← (已存在)
│       ├── SecurityAutoConfiguration.java   ← (已存在)
│       └── TokenAutoConfiguration.java      ← 新增
```

---

## ITokenService 接口

```java
public interface ITokenService {
    String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims);
    String generateRefreshToken(Long userId, String username);
    boolean validateToken(String token);
    Long getUserId(String token);
    String getUsername(String token);
    String getTokenType(String token);
    TokenPair refresh(String refreshToken);

    record TokenPair(String accessToken, String refreshToken) {}
}
```

---

## 变更文件清单

### 新增
| 文件 | 说明 |
|------|------|
| `infra-security-api/.../ITokenService.java` | JWT 服务接口 |
| `infra-security-core/.../JjwtTokenServiceImpl.java` | JJWT 实现 |
| `infra-security-starter/.../TokenAutoConfiguration.java` | SPI 自动配置 |
| `JjwtTokenServiceImplTest.java` | 单元测试（13 cases） |

### 删除
| 文件 | 说明 |
|------|------|
| `mango-auth-core/util/AuthJwtUtil.java` | 重复，JWT 逻辑已移入 infra |
| `mango-gateway-core/util/JwtUtil.java` | 重复，JWT 逻辑已移入 infra |

### 重构
| 文件 | 变更 |
|------|------|
| `AuthServiceImpl.java` | 删除 JJWT 直接调用，注入 `ITokenService` |
| `AuthSecurityConfig.java` | `AuthFilterBean` 改用 `ITokenService` |
| `AuthGlobalFilter.java` | 改用 `ITokenService` |
| `AuthFilter.java` | 改用 `ITokenService` |
| `GatewayStarterConfig.java` | 删除 `JwtUtil` bean |
| `GatewayRemoteConfig.java` | 删除 `JwtUtil` bean |
| `SysUserController.java` | `getUsernameFromToken` 改用 `ITokenService` |

### POM 变更
| 模块 | 变更 |
|------|------|
| `infra-security-core/pom.xml` | +JJWT 依赖 |
| `infra-security-starter/pom.xml` | +JJWT 依赖 +`TokenAutoConfiguration` 注册 |
| `mango-auth-core/pom.xml` | -JJWT 依赖，+`mango-infra-security-api` |
| `mango-auth-starter/pom.xml` | +`mango-infra-security-starter` |
| `mango-gateway-core/pom.xml` | -JJWT 依赖，+`mango-infra-security-api` |
| `mango-gateway-starter/pom.xml` | -JJWT 依赖，+`mango-infra-security-starter` |
| `mango-gateway-starter-remote/pom.xml` | -JJWT 依赖，+`mango-infra-security-starter` |
| `mango-permission-starter/pom.xml` | -JJWT 依赖，+`mango-infra-security-starter` |

---

## 实施步骤

### Phase 1: T6 已有工作 ✅

- [x] 创建 `mango-infra-security/` 4 层目录结构
- [x] 移入 `PermAspect` 到 `-starter`
- [x] 移入 `IPermissionService` 到 `-api`
- [x] 在 `-core` 添加 `DefaultPermissionServiceImpl`
- [x] 配置 SPI 注入 + `@ConditionalOnMissingBean`

### Phase 2: JWT 合并（本次完成 ✅）

- [x] 13. 在 `infra-security-api` 新建 `ITokenService.java`
- [x] 14. 在 `infra-security-core` 新建 `JjwtTokenServiceImpl.java`
- [x] 15. 在 `infra-security-starter` 新建 `TokenAutoConfiguration.java`
- [x] 16. 重构 `AuthServiceImpl`：删除 JJWT，注入 `ITokenService`
- [x] 17. 重构 `AuthSecurityConfig`：注入 `ITokenService`
- [x] 18. 重构 `AuthGlobalFilter`：注入 `ITokenService`
- [x] 19. 重构 `AuthFilter`：注入 `ITokenService`
- [x] 20. 重构 `SysUserController`：注入 `ITokenService`
- [x] 21. 重构 `GatewayStarterConfig`：删除 `JwtUtil` bean
- [x] 22. 重构 `GatewayRemoteConfig`：删除 `JwtUtil` bean
- [x] 23. 删除 `AuthJwtUtil.java`
- [x] 24. 删除 `JwtUtil.java`
- [x] 25. 更新所有相关 POM 文件

### Phase 3: 收尾

- [ ] 26. `mvn compile` 全项目验证
- [ ] 27. `mvn test` 全项目测试
- [ ] 28. 更新 sprint-00 架构文档

---

## 验证结果

```bash
mvn compile  ✅  全项目编译通过
mvn test     ✅  21 tests passed (PermAspectTest 5 + DefaultPermissionServiceImplTest 6 + JjwtTokenServiceImplTest 13)
```
