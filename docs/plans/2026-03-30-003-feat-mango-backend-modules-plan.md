---
title: "feat: mango 后端模块脚手架与基础设施"
type: feat
status: active
date: 2026-03-30
origin: docs/plans/2026-03-30-001-feat-mango-web-pigx-gap-analysis-plan.md
deepened: 2026-03-30
---

# mango 后端模块脚手架与基础设施计划

## Overview

本计划面向 mango 后端模块脚手架，搭建 6 个独立模块 + BFF 层，并完善加密体系、权限服务、国际化服务。本计划是 mango-web 前端计划的前置依赖，可与前端并行开发。

## Problem Frame

mango 后端当前仅有 `mango-parent` 和 `mango-common`，缺少支撑前端基础设施所需的后端模块：

- **模块缺失**：mango-user / mango-auth / mango-permission / mango-org / mango-i18n / mango-bff-admin 均不存在
- **加密体系缺失**：mango-common/crypto 子模块未建立
- **BFF 层缺失**：前端无法通过聚合层访问后端微服务

## Scope Boundaries

**本计划范围：**
- Phase 0: 6 个后端模块 + BFF 层的 Maven 脚手架
- Phase 1: crypto 加密模块、permission 服务、i18n 服务、BFF 聚合接口

**明确排除：**
- 各模块的业务逻辑实现（如用户 CRUD、角色管理等）→ 属于业务开发阶段
- 前端组件开发 → 属于 mango-web 计划

**为前端计划提供：**
- BFF 接口契约（见 Dependencies）
- 加密服务（后端持有 SM4 密钥）

---

## Module Architecture

```
mango-user          # 用户信息
mango-auth          # 认证（登录/登出/Token）
mango-permission    # 权限、角色、菜单
mango-org           # 租户、部门、岗位
mango-i18n          # 国际化
mango-bff-admin     # 前端聚合层（BFF）

各模块遵循 4 模式: {name}-api / {name}-core / {name}-starter / {name}-starter-remote
```

### crypto 模块结构

```
mango-common/crypto/
├── symmetric/       # 对称加密（SM4/AES）
├── asymmetric/     # 非对称加密（SM2/RSA）
├── digest/         # 摘要算法（SM3/SHA）
├── base/           # Base64 工具
└── support/        # CryptoFactory, KeyManager
```

---

## Dependencies

### 模块依赖关系（Maven xx-api 依赖）

> 遵循 SPI 规范，implementation 只依赖 api

```
mango-bff-admin
  ├── mango-user-api         # 用户信息
  ├── mango-auth-api         # 认证
  ├── mango-permission-api   # 权限
  ├── mango-org-api          # 组织
  └── mango-i18n-api         # 国际化

mango-permission-api
  ├── mango-org-api          # 部门、岗位
  └── mango-user-api         # 用户

mango-i18n-api
  └── 无外部依赖

mango-user-api / mango-auth-api / mango-org-api
  └── 无外部依赖
```

### 数据库依赖

- **sys_i18n 表**: 在 mango-i18n 模块创建
- **sys_user 表**: 已有，需增加 permissions 字段关联查询
- **sys_menu 表**: 已有，需确认 permission 字段
- **sys_role_menu 表**: 已有，角色-菜单关系

---

## Implementation Units

### Phase 0: 后端模块脚手架搭建

> ⚠️ Phase 0 是后端所有工作的前置条件，必须先完成。

---

#### Unit 0.1: 后端模块脚手架搭建

**Goal:** 搭建 6 个独立模块 + BFF 层的 Maven 项目结构

**Dependencies:** 无

