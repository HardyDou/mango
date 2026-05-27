# Aspose License Compatible Versions 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| ASP-001 | 用户要求 | 选择与现有 license 兼容的 Aspose 版本并下载使用 | 使用授权升级截止日 `2020-04-03` 前的 Maven 20.3 版本，并以 license 加载实测为准 | `mango/pom.xml`、`mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml` | Aspose 20.3 license probe 全部通过 | DONE | `words 20.3 OK`、`cells 20.3 OK`、`slides 20.3 OK`、`pdf 20.3 OK`、`imaging 20.3 OK`、`cad 20.3 OK` |
| ASP-002 | 用户要求 | 将 Aspose license 放入包内默认资源 | 沿用现有 `classpath:/aspose/license.xml` 默认加载路径，放入 `mango-infra-fileproc-core` 资源目录 | `mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/license.xml` | Maven resources/编译验证；不打印 license 内容 | DONE | `mvn -U -pl mango-infra/mango-infra-fileproc/mango-infra-fileproc-core -am test` 通过 |
| ASP-003 | 用户要求 | 验证 license 是否 OK | 对 Words、Cells、Slides、PDF、Imaging、CAD 分别调用 `License#setLicense` | 本地验证命令输出 | license probe 返回成功 | DONE | 20.3 六个产品 `License#setLicense` 全部返回 OK |
| ASP-004 | PMO | 确认受影响模块可构建 | 对 fileproc core 和 file-preview engine 执行 Maven 验证 | Maven 命令输出 | Maven 命令成功或记录明确阻塞 | DONE | `mvn -U -pl mango-infra/mango-infra-fileproc/mango-infra-fileproc-core -am test` 通过；`mvn -U -pl mango-platform/mango-file-preview/mango-file-preview-engine -am -DskipTests compile` 通过 |
| ASP-005 | PMO 安全例外 | 商业 license 文件按用户要求内置到包中 | 明确记录用户要求和风险，不在日志/文档中暴露 license 内容 | 本计划与台账 | 台账状态为 EXCEPTION 并说明用户确认依据 | EXCEPTION | 用户明确要求“直接放到 infra-aspose 的包里面吧”；交付过程未打印 license 内容 |
| ASP-006 | 用户要求 | 发布所需 Aspose 物料到内部仓库 | 将选定的 Aspose 20.3 官方 jar 与 Words 必需附属 classifier 发布到内部 Nexus `maven-releases` | 内部 Nexus `maven-releases` | `maven-public` 可解析相关 jar | DONE | 已发布并以 HTTP 200 验证 main jars 可访问 |
| ASP-007 | 用户要求 | 验证用户提供的本地 `aspose-pdf-21.3.jar` 是否可用 | 直接使用该 jar 调用 `com.aspose.pdf.License#setLicense`；先复现 locale 问题，再切换 `Locale.CHINA` 验证授权日期 | 本地 `aspose-pdf-21.3.jar` 验证输出 | 明确兼容或不兼容结论 | EXCEPTION | `aspose-pdf-21.3.jar` 发布于 `2021-03-17`，超过 license 免费升级截止日 `2020-04-03`，Aspose 返回授权拒绝 |
