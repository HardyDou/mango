# 标准交付记录

## 1. 元数据

- 任务 ID：DOCSIGN-20260820
- 任务名称：文档数字签章公共能力
- 交付模式：STANDARD
- 需求影响：L2 - 新增 PDF/OFD 文档签名、电子印章外观和验签公共契约，影响文档完整性与证书信任语义，但不接入现有业务流程。
- 方案风险：L2 - 新增隔离的可选基础设施模块；密码实现复用 PDFBox、Bouncy Castle 和 OFDRW，失败可通过移除 starter 依赖回退。
- 最终风险：L2
- 工作区决策：REUSE - 复用 `feat/fileproc-ofd-conversion` 对应的任务 worktree。

## 2. 目标与范围

- 目标：提供统一的 PDF/OFD 文档签名、可见印章、骑缝章和验签能力。
- 成功条件：PDF 支持自动选择或显式指定 RSA SHA-256/384/512、RSA-PSS SHA-256、SM2/SM3 CMS 签名，OFD 支持 SM2/SM3 数字签名和调用方提供的 SES v4 电子印章；算法、私钥和证书不匹配时拒绝签名；篡改、证书失效或不受信均不能返回整体有效；大文件签名和验签不把完整文档加载到堆内存。
- 处理范围：PKCS#12 私钥与证书链载入、受控签名算法选择与匹配校验、信任库载入、坐标签章、骑缝章、多签名、签名列表与验证结果、流式输入输出与磁盘随机访问、Spring Boot 自动装配、能力说明。
- 不处理范围：密钥托管、KMS/HSM、证书申请和轮换、电子印章制作和审批、TSA、OCSP/CRL、业务权限与文件存储。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| DSR-001 | `DocumentSignApi.sign` | PDF、PKCS#12、可选受控算法、章图和毫米坐标 | 生成带 CMS 增量签名的 PDF；默认按密钥选择 RSA/SHA-256 或 SM2/SM3，也可显式选择 RSA SHA-256/384/512、RSA-PSS SHA-256、SM2/SM3 | 私钥、叶子证书、SM2 曲线、算法、页码或图像不合法时明确拒绝，不产生固定成功结果 | 输出可由 PDFBox 读取，CMS OID 与选择一致，签名密码学有效，可见章存在 |
| DSR-002 | `DocumentSignApi.sign` | OFD、SM2 PKCS#12、可选 SES v4 电子印章和位置 | 生成 OFDRW 签名结构；无印章时为 GB/T 35275 数字签名，有印章时保留普通章或骑缝章位置 | OFD 使用 RSA、章位置缺少 SES 数据或签名结构不可追加时明确失败 | 输出可由 OFDRW 读取并验签，多签名可继续追加 |
| DSR-003 | `DocumentSignApi.verify` | 已签文档和信任库 | 返回每个签名的算法、签署者、密码学、完整性、证书有效性、信任和整体结论 | 无签名、篡改、证书过期、不受信或结构损坏均不能返回整体有效 | 受信且未篡改为有效；篡改和空信任库为无效 |
| DSR-004 | Spring Boot starter | 引入 starter，配置启用 | 自动注册 PDF/OFD provider 和统一 API | 关闭配置时不注册能力 | 装配测试覆盖启用和关闭 |
| DSR-005 | `DocumentSignApi` 流式入口 | PDF/OFD 输入流、签名输出流、大小和临时目录配置 | 分块写入临时源文件，PDF CMS 按 `ByteRange` 分块摘要，签名结果直接写调用方输出流；调用方流保持打开 | 超过总大小、临时目录不可写或文档损坏时明确失败并清理临时源文件 | 不调用整文档 `readAllBytes()`；25 页 PDF 和 12 页 OFD 骑缝签、流式验签通过；`byte[]` 超限被拒绝 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| ADR-001 | DSR-001~DSR-004 | 新建 `mango-infra-docsign` 的 api/core/starter 三层；不扩展只处理字符串的 `ISignService`，不把安全信任语义放入 fileproc | `mango/mango-infra/mango-infra-docsign/**` | 移除新模块及聚合/BOM 坐标 |
| ADR-002 | DSR-001~DSR-003 | API 坐标统一为毫米、左上角；输出验证结论拆成密码学、完整性、证书时间、信任和整体状态 | docsign api/core | 保留调用方输入，替换 provider 实现 |
| ADR-003 | DSR-001 | PDF 使用 PDFBox 3 增量签名与 Bouncy Castle `adbe.pkcs7.detached` CMS；公开算法只接受受控枚举，`AUTO` 保持 RSA/SHA-256 与 SM2/SM3 兼容默认值；写入前校验算法、私钥、叶子证书公钥及 SM2 曲线；可见章先写入受签页面内容，骑缝章按页切片 | docsign api、PDF provider | 调用方移除显式算法可回到 `AUTO`；移除 PDF provider 不影响 OFD provider |
| ADR-004 | DSR-002 | OFD 复用 OFDRW 2.4.0；SM2 数字签名使用 GB/T 35275，原生电子印章只消费外部合规 SES v4 数据，不在 Mango 生成印章主体 | OFD provider | 移除 OFD provider，不影响 PDF provider |
| ADR-005 | DSR-003 | 默认 fail-closed：缺少信任库时可报告密码学结果，但 `valid=false`；首期不联网执行 TSA、OCSP 或 CRL | 验签 provider | 调用方可继续使用分项结果，不改变签名文件 |
| ADR-006 | DSR-005 | 流式 API 为大文件首选入口；PDF/OFD 所需随机访问由受控临时文件承载，输出直接写调用方流。保留带上限的 `byte[]` 兼容入口 | docsign api/core/starter | 删除流式重载可回到小文件 API；不改变签名算法和文件格式 |

