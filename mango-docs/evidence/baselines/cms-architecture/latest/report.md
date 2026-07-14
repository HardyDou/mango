# CMS 架构债务治理基线对比报告

## 1. 当前结论

CMS 测试从原有 14 条补强到改造前基线 35 条，架构治理后同一组 35 条全部保持通过；数据
初始化分层又新增 7 条有针对性的迁移、资源声明和资源处理器契约测试，最终为 42/42，
失败、错误、跳过均为 0。新增用例集中保护内容状态机、租户与数据权限、公开可见性、文件
引用授权、Java API 指纹、关键 HTTP 协议、纯 DDL 和显式 Demo，不以重复参数组合堆数量。

改造前生产提交：`747f8cbde`；任务分支：`refactor/cms-architecture-debt`。本报告只声明 CMS
四个直接模块及其定向架构验证结果，不代表全仓检查。

## 2. 同一测试入口

```bash
mvn -f mango/mango-platform/mango-cms/pom.xml clean test
```

| 模块 | 原有用例 | 补强后的改造前基线 | 改造后 | 失败/错误/跳过 |
|---|---:|---:|---:|---:|
| mango-cms-api | 0 | 0 | 0 | 0/0/0 |
| mango-cms-core | 10 | 25 | 27 | 0/0/0 |
| mango-cms-starter | 4 | 10 | 15 | 0/0/0 |
| mango-cms-starter-remote | 0 | 0 | 0 | 0/0/0 |
| 合计 | 14 | 35 | 42 | 0/0/0 |

新增 21 条测试的保护边界：

- `CmsAdminServiceSecurityTest` 新增 6 条：提交、审核通过、驳回、已发布删除保护和跨站点发布；
- `CmsSiteServiceBehaviorTest` 新增 9 条：匿名解析、唯一站点、公开内容状态/租户/发布关系、文件授权与上下文恢复、公开分页参数；
- `CmsApiSurfaceContractTest` 新增 2 条：完整管理 API 与公开 API 的方法、泛型、参数和校验注解 SHA-256 指纹；
- `CmsControllerContractTest` 新增 2 条：Controller 完整实现 API，审核/解析/文件关键路由与权限保持；
- `CmsPublicFileHttpContractTest` 新增 2 条：二进制状态、Content-Type、Content-Length、Content-Disposition 与 body。

测试质量门禁：

```text
node mango-pmo/tools/test-quality-check.mjs --base origin/main
Test quality PASS: 8 file(s)
```

改造后新增 7 条测试只覆盖新引入的风险边界：V1 单迁移且纯 DDL、正式与 Demo 资源隔离、
71 条 Demo 声明自包含、资源处理器新增/校验/禁用行为，以及文件 ID 和日期格式约束。

## 3. 改造前 Java 与 HTTP 契约

| 契约 | 改造前证据 |
|---|---|
| `CmsAdminApi` 完整指纹 | `a932efdaa71a3ac3e16014ca0db9984d3921f7f96bd5f4f6e4e51ac9a1edd473` |
| `CmsSiteApi` 完整指纹 | `f3b47db53ba377390133ca7186b6fc00e28d448b4426a010ce4fbfb0e598652b` |
| 公开站点解析 | `GET /cms/open/sites/resolve?domain=127.0.0.1%3A5193`，HTTP 200、业务 code 200、`demo/演示站点/ENABLED` |
| 关键管理路由 | `POST /cms/contents/approve`，权限 `cms:content:approve` |
| 公开文件路由 | `GET /cms/open/files/public-preview`，inline 二进制响应 |

改造后两份 API 指纹保持不变；既有 HTTP 路径/verb/参数/返回泛型、权限码和失败消息均由
契约测试保护。公开文件实现迁移到专用 endpoint，但请求路径与二进制响应协议不变。

## 4. 改造前新库与 Flyway 基线

专属 workspace：`mango_182`；后端端口 `18182`；数据库
`mango_dev_mango_cms_architecture_debt_182`。数据库由 workspace 新建，不使用共享业务库。

当前 CMS Flyway history：baseline 0，加 V1、V2、V3、V4、V5、V6、V8、V9、V10，全部成功。
其中 V4-V10 包含菜单、Demo 和跨模块文件引用 DML，属于本次要移出的历史债务。

