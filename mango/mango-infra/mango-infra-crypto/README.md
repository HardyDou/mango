# mango-infra-crypto

`mango-infra-crypto` 提供项目内可复用的基础密码能力实现。

该模块不是生产级完整密码框架，也不负责密钥生命周期、密钥轮换、硬件密钥托管、合规审计或统一加密策略。涉及敏感数据、跨系统协议或合规要求时，应在业务场景中做单独安全评审。

## 当前职责

- 提供 SM4 对称加解密基础实现。
- 提供 SM3 哈希基础实现。
- 提供 SM2 签名验签基础实现。
- 保留 AES、RSA、SHA-256、HMAC-SM3 等基础实现供显式使用。
- 提供 Spring Boot 自动配置入口，默认只装配当前已验证的 SM2/SM3/SM4 能力。

## 非职责

- 不默认声明 AES、RSA、HMAC 为完整生产能力。
- 不提供 RSA 私钥解密能力；`RsaCipher` 当前只表达公钥加密。
- 不把 HMAC 伪装成无密钥摘要；`HmacSm3Digester` 必须显式传入 key。
- 不负责密钥生成、密钥存储、密钥轮换、证书管理或 KMS/HSM 集成。
- 不替代业务侧的数据分级、脱敏、签名协议设计和合规评审。

## 自动配置

启用条件：

```yaml
mango:
  crypto:
    enabled: true
```

默认自动装配：

- `ICryptoService`：`Sm4CryptoService`
- `Sm3CryptoService`
- `ISignService`：`Sm2SignService`

SM4 示例配置：

```yaml
mango:
  crypto:
    sm4:
      secret-key: "${MANGO_CRYPTO_SM4_SECRET_KEY:}"
      mode: CBC
      padding: PKCS5Padding
```

`secret-key` 必须通过环境变量、密钥管理系统或部署配置提供，不应把公开示例值写入真实配置。

SM2 示例配置：

```yaml
mango:
  crypto:
    sm2:
      private-key: "<Base64 PKCS#8 private key>"
      public-key: "<Base64 public key>"
      user-id: "1234567812345678"
```

## 使用边界

`IAsymmetricCryptoService` 只表示公钥加密能力，不再承诺私钥解密。需要私钥解密时，应新增独立接口和明确的密钥托管策略。

`IDigester` 只表示无密钥摘要能力。HMAC 应使用 `IKeyedDigester`，调用方必须显式提供 key。
