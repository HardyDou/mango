# Aspose License Compatible Versions 交付契约

## 1. 目标

将 Mango 使用的 Aspose Java 依赖调整为与现有 `Aspose.Total for Java` 授权兼容的版本，并验证 license 可被相关产品组件成功加载。

## 2. 范围

- 选择不晚于授权升级截止日 `2020-04-03` 的 Aspose 依赖版本。
- 更新 Mango 根 POM 与 file-preview engine 中的 Aspose 版本声明。
- 按用户要求将现有授权文件作为包内 classpath 资源放入 fileproc Aspose 默认加载路径。
- 补充/更新 Aspose license 资源说明。
- 将选定的 Aspose 20.3 物料发布到内部 Nexus，供业务项目和 Mango 构建解析。
- 执行 license 加载验证和受影响 Maven 模块验证。

## 3. 不做什么

- 不升级或改造 file-preview 的转换业务流程。
- 不新增新的 infra-aspose 模块；本次沿用现有 `mango-infra-fileproc` Aspose 能力。
- 不变更 API、数据库、菜单、页面或权限。
- 不发布 Mango 自身正式版本制品；本次只发布已选定的 Aspose 第三方依赖物料到内部 Nexus。

## 4. 设计输入

- 用户提供的 Aspose license 文件路径。
- 用户要求：选择合适版本下载并依赖使用。
- 用户补充验证：本地 `aspose-pdf-21.3.jar` 应尝试验证是否可用。
- 用户要求：将 Aspose 授权直接放到 infra-aspose 包内；当前代码中对应能力位于 `mango-infra-fileproc`。
- 当前 license 元数据：`Aspose.Total for Java`，免费升级截止日 `2020-04-03`。

## 5. 设计说明

### 5.1 影响模块

- `mango/pom.xml`：统一管理 Words、Cells、Slides、PDF、Imaging 版本。
- `mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml`：管理 CAD 版本。
- `mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/`：放置默认 classpath license 资源和说明。
- 内部 Nexus `maven-releases`：保存 Aspose 20.3 依赖物料。

### 5.2 接口变化

无 HTTP API、Java API、Feign 契约变化。

### 5.3 数据变化

无数据库结构和初始化数据变化。

### 5.4 菜单/页面/权限变化

无菜单、页面、权限变化。

### 5.5 测试范围

- 使用选定版本的 Aspose jars 执行 `License#setLicense` 加载验证。
- 对用户提供的本地 `aspose-pdf-21.3.jar` 执行 `License#setLicense` 验证，记录是否兼容。
- 执行受影响模块 Maven 编译/测试，确认依赖版本可解析且源码可编译。
- 执行 PMO delivery-contract-check。

## 6. 风险与限制

- Aspose license 属于商业敏感文件。用户已明确要求内置到包内，本次作为 PMO 安全例外记录；输出和日志不得打印 license 内容。
- 旧版 Aspose 与当前 JDK、字体、图形库或文档格式兼容性弱于新版；本次只验证 license 加载和模块编译，不宣称覆盖所有文档转换格式。
- 如果某个 Aspose 产品在授权截止日前不存在可解析 Maven 坐标，必须记录为阻塞或例外，不能伪装完成。

## 7. 交付台账

交付台账见 `mango-docs/plans/2026-05-27-aspose-license-compatible-versions-ledger.md`。