| 检查项 | 改造前结果 |
|---|---:|
| `cms_*` 表 | 12 |
| schema 列与索引规范化指纹 | `aefc78ae649da846a2710650c191903862aeafa9957fc7d52f92fb32e1737518` |
| cms_site | 3 |
| cms_site_category | 13 |
| cms_content | 11 |
| cms_content_publish | 11 |
| cms_navigation | 22 |
| cms_banner | 1 |
| cms_advertisement | 4 |
| cms_ad_delivery | 3 |

最终纯 DDL V1 必须复现 12 张表的列、主键、索引和约束指纹；Demo 数量不要求留在 Flyway，
而是由显式 Demo Resource 产生。正式默认启动必须是零 Demo。

改造后在全新数据库执行单个 V1：Flyway history 仅 baseline 0 与 V1；12 张 CMS 表全部为空。
与改造前数据库逐表比较列和索引完全一致，规范化 schema 指纹仍为
`aefc78ae649da846a2710650c191903862aeafa9957fc7d52f92fb32e1737518`。V2-V10 已折叠，V1
只含 DDL；菜单位于正式资源目录，CMS 当前没有必须初始化的业务数据。

## 5. 改造前公开消费基线

使用当前 Flyway 自动写入的 `demo` 站点，服务健康检查为 HTTP 200/UP：

| 接口 | HTTP/业务结果 | 改造前数量 |
|---|---|---:|
| sites/resolve | 200/成功 | 唯一站点 `2070000000000000001` |
| site-categories/tree | 200/成功 | 0 |
| navigations/list | 200/成功 | 9 |
| banners/list | 200/成功 | 1 |
| advertisements/list | 200/成功 | 1 |
| contents/page | 200/成功 | 3 |

栏目树为 0 是已确认的旧 Demo 数据缺陷：SQL 写入 `VISIBLE`，服务与前端契约使用
`ENABLED`。治理时不改变公开筛选逻辑，Demo Resource 改为合法 `ENABLED`，因此显式 Demo
环境的栏目树应恢复为非空；这是初始化数据纠错，不作为业务逻辑差异隐藏。

## 6. 改造后新库与运行验收

正式默认启动和显式 Demo 启动均使用新建 workspace 数据库，并通过 Mango CLI 启动：

| 检查项 | 改造后结果 |
|---|---|
| 默认启动 | HTTP 200/UP；12 张 CMS 表均为 0；无 CMS Demo 资源 |
| 显式 Demo 资源 | 9 类、71 条注册与同步均成功 |
| Demo 行数 | site 3、setting 3、category 13、content 11、publish 11、navigation 22、banner 1、advertisement 4、delivery 3 |
| Demo 合法性 | 13 个栏目全部 `ENABLED`；所有演示文件 ID 均为空 |
| Demo 前端冒烟 | `mango-site-demo-app` HTTP 200，HTML 标题/挂载点与 `/api` 代理均可用 |

公开接口实测：

| 接口 | HTTP/业务结果 | 改造后数量 |
|---|---|---:|
| sites/resolve | 200/200 | 唯一站点 `2070000000000000001` |
| site-categories/tree | 200/200 | 5 个根栏目 |
| navigations/list | 200/200 | 9 |
| banners/list | 200/200 | 1 |
| advertisements/list | 200/200 | 1 |
| contents/page | 200/200 | 3 |

栏目从 0 恢复为 5 个根节点是旧 Demo 状态值纠错；服务筛选逻辑和 API 契约未改变。

定向架构验证覆盖 CMS API、core、starter、remote 和 architecture-verification：依赖违规、
ArchUnit、PMD、新增阻断问题均为 0。治理前记录的 1,126 个 CMS 正式架构问题全部归零；
静态门禁的 110 个输出均属于基线，新增问题为 0。

当前会话未提供 in-app browser 控制入口，因此没有取得浏览器截图和 console/network 证据；
该项登记为 UI 例外，不将前端 HTTP 可访问冒充完整浏览器 UI 验收。后端真实数据库、公开
API、前端 HTML 与 `/api` 代理结果不受此例外影响。

## 7. 改造后停止条件

- 35 条基线测试被删除、跳过、弱化或不再从同一入口执行；
- Java API 指纹、HTTP 路径/verb/参数/返回、权限或错误 code/message 出现未批准差异；
- 内容状态、公开资格、租户上下文或文件引用授权与用例不一致；
- 新 V1 不是纯 DDL，12 表 schema 指纹不一致，或正式默认环境出现 Demo；
- CMS 四个直接模块正式架构债务未从 1,126 降至 0；
- 新库服务健康、公开接口或定向 Demo 页面验收失败。