**Files:**
- Create: `mango/pom.xml` → parent POM（扩展现有 mango-parent）
- Create: `mango-user/mango-user-api/pom.xml`
- Create: `mango-user/mango-user-core/pom.xml`
- Create: `mango-auth/mango-auth-api/pom.xml`
- Create: `mango-auth/mango-auth-core/pom.xml`
- Create: `mango-permission/mango-permission-api/pom.xml`
- Create: `mango-permission/mango-permission-core/pom.xml`
- Create: `mango-org/mango-org-api/pom.xml`
- Create: `mango-org/mango-org-core/pom.xml`
- Create: `mango-i18n/mango-i18n-api/pom.xml`
- Create: `mango-i18n/mango-i18n-core/pom.xml`
- Create: `mango-bff-admin/mango-bff-admin-api/pom.xml`
- Create: `mango-bff-admin/mango-bff-admin-core/pom.xml`
- Create: `mango-common/pom.xml`
- Create: `mango-common/crypto/pom.xml`

**Approach:**

```
1. 扩展现有 mango parent POM
   - mango-parent 已存在，定义 versions, pluginManagement
   - 统一 Java 17 + Spring Boot 3.2.3 + MyBatis-Plus 3.5.5

2. 创建各模块（与 pigx-upms 结构对齐）
   - mango-user: user-api / user-core
   - mango-auth: auth-api / auth-core
   - mango-permission: permission-api / permission-core
   - mango-org: org-api / org-core
   - mango-i18n: i18n-api / i18n-core
   - mango-bff-admin: bff-admin-api / bff-admin-core

   注：遵循 mango module-rules 规范，使用 -core 而非 -biz

3. 各模块遵循 4 模式:
   - {name}-api: 接口定义、DTO、VO、FeignClient
   - {name}-core: 业务实现、Controller、Service、Mapper
   - {name}-starter: Spring Boot 自动配置
   - {name}-starter-remote: 远程调用-starter（可选）

4. 扩展 mango-common 公共模块
   - 已有: R, BizCode, BasePO, BaseVO, PageVO, annotation, exception, valid
   - 新增: crypto 子模块（SM4/AES/RSA/SM2 实现）
```

**Patterns to follow:**
- 参考 `/Users/hardy/Work/company02/mango` 现有模块结构
- 参考 pigx-upms 模块划分

**Verification:**
- `mvn clean compile` 各模块都能编译通过
- 模块间无循环依赖

---

### Phase 1: 基础设施完善 (P1)

---

#### Unit 1.1: crypto 加密模块

**Goal:** 实现完整的加密算法支持，为 BFF 层提供加解密能力

**Dependencies:** Unit 0.1（脚手架）

**Files:**
- Create: `mango-common/crypto/src/main/java/io/mango/crypto/` → 全部类

**Approach:**

```
mango-common/crypto/
├── symmetric/
│   ├── Sm4Cipher.java      # SM4 对称加密（国密）
│   └── AesCipher.java      # AES 对称加密
├── asymmetric/
│   ├── Sm2Signer.java, Sm2Verifier.java, Sm2Cipher.java  # SM2
│   └── RsaSigner.java, RsaVerifier.java, RsaCipher.java  # RSA
├── digest/
│   ├── Sm3Digester.java, HmacSm3Digester.java            # SM3
│   └── Sha256Digester.java, Sha512Digester.java
├── base/Base64Utils.java
└── support/
    ├── CryptoFactory.java  # 密码学工厂
    └── KeyManager.java     # 密钥管理
```

```java
// 1. CryptoFactory.java
@Component
public class CryptoFactory {
    public SymmetricCipher getSymmetricCipher(String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "SM4" -> new Sm4Cipher();
            case "AES" -> new AesCipher();
            default -> throw new IllegalArgumentException("Unsupported: " + algorithm);
        };
    }
}

// 2. Sm4Cipher.java
@Component
public class Sm4Cipher implements SymmetricCipher {
    @Value("${mango.crypto.sm4-key}")
    private byte[] key;

    public String encrypt(String data) {
        return Base64Utils.encode(sm4Encrypt(data.getBytes(StandardCharsets.UTF_8)));
    }

    public String decrypt(String encrypted) {
        return new String(sm4Decrypt(Base64Utils.decode(encrypted)));
    }
}

// 3. application.yml
mango:
  crypto:
    enabled: true
    sm4-key: ${CRYPTO_SM4_KEY}   # 环境变量注入，服务端持有
    sm2-private-key: ${CRYPTO_SM2_PRIVATE_KEY}
```

