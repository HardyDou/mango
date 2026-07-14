# CMS 架构债务治理基线对比报告

## 1. 当前结论

生产代码尚未修改时，CMS 测试从原有 14 条补强到 35 条，并使用同一个模块入口建立
改造前基线：35/35 通过，失败、错误、跳过均为 0。新增用例集中保护内容状态机、租户与
数据权限、公开可见性、文件引用授权、Java API 指纹、关键 HTTP 路径与二进制响应头，
不是以重复参数组合堆数量。

改造前生产提交：`747f8cbde`；任务分支：`refactor/cms-architecture-debt`；改造后结果待实现
完成后用同一入口回填。本报告只声明 CMS 直接模块结果，不代表全仓检查。

## 2. 同一测试入口

```bash
mvn -f mango/mango-platform/mango-cms/pom.xml clean test
```

| 模块 | 原有用例 | 补强后的改造前基线 | 改造后 | 失败/错误/跳过 |
|---|---:|---:|---:|---:|
| mango-cms-api | 0 | 0 | 待执行 | 0/0/0 |
| mango-cms-core | 10 | 25 | 待执行 | 0/0/0 |
| mango-cms-starter | 4 | 10 | 待执行 | 0/0/0 |
| mango-cms-starter-remote | 0 | 0 | 待执行 | 0/0/0 |
| 合计 | 14 | 35 | 待执行 | 0/0/0 |

新增 21 条测试的保护边界：

- `CmsAdminServiceSecurityTest` 新增 6 条：提交、审核通过、驳回、已发布删除保护和跨站点发布；
- `CmsSiteServiceBehaviorTest` 新增 9 条：匿名解析、唯一站点、公开内容状态/租户/发布关系、文件授权与上下文恢复、公开分页参数；
- `CmsApiSurfaceContractTest` 新增 2 条：完整管理 API 与公开 API 的方法、泛型、参数和校验注解 SHA-256 指纹；
- `CmsControllerContractTest` 新增 2 条：Controller 完整实现 API，审核/解析/文件关键路由与权限保持；
- `CmsPublicFileHttpContractTest` 新增 2 条：二进制状态、Content-Type、Content-Length、Content-Disposition 与 body。

测试质量门禁：

```text
node mango-pmo/tools/test-quality-check.mjs --base origin/main
Test quality PASS: 5 file(s)
```

## 3. 改造前 Java 与 HTTP 契约

| 契约 | 改造前证据 |
|---|---|
| `CmsAdminApi` 完整指纹 | `a932efdaa71a3ac3e16014ca0db9984d3921f7f96bd5f4f6e4e51ac9a1edd473` |
| `CmsSiteApi` 完整指纹 | `f3b47db53ba377390133ca7186b6fc00e28d448b4426a010ce4fbfb0e598652b` |
| 公开站点解析 | `GET /cms/open/sites/resolve?domain=127.0.0.1%3A5193`，HTTP 200、业务 code 200、`demo/演示站点/ENABLED` |
| 关键管理路由 | `POST /cms/contents/approve`，权限 `cms:content:approve` |
| 公开文件路由 | `GET /cms/open/files/public-preview`，inline 二进制响应 |

改造后必须保持两份 API 指纹、所有既有 HTTP 路径/verb/参数/返回泛型、权限码和失败消息；
文件入口允许迁移到专用 endpoint，但请求和响应协议不得变化。

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

## 6. 改造后停止条件

- 35 条基线测试被删除、跳过、弱化或不再从同一入口执行；
- Java API 指纹、HTTP 路径/verb/参数/返回、权限或错误 code/message 出现未批准差异；
- 内容状态、公开资格、租户上下文或文件引用授权与用例不一致；
- 新 V1 不是纯 DDL，12 表 schema 指纹不一致，或正式默认环境出现 Demo；
- CMS 四个直接模块正式架构债务未从 1,126 降至 0；
- 新库服务健康、公开接口或定向 Demo 页面验收失败。
