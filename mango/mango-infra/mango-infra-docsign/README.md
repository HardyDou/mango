# Mango Infra Docsign

## 1. 概览

`mango-infra-docsign` 提供本地 PDF/OFD 文档数字签名、可见印章、骑缝章和验签能力。统一入口是 `DocumentSignApi`，实现基于 PDFBox 3、Bouncy Castle 和 OFDRW 2.4.0。大文件使用流式入口：输入以固定缓冲区写入受控临时文件，输出直接写入调用方流，堆内存不随文档大小线性增长。

文档签章与字符串数据签名不同：它需要保护文档字节范围、表达签章位置、支持多签名，并同时判断密码学结果、文档完整性、证书时间和调用方信任。

## 2. 功能清单

| 能力 | 实现 |
|------|------|
| PDF 数字签名 | PDFBox 增量签名与 `adbe.pkcs7.detached` CMS/PKCS#7；支持自动选择或显式指定 RSA SHA-256/384/512、RSA-PSS SHA-256、SM2/SM3 |
| PDF 可见章 | 普通章或按页切片的骑缝章；章图写入受签页面内容 |
| OFD 数字签名 | OFDRW GB/T 35275 SM2/SM3 签名 |
| OFD 电子印章 | 消费调用方提供的 SES v4 电子印章数据；支持普通章与骑缝章位置 |
| 多签名 | PDF 继续增量签名；OFD `ContinueSign` 追加签名 |
| 验签 | 返回每个签名的密码学、完整性、证书时间、信任、当前文档覆盖和整体结论 |
| 大文件处理 | `InputStream`/`OutputStream` 流式 API；PDF/OFD 随机访问由短期磁盘文件承载 |

## 3. 能力边界

- 本模块不存储文件，不读取文件中心，不感知租户、菜单、角色或业务权限。调用方必须先完成文件归属、租户和签章权限校验。
- 调用方在每次签名时提供 PKCS#12 密钥材料，在验签时提供信任库；模块不从仓库配置读取或托管私钥。
- 缺少信任库时仍可返回密码学和完整性分项结果，但整体 `valid=false`。
- 可见章图片只是一种受签外观，不等于数字签名；必须结合文档签名和验签结果判断真实性。
- OFD 当前只交付标准 SM2/SM3 签名，不生成自定义 RSA OFD 签名格式。
- OFD 原生电子印章必须由调用方提供 SES v4 `.esl`/DER 数据。OFDRW 的 `SESV4Container` 不能替代合规制章、证书签发、专用设备、审批和法律效力认定流程。
- 首期不提供 TSA 时间戳、OCSP、CRL、HSM/KMS、证书申请与轮换、电子印章制作与审批、审计台账或文件存储。
- 流式处理是有界堆内存处理，不是纯单向无磁盘处理。PDF 增量签名和 OFD ZIP 包访问需要临时磁盘空间；临时源文件在成功和异常路径均删除。

## 4. 模块入口

- `mango-infra-docsign-api`：`DocumentSignApi`、签章/验签命令、枚举、SPI 和结果 VO。
- `mango-infra-docsign-core`：PDF/OFD provider、密钥材料载入、CMS、证书信任和验签实现。
- `mango-infra-docsign-starter`：Spring Boot 自动配置。

坐标统一使用毫米、左上角原点，页码从 1 开始。

## 5. 接入方式

使用 Spring Boot 自动装配：

```xml
<dependency>
    <groupId>io.mango.infra.docsign</groupId>
    <artifactId>mango-infra-docsign-starter</artifactId>
</dependency>
```

只引用公共契约：

```xml
<dependency>
    <groupId>io.mango.infra.docsign</groupId>
    <artifactId>mango-infra-docsign-api</artifactId>
</dependency>
```

PDF 普通可见章流式示例：

```java
DocumentSignStreamResultVO result = documentSignApi.sign(DocumentSignCommand.builder()
        .format(DocumentSignFormat.PDF)
        .keyMaterial(new Pkcs12KeyMaterial(pkcs12Bytes, password, "signer"))
        .signatureAlgorithm(DocumentSignatureAlgorithm.SHA256_WITH_RSA_PSS)
        .signerName("Mango Signer")
        .reason("合同签署")
        .stamp(DocumentStampCommand.normal(1, 20, 30, 40, 40)
                .image(stampPng)
                .build())
        .build(), pdfInputStream, signedPdfOutputStream);
```

调用方保持输入、输出流的所有权，API 不会关闭它们。已有 `sign(DocumentSignCommand)` 与 `verify(DocumentVerifyCommand)` 继续用于小文件 `byte[]` 兼容调用，超过 `max-in-memory-size` 时会明确拒绝并提示使用流式入口。

`signatureAlgorithm` 使用受控枚举，未设置时默认为 `AUTO`：RSA 私钥选择 `SHA256_WITH_RSA`，SM2 `sm2p256v1` 私钥选择 `SM3_WITH_SM2`。PDF 可显式选择 `SHA256_WITH_RSA`、`SHA384_WITH_RSA`、`SHA512_WITH_RSA`、`SHA256_WITH_RSA_PSS` 或 `SM3_WITH_SM2`；OFD 标准签名只接受 `AUTO` 或 `SM3_WITH_SM2`。模块在写入文档前校验所选算法、私钥、叶子证书公钥和 SM2 曲线，组合不匹配时明确拒绝。普通 P-256 ECDSA 证书当前不属于支持范围。