> ⚠️ **安全原则**：SM4 对称密钥仅存在于服务端，通过环境变量 `${CRYPTO_SM4_KEY}` 注入，严禁出现在代码或仓库。

**Test scenarios:**
- SM4 加密后能正确解密
- SM2 签名能正确验签
- 密钥不存在时服务启动失败（有明确错误信息）

**Verification:**
- 单元测试：加密/解密、签名/验签正确
- 集成测试：BFF 层加解密往返成功

---

#### Unit 1.2: 权限服务 + BFF User 接口

**Goal:** 实现 `/bff/admin/user/info` 聚合接口，返回用户信息含 permissions 数组

**Dependencies:** Unit 0.1

**Files:**
- Create: `mango-permission-api/src/main/java/io/mango/permission/api/vo/UserInfoVO.java`
- Create: `mango-permission-core/src/main/java/io/mango/permission/controller/SysUserController.java`
- Create: `mango-permission-core/src/main/java/io/mango/permission/service/impl/SysUserServiceImpl.java`
- Create: `mango-bff-admin-core/src/main/java/io/mango/bff/admin/controller/UserBffController.java`

**Approach:**

```java
// 1. UserInfoVO.java
@Data
public class UserInfoVO extends UserVO {
    private List<String> permissions = new ArrayList<>();  // 权限码列表
}

// 2. SysUserController.java (mango-permission)
@GetMapping("/info")
public R<UserInfoVO> info() {
    String username = SecurityUtils.getUser().getUsername();
    return userService.getUserInfo(username);
}

// 3. UserBffController.java (BFF 聚合层)
@GetMapping("/bff/admin/user/info")
public R<UserInfoVO> info() {
    // 调用 mango-permission /user/info
    // 聚合返回
}

// 4. 数据库权限查询
-- sys_role_menu 表存储角色-菜单权限关系
-- sys_menu 表存储菜单 permission 字段
-- 通过 role_id 查询关联的 menu.permission 列表
```

> ⚠️ **后端强制鉴权**：前端 v-auth 仅做展示控制，所有按钮操作在后端必须强制校验 permissions。后端是唯一可信鉴权点。

**API 响应格式:**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "userId": 1,
    "username": "admin",
    "permissions": ["system:user:view", "system:user:add", "system:user:edit"]
  }
}
```

**Test scenarios:**
- admin 用户返回 permissions 数组（包含具体权限码，非 "*"）
- 普通用户只返回其角色的权限码
- 无权限用户返回空数组

**Verification:**
- GET /bff/admin/user/info 返回正确结构
- 权限码与数据库一致
- 未登录返回 401

---

#### Unit 1.3: 国际化服务 + BFF i18n 接口

**Goal:** 实现 `/admin/i18n/public` 公开端点和 `/bff/admin/i18n` 聚合接口

**Dependencies:** Unit 0.1

**Files:**
- Create: `mango-i18n-api/src/main/java/io/mango/i18n/api/entity/SysI18n.java`
- Create: `mango-i18n-api/src/main/java/io/mango/i18n/api/vo/SysI18nVO.java`
- Create: `mango-i18n-core/src/main/java/io/mango/i18n/mapper/SysI18nMapper.java`
- Create: `mango-i18n-core/src/main/java/io/mango/i18n/service/SysI18nService.java`
- Create: `mango-i18n-core/src/main/java/io/mango/i18n/service/impl/SysI18nServiceImpl.java`
- Create: `mango-i18n-core/src/main/java/io/mango/i18n/controller/SysI18nController.java`
- Create: `mango-bff-admin-core/src/main/java/io/mango/bff/admin/controller/I18nBffController.java`

**Approach:**

```java
// 1. SysI18n.java
@TableName("sys_i18n")
@Data
public class SysI18n {
    private Long id;
    private String name;    // key
    private String zhCn;    // 中文
    private String en;       // 英文
}