### ADR 摘要

- 状态：Accepted，用户于 2026-08-20 明确选择独立文档签章模块方案。
- 正向结果：文件转换、基础密码算法和文档签章职责分离；PDF/OFD 对业务暴露统一契约；验证结论不混淆完整性与信任。
- 负向结果：新增三个 Maven 制品；OFD RSA 不作为互操作签名交付；合规电子印章仍依赖外部制章和密钥设施。
- 替代方案：把 provider 放进 fileproc，因转换与证书信任耦合而拒绝；扩展 `ISignService`，因其字符串契约无法表达文档位置、证书链和多签名而拒绝。
- 流式替代方案：纯前向 `InputStream` 被拒绝，因为 PDF 增量签名和 OFD ZIP 包都需要随机访问；统一 `DocumentSource/DocumentTarget` 抽象暂不采用，避免在真实文件/流需求之外扩大公共契约。

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| DSI-001 | ADR-001、ADR-002 | 1 | docsign api 与 Maven 聚合/BOM | 契约可编译，敏感数组防御性复制 |
| DSI-002 | ADR-003 | 2 | docsign api 与 core PDF provider | `AUTO`、RSA SHA-256/384/512、RSA-PSS SHA-256、SM2/SM3、算法/证书不匹配拒绝、普通章、骑缝章、多签名和验签测试通过 |
| DSI-003 | ADR-004 | 3 | docsign core OFD provider | 数字签名、SES v4 位置、多签名和篡改检测测试通过 |
| DSI-004 | ADR-005 | 4 | 信任库和证书链验证 | 缺少信任、证书失效和篡改均 fail-closed |
| DSI-005 | ADR-001 | 5 | docsign starter | 条件装配测试通过 |
| DSI-006 | ADR-001~ADR-005 | 6 | 模块 README、能力地图、fileproc/crypto 边界说明 | 能力文档审计通过 |
| DSI-007 | ADR-006 | 7 | docsign api/core/starter、流式回归测试与能力说明 | 流式签名/验签有界使用堆内存，临时文件清理和大小限制测试通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| DSR-001 | M10 单元/组件测试 | `mvn -q -f mango/mango-infra/mango-infra-docsign/pom.xml test` | PASS（PDF 17/17） | PDF `AUTO`、RSA SHA-256/384/512、RSA-PSS SHA-256、SM2/SM3 均生成目标 CMS OID 并通过真实验签；算法/证书不匹配和普通 P-256 ECDSA 明确拒绝；普通章、骑缝章、25 页流式骑缝签、增量多签名、追加篡改、证书过期、无信任库和大小限制通过 |
| DSR-002 | M10 单元/组件测试 | `mvn -q -f mango/mango-infra/mango-infra-docsign/pom.xml test` | PASS（OFD 11/11） | OFD 显式 SM2/SM3、默认 `AUTO`、SES v4 普通章/骑缝章、12 页流式骑缝签、追加多签名、Content.xml 篡改、证书过期、RSA 密钥/显式 RSA 算法拒绝和大小限制通过 |
| DSR-003 | M10 单元/组件测试 | docsign core 全量测试 | PASS（28/28） | PDF/OFD 验签分项结果、算法 OID、完整性、证书信任和失败闭合均有真实物料测试 |
| DSR-004 | M11 Spring 装配测试 | `mvn -q -pl mango-infra/mango-infra-docsign/mango-infra-docsign-starter -am -Dtest=DocumentSignAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS（7/7） | 默认、总开关、PDF/OFD 独立开关、全部 provider 关闭和 ConditionalOnMissingBean 覆盖通过 |
| DSR-005 | M10 流式组件测试 | PDF/OFD provider 定向测试；生产代码搜索整文档读取 | PASS（PDF 3 项、OFD 2 项） | 输入流拒绝 `readAllBytes()` 仍可完成签名和验签；调用方流保持打开；临时文件成功/超限均清理；内存入口超限明确拒绝；PDF CMS 使用流式生成和 `ByteRange` 流式验签 |
| DSR-001、DSR-005 | M14 外部 PDF 互操作复核 | `pdfsig -nssdir <empty-nss-dir> -nocert mango-streaming-riding-seal-signed.pdf` | PASS | Poppler 识别 `adbe.pkcs7.detached`、SHA-256、完整 `Signed Ranges`，并报告 `Signature is Valid`；WPS macOS CLI 因应用沙箱无权读取工作树文件，未形成 WPS 内部签名面板验收结论 |
| DSR-001~DSR-004 | M09 静态与构建验证 | `mvn -q -f mango/mango-infra/mango-infra-docsign/pom.xml verify`；PMO 文档/测试审计 | PASS（35/35） | docsign 聚合模块 verify、`git diff --check`、测试质量、后端测试物料、模块 README、README source facts 审计通过；PDF 17、OFD 11、Starter 7 |

## 7. 例外与剩余风险

- TSA、OCSP/CRL 和 HSM/KMS 未进入首期；签名时间来自本机，吊销状态不联网判断。
- OFD 原生电子印章的制作与法律效力取决于外部合规制章系统、证书和设备；Mango 仅消费并验证其密码结构，不自行签发印章。
- PDF 骑缝章以受签页面内容图像实现；不同阅读器均能看到外观，但它不是多个独立 PDF 签名域。
- 流式模式仍需要最多接近源文档大小的临时磁盘空间，OFDReader 解包还会产生 OFDRW 工作目录；部署时必须按并发量预留临时空间。当前自动化证明内部 PDFBox/OFDRW/Bouncy Castle 验签兼容，WPS 等桌面软件的证书信任展示仍需使用目标生产证书和客户端版本做人工兼容验收。
