# Infra IP Location 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-ip-location-api/core/starter`。
- 当前源码消费者：`mango-auth-starter` 登录日志、`mango-system-core/starter` 操作日志。
- 行为：IP 字面量分类、noop/ip2region 解析、缓存、XDB 资源加载、Spring 自动配置和真实 HTTP 调用。
- 边界：本模块无数据库、Flyway、初始化/demo 数据、菜单或浏览器页面；XDB 是部署资源，不是数据库初始化数据。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | Maven 构建通过，但测试数为 0 |
| 服务入口 | 无 Spring 上下文或真实 HTTP 验证 |
| 地址语义 | `InetAddress.getByName` 接受主机名和缩写地址，并可能触发 DNS |
| 缓存契约 | 缓存并返回同一可变对象；delegate 收到未标准化 IP；null/异常可穿透接口边界 |
| XDB 加载 | 所有资源强制 `Resource#getFile()`，可执行 JAR 内 `classpath:` XDB 无法加载 |
| 解析状态 | XDB 返回 null/空字符串仍标记为 resolved |
| 并发 | 共享单例直接并发调用官方声明非线程安全的 `Searcher` |
| 静态债务 | Checkstyle 31 条、SpotBugs 2 条 |

## 3. 缺陷红灯

生产代码修改前建立同一测试集，稳定暴露 9 个失败事实：主机名/畸形地址被当作有效 IP，缓存标准化、快照隔离、null 与异常兜底不符合接口约定，空 XDB 结果被标记为成功。真实 XDB Searcher 用最小合法 XDB 字节执行，不 mock 被测解析器。

## 4. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| DNS 与宽松地址解析 | IPv4 严格校验四段十进制；IPv6 先验证字面量字符；主机名不再进入 DNS | 合法 IPv4/IPv6、公网/私网分类保持原接口 |
| 可变缓存污染 | key 和 delegate 入参统一 trim；缓存写入和命中均返回独立快照 | `IpLocation` 原 getter/setter、TTL 和最大容量配置不变 |
| 解析兜底 | delegate null/运行时异常统一返回 unresolved | 保持“归属地失败不能阻断主业务”接口合同 |
| XDB 资源 | 非文件资源通过 InputStream 内容加载；文件继续支持 buffer/vector/file-only | `file:` 配置行为不变，新增可执行 JAR 内 `classpath:` 可用性 |
| 状态与并发 | 仅非空 region 标记 resolved；共享 Searcher 调用串行保护 | 结果字段、source 和关闭语义不变 |
| API 架构 | API 类型声明本地能力合同，API 显式依赖 `mango-common` | 未引入远程协议或业务依赖 |
| 静态债务 | Checkstyle 31→0，SpotBugs 2→0 | Spring 嵌套配置可变性使用有理由的定向抑制 |

## 5. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 执行入口 | 状态 |
|---|---|---|---|---|---|
| TC-IP-001 | P0 | 单元 | IPv4/IPv6、公网/内网、主机名和畸形地址 | `IpAddressClassifierTest` | AUTOMATED |
| TC-IP-002 | P0 | 单元 | 缓存标准化、快照隔离、TTL、null/异常兜底 | `CachingIpLocationResolverTest` | AUTOMATED |
| TC-IP-003 | P0 | 组件 | 真实 Searcher 读取合法 XDB、空结果和私网短路 | `Ip2RegionXdbLocationResolverTest` | AUTOMATED |
| TC-IP-004 | P0 | 接口 | disabled、missing、fail-fast、自定义 Bean、非文件 XDB | `IpLocationAutoConfigurationTest` | AUTOMATED |
| TC-IP-005 | P0 | 入口流程 | 随机端口 Tomcat 通过自动配置加载真实 XDB，HTTP 返回完整归属地 | `IpLocationHttpFlowTest` | AUTOMATED |
| TC-IP-006 | P1 | 单元 | 结果空值、展示文本和全字段独立快照 | `IpLocationTest` | AUTOMATED |

## 6. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前基线 | API/Core/Starter 定向 Maven test | 0 个测试，构建成功 | INSUFFICIENT |
| 缺陷红灯 | 新增测试运行于旧实现 | 9 个稳定失败 | DEFECT CONFIRMED |
| 治理后回归 | API/Core/Starter 同一套件 | 31/31，fail/error/skip 0 | PASS |
| 入口流程 | `IpLocationHttpFlowTest`，标签 `flow` + `infra-ip-location` | 随机 Tomcat 端口、真实 HTTP 与真实 XDB 1/1 | PASS |
| 当前源码契约 | 当前 IP 三模块、Captcha API、Auth Starter、System Core/Starter 同 reactor 编译 | PASS | PASS |
| 正式架构 | IP 三模块 + `mango-architecture-verification` partial reactor | dependency、ArchUnit、PMD 7、blocking 均 0 | PASS |
| 直接静态 | Checkstyle、SpotBugs | 0/0 | PASS |
| 测试质量 | `test-quality-check`、Mockito changed-only audit | 7 个测试资产 PASS；block=0、warn=0 | PASS |

## 7. Issue #522 防回归

第一次只加入 Auth 当前源码消费者时，构建读取了公共 Maven 缓存中的旧 Captcha API，出现与当前 Auth 源码不匹配的编译错误。这不是 IP 模块缺陷，但证明单独编译消费者仍可能产生假结论。最终验证显式把当前 Captcha API、当前 IP 生产者、Auth/System 消费者放入同一 reactor，验证通过；未通过手改本地 JAR 或跳过消费者绕开。

## 8. 数据与未验证项

| 项目 | 结论 |
|---|---|
| 数据库/Flyway/init/demo | N/A |
| XDB | 测试运行时生成最小合法文件并在结束后删除；生产数据不入库、不入 Git |
| 浏览器 UI | N/A；公共产品边界是 Java 能力和服务 HTTP 消费链 |
| 全仓测试 | 未执行；按要求只验证 IP 模块、真实入口和当前直接消费者 |

## 9. 风险分级

- 需求影响：L2。错误会造成日志归属地不准、DNS 阻塞或并发解析不稳定，但不直接修改业务数据。
- 方案风险：L2。保持公开解析接口与配置项，新增严格输入、快照隔离和资源兼容；可按单提交回退。
- 最终风险：L2。由旧实现红灯、同一回归集、真实 XDB/HTTP、当前源码消费者和静态架构门禁共同覆盖。