OFD 原生电子印章将 `.ofdSeal(sesV4SealBytes)` 传给普通章或骑缝章命令；没有印章命令时生成 GB/T 35275 纯数字签名。

验签示例：

```java
DocumentVerifyResultVO verified = documentSignApi.verify(DocumentVerifyCommand.builder()
        .format(DocumentSignFormat.PDF)
        .trustStore(new TrustStoreMaterial("PKCS12", trustStoreBytes, password))
        .build(), signedPdfInputStream);
```

业务逻辑应先检查整体 `verified.valid()`，再按需展示 `signatures()` 中的分项原因。

## 6. 配置说明

配置前缀：`mango.docsign`。

```yaml
mango:
  docsign:
    enabled: true
    pdf-enabled: true
    ofd-enabled: true
    max-in-memory-size: 16MB
    max-document-size: 2GB
    temporary-directory: ${java.io.tmpdir}/mango-docsign
```

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用文档签章自动配置。 |
| `pdf-enabled` | `true` | 是否注册 PDF provider。 |
| `ofd-enabled` | `true` | 是否注册 OFD provider。 |
| `max-in-memory-size` | `16MB` | 兼容 `byte[]` 签名/验签入口允许的最大文档大小。 |
| `max-document-size` | `2GB` | 流式入口允许写入临时文件的最大源文档大小。 |
| `temporary-directory` | `${java.io.tmpdir}/mango-docsign` | PDF/OFD 随机访问使用的短期源文件目录。 |

配置只控制 provider 装配，不包含私钥、证书或信任库内容。

## 7. API 与扩展

- `DocumentSignApi.sign(command, input, output)`：流式签名首选入口，返回格式、输出长度和签名数量。
- `DocumentSignApi.verify(command, input)`：流式验证文档内全部签名。
- `DocumentSignApi.sign(command)`：小文件兼容入口，返回签名后的完整 `byte[]` 文档。
- `DocumentSignApi.verify(command)`：小文件兼容验签入口。
- `DocumentSignApi.supportedFormats()`：返回当前已装配格式。
- `IDocumentSignProvider`：扩展其它文档格式或替换实现的 SPI。

`SignatureValidationVO` 的主要结论：

| 字段 | 含义 |
|------|------|
| `cryptographicallyValid` | 签名结构和密码学验证是否通过。 |
| `documentIntegrityValid` | 受保护的文档内容是否未被篡改。 |
| `certificateTimeValid` | 签名证书在实际验签时刻是否有效；首期没有可信 TSA，不使用文档自报签署时间放宽证书有效期。 |
| `trusted` | 证书和电子印章是否被调用方信任库信任。 |
| `coversCurrentDocument` | 签名是否覆盖当前文档版本；PDF 较早的增量签名通常为 `false`。 |
| `valid` | 当前签名的失败闭合整体结论。 |

## 8. 数据与初始化

本模块没有数据库 migration、菜单、权限码、初始化数据或 Runner。文档输入输出由调用方通过流或小文件字节提供并自行持久化；流式处理期间只在配置的临时目录保存短期随机访问文件。

## 9. 管理入口

本模块没有 HTTP 接口和管理页面。调用方必须自行控制：

- 谁可以读取待签文件和签名结果。
- 谁可以调用指定证书或电子印章。
- 文件所属租户和业务对象是否匹配。
- 签名结果、验签结果和失败原因的审计留痕。

## 10. 快速开始

1. 由业务模块完成文件权限、租户和签章授权校验。
2. 从合规密钥设施或调用方安全上下文取得 PKCS#12；不要把真实密钥写入配置或日志。
3. 构造 PDF/OFD 签章命令，坐标使用毫米和左上角原点，并把文件输入流、持久化输出流传给流式签名入口。
4. 根据 `DocumentSignStreamResultVO` 记录输出长度和签名数量。
5. 验签时提供独立信任库，并以整体 `valid` 作为通过条件。

## 11. 问题排查

- 整体无效但密码学有效：检查是否提供信任库、证书链是否受信、证书时间是否有效。
- PDF 多签名中早期签名 `coversCurrentDocument=false`：它只覆盖当时的增量版本；应结合其密码学/完整性结果和最后一个签名判断。
- OFD 使用 RSA 失败：当前 OFD provider 只接受 SM2/SM3。
- 签名算法与证书不匹配：确认 PKCS#12 中的私钥和叶子证书均符合所选算法；SM2 必须使用 `sm2p256v1`，普通 ECDSA 不能替代 SM2。
- OFD 章不可生成：检查是否提供有效 SES v4 数据；普通 PNG 不能替代 OFD 原生电子印章结构。
- 文档签章后仍需判断吊销状态：首期不联网查询 OCSP/CRL，需要调用方或后续安全基础设施补齐。
- `byte[]` 入口提示超限：改用流式入口，或仅在业务确认小文件边界后调整 `max-in-memory-size`。
- 流式入口提示文档超限或临时文件失败：检查 `max-document-size`、`temporary-directory` 容量和进程读写权限。

## 12. 相关文档

- [文件处理基础能力](../mango-infra-fileproc/README.md)
- [密码基础能力](../mango-infra-crypto/README.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [STANDARD 交付记录](../../../mango-docs/plans/2026-08-20-docsign-delivery-record.md)
