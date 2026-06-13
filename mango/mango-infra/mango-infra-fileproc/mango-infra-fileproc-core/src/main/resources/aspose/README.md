# Mango Fileproc Aspose License Resource

## 1. 能力定位

本目录保存 `mango-infra-fileproc` 默认 Aspose 授权资源，供文档转换、PDF 处理、图片转换等 fileproc 能力加载 Aspose license。

## 2. 适用场景

- 使用 `mango-infra-fileproc` 内置 Aspose providers。
- 需要从 classpath 默认加载 `aspose/license.xml`。
- 需要说明外部 license 覆盖方式。

## 3. 不适用场景

- 不提供文件处理 API 本身。
- 不负责 Aspose license 采购、续期或法律合规判断。
- 不替代 fileproc 模块总 README。

## 4. 模块边界

本 README 是资源级说明。实际转换能力位于 `mango-infra-fileproc-api`、`mango-infra-fileproc-core` 和 `mango-infra-fileproc-starter`。

## 5. 接入方式

默认配置会从以下路径加载授权文件：

```text
classpath:/aspose/license.xml
```

也可以通过配置项指定外部文件或目录。

## 6. 配置项

配置项：

- `mango.fileproc.aspose.license-location`
- `mango.fileproc.aspose.words-license-location`
- `mango.fileproc.aspose.cells-license-location`
- `mango.fileproc.aspose.slides-license-location`
- `mango.fileproc.aspose.pdf-license-location`
- `mango.fileproc.aspose.imaging-license-location`

相关测试还覆盖 `mango.fileproc.aspose.enabled` 和若干 `mango.fileproc.convert.*-enabled` 开关。

## 7. 对外接口 / 扩展点

- `AsposeLicenseApi`
- `DefaultAsposeLicenseApi`
- `AsposeProduct`
- 转换 provider 会按产品读取 license 内容，例如 PDF、IMAGING、WORDS、CELLS、SLIDES。

## 8. 数据库 / 初始化数据

本目录不包含数据库 migration。`license.xml` 是 classpath 授权资源，生产使用前需要确认授权来源、有效期和合规范围。

## 9. 菜单 / 权限 / 租户

本资源不提供菜单、权限资源或租户数据。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-fileproc -am test
```

代表性测试入口包括 `ConvertAutoConfigurationTest`、`RenderAutoConfigurationTest` 和 fileproc core 转换/渲染测试。

## 11. 业务接入最小闭环

默认授权从 `classpath:/aspose/license.xml` 加载；需要替换时配置通用 `license-location`，如不同产品线使用不同授权，再配置 words、cells、slides、pdf、imaging 的专用 license location。专用配置优先用于对应产品线。

验收断言区分两层：Bean 装配成功只证明 provider 可用，真实转换/渲染样本且无水印才证明 Aspose 授权生效。禁用 `mango.fileproc.aspose.enabled=false` 后，依赖 Aspose 的 provider 不应被装配。

## 12. 常见问题

- 转换结果带水印时先检查 license 是否加载成功。
- 外部 license 路径应使用宿主应用可读取的文件或目录。
- 禁用 Aspose 后，依赖 Aspose 的转换 provider 不应被装配。

## 13. 关联 PMO 规则

- [后端模块规范](../../../../../../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../../../../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../../../../../../mango-docs/capabilities/README.md)
