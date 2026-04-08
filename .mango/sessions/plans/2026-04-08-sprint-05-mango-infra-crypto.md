# Sprint 05: mango-infra-crypto 新建

- 起始日期：2026-04-08
- 状态：⚠️ 已合并到 sprint-06
- 所属任务：T5（已合并到 T6）
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## 背景

`mango-common/crypto/` 目录下有框架耦合代码（依赖 BouncyCastle / Spring / `@Component`），需移入 `mango-infra-crypto` 新模块。

> 参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 1.6 框架分层原则 + Section 三 `mango-infra-crypto`

---

## 待移出文件（13 个）

```
mango-common/src/main/java/io/mango/common/crypto/
├── Sm4Cipher.java              ← @Component，依赖 BouncyCastle → 移入 infra-crypto
├── AesCipher.java              ← @Component，依赖 BouncyCastle → 移入
├── RsaSigner.java             ← @Component，依赖 BouncyCastle → 移入
├── Sm2Signer.java             ← @Component，依赖 BouncyCastle → 移入
├── Sm3Digest.java             ← @Component，依赖 BouncyCastle → 移入
├── CryptoFactory.java         ← @Component，@Autowired → 移入
├── ICipher.java               ← 接口，零依赖 → 可保留 common
├── ISigner.java               ← 接口，零依赖 → 可保留 common
├── ISignEncryptService.java   ← 接口定义 → 视依赖决定
├── support/
│   ├── CryptoFactory.java
│   ├── Sm4Utils.java
│   └── BouncyCastleUtils.java
└── config/
    └── CryptoConfig.java       ← @Configuration → 移入 infra-crypto
```

**判断标准**（Section 1.6）：
- 零运行时依赖（只依赖 JDK）→ 留 `mango-common`
- 依赖 BouncyCastle / Spring / AOP → 移入 `mango-infra-crypto`

---

## 4 层结构

```
mango-infra-crypto/
├── mango-infra-crypto-api/        ← 接口定义（ICipher/ISigner/ISignEncryptService）
├── mango-infra-crypto-core/        ← 实现（Sm4Cipher/AesCipher/RsaSigner/Sm2Signer/CryptoFactory）
├── mango-infra-crypto-starter/     ← @Configuration + @ConditionalOnProperty SPI 注入
└── mango-infra-crypto-starter-remote/ ← 预留（微服务时）
```

---

## 约束

- 移出后 `mango-common` 禁止再引入 BouncyCastle 依赖
- `mango-common/crypto/` 只保留接口定义和工具类
- SPI 注入使用 `@ConditionalOnProperty`

---

## 实施步骤

- [ ] 1. 创建 `mango-infra-crypto/` 4 层目录结构
- [ ] 2. 移入所有 `@Component` 实现类
- [ ] 3. 在 `-api` 中定义接口（ICipher/ISigner）
- [ ] 4. 在 `-starter` 中配置 SPI 注入
- [ ] 5. 修正 `mango-common/pom.xml` 移除 BouncyCastle 依赖
- [ ] 6. 搜索全项目 import，修正包引用

---

## 参考

- T1 commit：`c0b7df41`（T1 的 4 层结构可复用）
- 接口设计参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 三
