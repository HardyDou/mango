# Mango Tools

## 1. 概览
`mango-tools` 提供 Mango 后端开发期工具，当前核心是 `mango-maven-plugin`。它用于质量检查、模块脚手架、CRUD 脚手架和权限资源生成。

主要使用者是 Mango 维护者、业务模块开发者、CI 门禁和 AI Agent。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| PR 需要执行 Mango 后端质量检查 | Maven 依赖 / HTTP API / Java API |
| 新增平台模块或业务模块时生成标准目录和基础文件 | Maven 依赖 / HTTP API / Java API |
| 快速生成 CRUD API、实体、Mapper、Service、Controller 等脚手架 | Maven 依赖 / HTTP API / Java API |
| 根据接口或资源生成权限数据草稿 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- PR 需要执行 Mango 后端质量检查。
- 新增平台模块或业务模块时生成标准目录和基础文件。
- 快速生成 CRUD API、实体、Mapper、Service、Controller 等脚手架。
- 根据接口或资源生成权限数据草稿。

## 4. 边界说明
- 不作为业务运行时依赖。
- 不替代真实业务建模、权限设计、README 和测试。
- 不保证生成代码可直接生产发布；生成后必须人工补齐业务逻辑和验收。

## 5. 模块组成
- `mango-tools`：Maven 聚合模块。
- `mango-maven-plugin`：Maven 插件，goalPrefix 为 `mango`。
- 插件代码读取项目源码、migration、报告文件和 Git 变更，不写运行时数据库。

## 6. 接入方式
在 Mango reactor 中直接调用：

```bash
mvn -f mango/pom.xml mango:check
```

或者在模块构建中使用插件 goal：

```bash
mvn -f mango/pom.xml mango:gen-module
mvn -f mango/pom.xml mango:gen-crud
mvn -f mango/pom.xml mango:gen-permission
```

具体参数以对应 Mojo 源码和 PMO 规则为准。

## 7. 配置说明
`mango:check` 常用参数：

| 参数 | 示例 | 含义 |
|------|------|------|
| `rule` | `-Drule=all` | 检查规则范围。 |
| `output` | `-Doutput=json` | 输出格式。 |
| `reportFile` | `-DreportFile=target/mango-check-report.json` | JSON 报告路径。 |
| `mango.check.gate` | `all` 或 `no-new-violations` | `all` 阻断所有问题；`no-new-violations` 只阻断新增问题。 |
| `mango.check.changedFiles` | `path1,path2` | 显式指定变更文件。 |
| `mango.check.baseRef` | `origin/main` | 未传 `changedFiles` 时用 Git diff 解析变更。 |
| `mango.check.baselineFile` | `target/baseline.json` | 存量问题基线报告。 |
| `mango.check.staticFailurePolicy` | `block` 或 `report` | 静态分析委托失败时阻断或只报告。 |

PR 检查推荐命令：

```bash
mvn -f mango/pom.xml mango:check -Drule=all \
  -Dmango.check.gate=no-new-violations \
  -Dmango.check.baseRef=origin/main \
  -Doutput=json \
  -DreportFile=target/mango-check-report.json
```

## 8. API 与扩展
当前 Maven goals：

| Goal | Mojo | 用途 |
|------|------|------|
| `mango:check` | `CheckMojo` | 执行 Mango 后端质量检查和 PR 门禁。 |
| `mango:gen-module` | `GenModuleMojo` | 生成模块脚手架。 |
| `mango:gen-crud` | `GenCrudMojo` | 生成 CRUD 脚手架。 |
| `mango:gen-permission` | `GenPermissionMojo` | 生成权限资源草稿。 |

`CheckMojo` 当前仍较大，复杂度偏高，是既有技术债；后续拆分时应保持参数兼容和报告格式兼容。

## 9. 数据与初始化
无生产数据库。工具可能读取 migration 和权限资源文件，但不直接连接生产库，也不授予运行时权限。

## 10. 管理入口
工具可生成或检查菜单、权限、API 资源相关文件；真正的菜单入库、角色授权和租户隔离仍由 `mango-authorization`、Flyway migration 和业务初始化流程负责。

## 11. 快速开始
1. 使用 `gen-module` 或 `gen-crud` 生成初稿。
2. 补齐业务模型、Controller 权限、migration、README、能力地图和测试。
3. 执行 `mango:check`。
4. 检查生成文件是否符合 PMO 模板和模块边界。

## 12. 问题排查
- `mango:check` 报存量问题：PR 模式使用 `no-new-violations` 和 baseline，但不能把新增问题放进 baseline。
- 生成代码编译不过：脚手架只提供结构，业务字段、依赖和 mapper 仍要补齐。
- 生成权限后页面仍无按钮：还需要菜单资源入库、角色授权和前端按钮权限接入。

## 13. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [模块菜单规范](../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