// 2. SysI18nController.java
@RestController
@RequestMapping("/i18n")
public class SysI18nController {

    // ✅ 公开端点：无需登录即可获取语言包
    @Inner(false)
    @GetMapping("/public")
    public R<Map<String, List<Map<String, String>>>> publicInfo() {
        return R.ok(sysI18nService.listMap());
    }

    // ✅ 公开端点：支持的语言列表
    @Inner(false)
    @GetMapping("/languages")
    public R<List<String>> languages() {
        return R.ok(Arrays.asList("zh-cn", "en"));
    }

    // ❌ 受保护端点：语言内容管理（需登录）
    // GET /i18n/page - 分页查询（需 permission:system:i18n:view）
    // POST /i18n - 新增（需 permission:system:i18n:add）
    // PUT /i18n - 修改（需 permission:system:i18n:edit）
    // DELETE /i18n - 删除（需 permission:system:i18n:del）
}

// 3. I18nBffController.java
@RestController
@RequestMapping("/bff/admin/i18n")
public class I18nBffController {
    @GetMapping
    public R<Map<String, List<Map<String, String>>>> i18n(
            @RequestParam String lang) {
        // 调用 mango-i18n /i18n/public
        return i18nService.listMap();
    }
}

// 4. 数据库表
CREATE TABLE sys_i18n (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT 'key',
  zh_cn VARCHAR(500) COMMENT '中文',
  en VARCHAR(500) COMMENT '英文',
  INDEX idx_name (name)
);
```

**BFF API 响应格式:**
```json
// GET /bff/admin/i18n?lang=zh-cn
{
  "code": 0,
  "msg": "success",
  "data": {
    "zh-cn": [
      { "common.save": "保存", "common.cancel": "取消" },
      { "menu.home": "首页" }
    ],
    "en": [
      { "common.save": "Save", "common.cancel": "Cancel" },
      { "menu.home": "Home" }
    ]
  }
}
```

**Test scenarios:**
- `/admin/i18n/public` 无需登录返回语言包
- `/admin/i18n/languages` 返回支持的语言列表
- BFF 聚合接口正确聚合多模块语言包

**Verification:**
- 公开端点无需 Authorization header 即可访问
- 语言包格式与 vue-i18n mergeLocaleMessage 兼容

---

#### Unit 1.4: BFF 层加密网关（可选，P1 可选）

**Goal:** BFF 层实现 SM4 加解密，作为前端与后端微服务之间的加密边界

**Dependencies:** Unit 0.1 + Unit 1.1

**Approach:**
- BFF 层在接收前端请求时使用 SM4 解密
- BFF 层在调用后端微服务时使用 SM4 加密
- 提供加密开关 `mango.crypto.enabled=false` 时透传明文（降级策略）

> ⚠️ **降级策略**：必须实现加密开关，加密服务不可用时自动降级为明文模式，保障系统可用性。

---

## Risks & Dependencies

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 后端模块脚手架未就绪 | 前端无法联调 | Phase 0 优先完成 |
| SM4 加密与前端不一致 | API 调用失败 | 先完成接口契约定义，再实现加密 |
| 加密失败导致 API 不可用 | 系统完全不可用 | **必须提供降级策略**：`mango.crypto.enabled=false` 透传明文 |
| 密钥泄露 | 数据安全风险 | 密钥通过环境变量/KMS 注入，严禁出现在代码或仓库 |

---

## Sources & References

- **Origin:** [docs/plans/2026-03-30-001-feat-mango-web-pigx-gap-analysis-plan.md](docs/plans/2026-03-30-001-feat-mango-web-pigx-gap-analysis-plan.md)
- **mango 后端源码:** `/Users/hardy/Work/company02/mango`
- **pigx-upms 参考:** `/Users/hardy/Work/pigx/pigx-upms`
- **前端并行计划:** `docs/plans/2026-03-30-002-feat-mango-web-frontend-plan.md`
