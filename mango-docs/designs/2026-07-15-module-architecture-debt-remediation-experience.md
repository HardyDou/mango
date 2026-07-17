# 模块历史债务治理经验总结

## 1. 背景

本文记录 Payment、CMS、Workflow、Notice、Org、System、Authorization、Resource、File Preview 和 Template 治理暴露的真实失败模式，作为后续模块排序、方案设计和验收的经验输入。长期开发、数据库、测试和交付约束仍以 `mango-pmo` 为唯一规范源，本文不替代规范。

## 2. 已发生的问题类型

| 类型 | 已发生问题 | 为什么旧检查没有发现 | 后续治理关注点 |
|---|---|---|---|
| 构建与发布物污染 | 源码正确，但旧 `target/classes` 被打进 JAR；同一模块出现重复 V1 或旧 migration | 只看源码和本机缓存，没有从仓库重新下载并检查发布物 | clean 构建；发布后重新下载；核对 JAR 文件清单、migration 内容和 SHA-256 |
| Flyway 历史链损坏 | 新 V1 已包含最终字段，后续 V2 又重复加列/索引；空库启动失败 | 单元测试使用 H2 fixture 或关闭 Flyway，没有真实执行发布物迁移链 | 每个模块使用独立全新 MySQL；启动应用；检查 `flyway_schema_history` 和最终 schema |
| DDL、必需数据和 Demo 混杂 | Flyway 同时建表、写基础角色、演示租户、菜单和运行态数据；跨模块 migration 修改其它模块菜单 | SQL 文件存在不等于数据所有权和启动结果正确 | Flyway 只保留 DDL；必需资源与 Demo 资源分开；资源由所属模块登记，Demo 默认关闭 |
| 数据结构与实体漂移 | 实体继承的审计字段未出现在 SQL，运行时插入/更新才报缺列 | Mock Mapper、简化 H2 schema 和只读接口绕开真实写入 | migration、Entity、Mapper、真实写入和更新必须一起验证 |
| API 与 Controller 校验冲突 | API 参数已有 Bean Validation，Controller 覆盖方法再次声明 `@Valid`，Spring 启动或调用时报继承约束异常 | 只调用 Service 或直接 new Controller，没有真实 Spring 方法校验代理 | API 声明约束；Controller 只继承；真实 Spring 上下文和非法请求接口测试 |
| 方法校验异常误报系统错误 | 独立 Controller 的 `@NotBlank` 已生效，但 `ConstraintViolationException` 未被统一异常处理识别，非法请求返回 HTTP 500 | 单元测试只断言注解存在或 Service 拒绝，没有从 HTTP 入口断言状态与消息 | 在统一异常处理映射为 HTTP 400；同时用 infra-web 集成测试和业务 Flow 空值请求回归 |
| 部分 Reactor 丢失模块路径事实 | `ResourceTargetController` 已使用正确 `/resource/targets`，但定向架构命令只选 sync-starter，未选同域本地 starter，导致 `MANGO-ARCH-CTRL-008` 无法取得 `/resource` | 为让局部扫描变绿，错误地给 sync-starter 重复增加 `module.properties`，又违反“只有本地 starter 声明模块信息” | 保留 Controller 显式根路径及接口测试；定向 Reactor 同时纳入同域本地 starter，让架构门禁读取唯一合法元数据；禁止在 sync/support/remote 重复声明 |
| 进程内共享常量误放 API | 内部调用验签后的 request attribute key 放在 `mango-infra-web-api` 普通实现类 | 把“多个模块使用”误等同于 HTTP API 契约 | 无 HTTP 语义、无数据库的纯 JVM 共享类型放 support；消费者声明直接依赖并编译回归 |
| 分层边界失效 | Controller 转换 Entity、一个 Controller 实现多个 API、Controller 未实现 API；Service 使用 `Impl`、直接继承 MyBatis `ServiceImpl` | 测试只验证返回值，未验证结构和真实适配链 | API、Controller、Service、Mapper、Entity、Feign 按根因整体迁移，不保留第二套实现 |
| Mock 证明范围被夸大 | Mock Mapper/数据库的测试被用于证明 SQL、字段、事务或资源落库正确 | 测试数量和覆盖率替代了测试目标审计 | Mock 只隔离外部协作者；数据库、Mapper、权限、资源同步和事务使用真实集成物料 |
| 部分绿灯冒充模块绿灯 | 聚合 POM `mvn test` 0.7 秒成功但实际执行 0 条测试；只跑 core 后宣称整个模块通过 | 没核对 Reactor 模块、Surefire 报告和测试数量 | 显式选择实际子模块；记录每个模块的 tests/failures/errors/skipped；零测试不得作为基线 |
| 源码与仓库依赖版本错位 | Authorization 使用了当前 `mango-resource-api` 新方法，但只选择 Authorization Reactor 时解析到 Nexus 旧 SNAPSHOT，编译报 `@Override` 错误 | 测试命令没有纳入发生契约变化的直接源码依赖 | 基线命令显式纳入必要的直接契约模块；同时保留独立消费者编译证据 |
| 自动配置条件不完整 | resource-sync 测试因 classpath 存在 Gateway 类就装配 Bean，但缺少 `RouteDefinitionLocator`，Spring 上下文失败 | 只测配置类存在或单类逻辑，未启动组合 classpath | 自动配置测试覆盖依赖存在、依赖缺失、Bean 存在和 Bean 缺失组合 |
| 兼容壳形成第二套协议 | Org API 同时保留旧 Entity、旧 Command 与新 VO/Command，Home、Notice 等内部消费者继续依赖旧协议，表面兼容实际扩大了分层债务 | 只检查 Org 自身编译，没有枚举直接消费者，也没有区分 Java 协议兼容和 HTTP 行为兼容 | 删除内部兼容壳前先搜索全部直接消费者；统一迁移到 VO、Command 和 Gateway；分别冻结 Java/HTTP 可观察契约 |
| 适配器失败语义丢失 | Gateway 把空远程结果包装为非空失败对象后，下游原有 `result == null` 回退分支失效，最终错误信息可能为空 | 测试只覆盖成功数据或只断言失败，没有断言失败消息和空响应边界 | 在适配器统一提供带 fallback 的失败消息；测试空响应、远程失败和业务失败三类结果 |
| 正确依赖注入被静态工具误报 | Spring 构造器注入被 SpotBugs `EI_EXPOSE_REP2` 误判，改成 `ObjectProvider` 后又触发 Controller 必须直接依赖 Service 接口的架构红线 | 质量工具不了解 Spring Bean 生命周期，单看告警数量无法判断真实缺陷 | 保留 `@RequiredArgsConstructor` 和 `private final I*Service`；优先用包级构造器限制非 Spring 调用面，并移除不必要的可变工具依赖；禁止用抑制注解、改成非 final 字段或包装 Service 来逃避检查 |
| Changed-only 路径口径不一致 | Git 变更路径带仓库根 `mango/`，Maven issue 路径从 `mango-platform/` 开始，报告把真实改动标成 `inChangedFiles=false` | 只看门禁绿灯，没有抽查报告里的 changed file 映射和 issue identity | 抽查 `inChangedFiles`、模块归属和路径归一化；在检查器修复前，用目标模块完整静态报告与改动文件交叉核验 |
| 定向检查被实现缺陷扩大或崩溃 | 模块级 `rule=all` 意外扫描仓库其它数字版本并触发 `NumberFormatException`，既慢又不能形成可靠结论 | 把命令名称里的“模块级”当成真实扫描边界，没有核对 report scope | 质量、架构和消费者编译分开执行；核对报告 scope；检查器异常不得当作业务失败或绿灯，需保留可复现证据 |
| 前端清单、锁文件和已发布版本漂移 | Org E2E 启动时源码要求 `@mango/admin-shell@1.0.42`，锁文件仍记录 `1.0.41`，内网仓库也只有 `1.0.41` | 后端测试不消费前端 workspace，旧机器已有 `node_modules` 时还会掩盖锁漂移 | E2E 前从干净依赖状态检查 manifest、lock 和 registry；主仓源码联调可显式链接同版本 workspace 包，发布验收仍必须等待锁文件与仓库版本一致 |
| 流程范围错位 | 发布物修复被扩大成无关代码 PR，或为解决仓库物问题修改源码 | 没先区分源码缺陷、构建缺陷、发布物缺陷和流程缺陷 | 先确定缺陷层级，只修改承担根因的层；一个模块一个任务分支和 PR |
| 资源 ID 只做模块内检查 | System 默认租户资源 ID 与 Identity 默认用户资源 ID 重复，单模块测试通过但组装应用启动冲突 | 每个模块只检查自己的声明文件，没有按最终 classpath 汇总 | 契约测试扫描组装仓库全部 `META-INF/mango/resources` 与 `demo`，资源 ID 必须全局唯一 |
| 资源租户字段混用编码与主键 | System 资源写入 `tenant_id=default`，运行时租户拦截器按主键 `1` 查询，数据存在但接口查不到 | Fixture 使用字符串租户且没有通过真实登录上下文读取 | `tenant_id` 使用运行时租户主键；租户编码只用于登录选择和业务展示；新库后按真实 token 查询 |
| 启动缓存早于资源同步 | 国际化 Service 在资源同步前缓存空列表，资源落库后五分钟内公开接口仍返回空 | 集成测试直接先写数据再调用 Service，没有覆盖空库启动顺序 | 空缓存不得长期命中，或资源同步后显式失效；空库启动后立即调用公开接口 |
| 跨资源类型初始化时序缺失 | 菜单套餐和管理员角色都成功落库，但租户最后创建，菜单补绑两次都找不到租户，所有管理接口 403 | 分别测试角色、菜单和租户 handler，没有验证同一批资源拓扑 | 使用 `dependsOnResourceTypes()` 固化租户→角色→菜单顺序，并验证最终 `role_menu` 数量和越权 403 |
| 内部调用未满足公开 API 校验 | 新建机构调用角色查询时漏传 `realm`、`actorType`，真实 Spring 方法校验抛 `ConstraintViolationException` | Mock API 接受不完整 Query，或直接调用 Service 绕过 Controller 代理 | 内部 Adapter 调用同样必须构造完整 Query/Command；测试记录实际参数并通过真实机构创建接口 |
| E2E 定位器语义不唯一 | 字典“值”字段用 `getByLabel('值')`，同时命中文本框和数字增减按钮，业务接口尚未执行即失败 | 页面结构变化后 label 模糊匹配范围扩大 | 优先按 role + accessible name 定位到具体控件；区分测试基础设施失败和业务失败后再处理 |
| 本地能力前置条件与业务缺陷混淆 | 单体启动因未安装 LibreOffice、未配置加密密钥失败，与 System 逻辑无关 | 启动命令没有显式登记宿主能力要求 | 验收环境明确关闭未纳入范围的可选预览能力，并提供仅用于测试的合法密钥；证据中记录，不改业务逻辑绕过 |
| 增量静态报告复用旧字节码 | 源码和 class 已修复，根目录聚合报告仍显示旧构造器和旧行号 | Maven 增量执行跳过了聚合报告重建，只看退出码误把旧 JSON 当新结果 | 核对报告时间、class 签名和模块原生报告；目标模块 clean 后重新生成，不用旧聚合报告证明新提交 |
| 开发启动安装阶段扩大为全仓门禁 | `mango dev start` 的安装阶段在目标模块均已构建后，被全仓存量架构债务阻断 | 启动、构件安装和正式质量门禁共用一个全 Reactor 生命周期，范围边界没有分离 | 质量结论使用已审计的目标 Reactor；启动失败先核对失败模块和阶段，不把其它模块存量债务冒充当前模块失败，也不重复跑无关全仓检查 |
| CLI 就绪早于资源派生关系完成 | 端口已响应时 demo 租户已出现，但行政区划、国际化和角色菜单仍在继续同步 | 就绪探测只证明 HTTP 进程可响应，不能证明 Resource Registry 完成 | 新库验收等待健康检查 UP 和预期资源计数/派生关系稳定，再运行 API/E2E；不能在首个 HTTP 响应后立即取数下结论 |
| HTTP API 与本地动态 SPI 混放 | Resource API 同时暴露 HTTP Command/VO 和 Provider、Handler、Builder、可变声明模型，任何本地扩展都被迫依赖公开协议层 | 只看“其它模块能 import”，没有区分跨进程稳定契约和进程内协作接口 | API 只放稳定 HTTP 契约；动态 Provider/Handler/Dispatcher 与声明构造能力放 support；删除旧协议并一次迁移全部直接消费者 |
| 为追求分层新增空壳 target 模块 | `target-core/target-starter` 的 Java 包命中 `.gitignore` 的 `target/`，最终 Git 只有 POM 和失效自动配置；发布物增加但没有能力 | 只看工作区和 Maven 成功，没有核对 Git 跟踪文件、JAR 与自动配置类 | 删除空模块；纯执行端口留在 support，目标 Controller 由 sync-starter 装配，remote 只保留客户端；用 `git ls-files`、JAR 清单和自动配置类加载测试验证 |
| 动态服务地址改写丢失上下文路径 | Feign 或 HTTP 客户端把服务名改成实例地址后，只保留 host/port，真实接口 404 | 单元测试只使用根路径 URL，未覆盖服务 base path | 地址改写只替换 authority，保留 path/query；测试服务名、显式 host:port 和带 base path 三种输入 |
| 内部 Header 被安全链直接信任 | 外部请求伪造内部调用 Header 即可绕过认证，或 HMAC 已验证但安全链看不到结果 | 验签 Filter 与 SecurityFilterChain 使用不同信任语义 | 验签成功后写入服务端 request attribute，安全链只信任该属性；原始 Header 必须有“不放行”回归测试 |
| 跨服务资源乱序启动导致声明永久缺失 | Authorization 先于 System 启动，父资源不存在；一次同步失败后应用退出或不再上报 | 把启动同步当一次性动作，没有建模跨服务最终一致性 | 远程失败/未完成时重试，完整成功后停止；真实乱序启动并核对最终 registry 与父子关系 |
| 分布式锁竞争被包装成同步成功 | 注册中心未取得锁而跳过批次，却向来源服务返回成功，来源服务停止重试 | 锁只被当作服务端实现细节，没有进入完成语义 | 未取得锁返回 `data=false`，来源继续重试；真实多节点断言 registry 数量、重复数和 SKIP 日志 |
| 后端能力被虚构为有产品页面 | Resource 没有独立菜单/页面，证据却写“Resource 页面 E2E 通过” | 把通用浏览器 shell/API 用例扩大解释 | 明确记录 UI 不适用；以真实 API、数据库和单/多节点拓扑 E2E 作为主证据 |
| 删除 migration 后旧构件仍残留 | 源码已删除 V2，普通 `mvn test` 后 `target/classes` 仍保留旧 V2，后续打包可能继续携带 | 只检查 `src/main/resources`，没有 clean 或检查最终构件清单 | migration 契约测试同时检查源码边界；最终验证必须 clean；检查 `target/classes` 和 JAR 中 migration 清单 |
| 消费者测试 schema 跟不上公共实体契约 | Resource 实体改为 `TenantEntity` 后，Authorization 集成 fixture 仍缺租户/审计列，消费者测试编译或运行失败 | 只跑生产者测试，消费者使用自建简化 H2 表 | 公共持久化契约变化后枚举直接消费者；同步更新消费者自有 fixture，并保留至少一个真实公共 Service 入口集成测试 |
| 资源 Handler 在调用者租户下处理声明租户 | 强制同步 Notice 的 `default` 渠道时，租户拦截器又附加当前租户 `1`，查询不到既有行后重复插入固定主键 | Handler H2 测试关闭租户插件，且只执行一轮同步 | 测试启用真实租户插件并从不同调用者租户连续重放；Handler 在声明租户上下文执行并 finally 恢复，禁止使用忽略租户检查注解 |
| 只验证启动同步、不验证管理操作 | 初始同步成功，但 `/resource/sync/force` 重放全部 AUTO 资源时才触发跨租户主键冲突 | E2E 只打开列表页，没有执行写操作和失败路径 | Resource 验收同时调用缺参接口和强制同步；断言 400 业务错误、200 成功以及目标表数量/租户数据不变 |
| 上游 vendored 代码被当作 Mango 业务代码 | File Preview 内置 `cn.keking` 上游引擎触发 211 个 Mango Controller/Service 约定问题 | 架构门禁没有建模供应商源码所有权，对所有 classpath 包名套同一规则 | 仅对已审计的精确供应商 namespace 划定所有权边界；`io.mango.*` 仍全部受控；用反例测试证明本地 Controller 无法借此逃逸 |
| 本地页面/流传输适配器被强迫返回 JSON | `ModelAndView` 和 `ResponseEntity<InputStreamResource>` 被要求实现 `XxxApi` 并返回 `R<T>` | 规则只建模 JSON API，没有区分原生 HTTP 页面/二进制响应 | 建立严格原生适配器白名单，仅允许 `ModelAndView` 和 `ResponseEntity<Resource>`；参数绑定、OpenAPI、Service 依赖和安全规则继续生效；`ResponseEntity<String>` 反例必须失败 |
| Controller 有权限注解但正式资源未声明 | File Preview 上传正常，但新库管理员调用预览链接返回 403 | 单元/Mock Flow 关闭授权，老库可能已有手工权限绑定 | 每个 `@ApiAccess(PERMISSION)` 都必须在所属模块正式菜单/Api Resource 中存在；新库以真实角色调用成功和无权限拒绝双向证明 |
| 源码新但本地 Maven 运行物仍旧 | 工作区已修复 Feign base path 和删除 Security Customizer，双进程却仍 404 并输出 22 条旧警告 | 普通增量 `install` 复用了旧 classes/JAR，只看源码无法确认实际 classpath | 删除类、migration 或资源后必须 `clean install/package`；对比 target 与 `~/.m2` SHA-256、`jar tf`、`javap`/class 清单；重启真实消费者 |
| 单进程 Mock 链路代替微服务验收 | File Preview 旧“E2E” Mock `FileApi` 和内容 Provider，无法发现 Feign 服务名改写丢失 `/file/files` | 测试运行了 HTTP，但核心跨进程边界被替身掉 | 正确命名为 Flow 测试；最终微服务 E2E 分别启动产生者/消费者，通过服务发现真实上传、元数据、二进制流和页面正文闭环验证 |
| 任意 JSON 为逃避强转使用裸 Map | Template 渲染变量直接在多层传递 `Map<String, Object>` 并依赖 unchecked cast | 只验证简单标量 JSON，没有冻结嵌套对象的线格式与类型边界 | API 用具名 JSON 值对象封装并保持 wire object 不变；兼容测试覆盖标量、嵌套对象、数组和反序列化回放 |
| 远程 `R<T>` 泄漏进 Core | Template Core 为校验业务域直接依赖远程响应包装，进程内业务层被 HTTP 成功/失败语义污染 | 单体装配时远程实现与本地实现位于同一进程，分层问题不影响功能测试 | Core 只依赖本域 Provider/值对象；starter 适配 Feign 并统一解包 `R<T>`、空响应和失败消息 |
| Demo 未显式开启导致菜单验收假失败 | Fresh DB DDL 和正式资源均成功，但演示管理员角色为零，页面菜单为空 | 把“新库启动成功”误当成“演示验收数据已加载” | 分别验证 demo 关闭的生产初始化和 demo 开启的验收初始化；启动命令显式设置正确资源注册前缀并核对角色/菜单计数 |
| 登录 E2E 在响应结束后才等待响应 | 自动填充/失焦已触发前置请求，测试随后 `waitForResponse` 永久错过事件 | 用网络等待代替页面稳定状态，且没有围绕真正登录 POST 建立等待 | 前置状态用可见内容判断；在点击登录前注册对登录 POST 的等待，并断言 HTTP 与业务响应双成功 |
| 直连能力服务混用浏览器租户头和内部上下文头 | 绕过网关直调 Template 服务只传 `X-Tenant-Id`，下游 Domain 收不到租户上下文，租户 SQL 被拒绝 | 单体共享线程上下文，无法暴露跨 JVM 传播协议差异 | 微服务 E2E 经网关时验证网关转换；确需直连时按内部协议传 `X-Mango-Tenant-Id`，并由 Feign 继续传播，禁止配置默认租户绕过 |
| Changed-only 绿灯掩盖模块存量质量债务 | Template 的 no-new-violations 报告最初把历史问题放入 baseline，PR 表面可只要求“没有新增” | 只看门禁退出码，没有审计 `issues/newIssues/baselineIssues` 和目标模块总量 | 历史债务任务不能停在 no-new；对目标模块逐项消除并确认四类报告列表都为空，最终静态问题为 0 |
| DTO/值对象暴露可变内部状态 | Command、VO、record 直接保存或返回 List、Map、JSON wrapper、`byte[]`，调用者可在校验后篡改对象 | 测试只比较初始值，没有覆盖构造后和 getter 后的外部修改 | 构造/Setter 输入和 Getter/accessor 输出都做防御复制；集合使用不可变副本，数组逐次复制；补嵌套 JSON 与二进制回归 |
| 能力应用误连默认 H2 | 单体使用 MySQL 环境别名，但独立 capability app 未读取同一别名，双 JVM启动时落到 H2 并拒绝 MySQL DDL | 只验证单体，未核对每个独立进程实际 JDBC URL | 微服务 E2E 为每个 JVM 显式绑定 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`，启动后核对连接库和 Flyway history |
| Feign 直连配置未命中实际客户端 | 只配置服务名 URL 后，按 `contextId` 创建的 Feign 客户端仍尝试负载均衡，或把带协议 URL 填进仅接受服务名的入口 | 单体本地 Provider 不经过 Feign，单进程测试无法暴露配置键差异 | 无注册中心的双 JVM测试同时设置服务名 `host:port` 与实际 `contextId` 的绝对 URL；保留真实 base path、租户头和 HTTP 断言 |

## 3. 改前与改后不变性的证明方式

治理不以“代码看起来等价”为结论，而以同一组可观察结果对比：

1. 冻结公开 Java/HTTP 契约、权限码、错误码、状态、租户边界和关键数据库副作用。
2. 在生产代码未修改时运行有效单元、真实数据库集成和 API 测试，登记精确用例数及失败事实。
3. 使用全新数据库真实启动服务，验证 Flyway、资源初始化、健康检查和关键读写。
4. 从浏览器用户入口执行 UI/E2E，验证菜单、页面、操作结果、无权限、租户隔离、console 和 network。
5. 修改后在另一套全新数据库执行完全相同的命令、账号角色和业务断言。
6. 检查最终 JAR，而不只检查 `src/main/resources`；确认 migration 清单、DDL/DML 边界和资源清单正确。
7. 单元、集成/API、UI/E2E 任一层失败时，模块保持未验收状态，不提交“部分完成”PR。
8. 质量门禁绿灯后抽查报告的 changed-file 映射、扫描范围和工具失败；工具自身误报或崩溃不能替代代码结论。
9. 对直接消费者执行编译和行为测试，特别核对失败消息、空响应、权限、租户和数据清理，不能只证明生产者模块自身通过。
10. 对资源驱动模块检查最终组装顺序、全局资源 ID、实际租户主键和派生关系表；“每张目标表都有数据”不能证明初始化完成。
11. 对启动期缓存执行“先读空、后同步、再读”回归，防止空结果在资源落库后继续存活。
12. E2E 失败先判断请求是否真正发出；定位器、Web Server、浏览器环境失败不能冒充业务回归失败，修复后必须重跑原业务断言。
13. 静态门禁同时核对报告生成时间、当前 class 和模块原生结果，避免把增量构建遗留的旧报告当成当前提交证据。
14. 启动验收必须等待健康检查和 Resource Registry 派生关系全部稳定；端口可访问、租户表已有数据都不等于初始化完成。
15. 公共能力先区分跨进程 HTTP 契约与进程内动态 SPI；二者不能因为“都被跨模块使用”而放入同一个 API 包。
16. 删除或合并 migration 后必须 clean 并检查最终构件清单，普通增量测试不能证明发布物不再携带旧资源。
17. 资源 Handler 的租户测试必须启用真实拦截器、使用不同调用者租户并至少重复执行两轮；上下文切换必须可恢复，不能靠忽略检查注解通过。
18. Resource E2E 不能只读列表，至少覆盖一个非法请求和一次真实强制同步，并对目标表数量、租户边界和运行时错误做断言。
19. 新增或迁移模块时核对 `git ls-files`、clean JAR 和自动配置导入类，防止 `.gitignore` 把合法包名吞掉。
20. 分布式同步必须区分 HTTP 成功与业务批次完成；锁竞争或依赖未就绪时来源节点继续重试，直到完整成功。
21. 内部调用只信任服务端验签产生的属性，不能把客户端可伪造 Header 直接作为放行依据。
22. 多节点 E2E 至少断言服务发现健康实例、registry 稳定数量、两类重复数、CREATE/SKIP 日志和一个节点失效后的继续同步。
23. 没有独立产品页面的后端能力明确标记 UI/E2E 不适用，不得把通用 shell/API 测试描述成该模块页面验收。
24. 对内置上游源码按精确 namespace 建模代码所有权，不通过模块排除或抑制注解让 Mango 自有代码逃逸。
25. 原生 HTTP 页面/流适配器只能返回受限原生类型；JSON `ResponseEntity` 仍必须遵守 `XxxApi + R<T>` 契约，且 PMD 与 ArchUnit 需有对称反例。
26. 权限注解与正式资源声明是一个契约；必须在 demo 关闭的新库检查资源落库，再用真实角色执行业务请求。
27. 涉及删除或资源清单变化的运行验收，必须在 `clean install` 后核对 target、本地 Maven 仓库与运行 classpath，否则旧 JAR 会让源码分析结论失效。
28. 微服务验收要使用真实的两个 JVM 和服务路由，并比对最终正文/数据副作用；单进程 Mock HTTP 测试只能标记为 Flow。
29. Bean Validation 验收不能停在注解或异常类型；必须从真实 HTTP 入口断言非法参数返回 400 和稳定消息，避免 `ConstraintViolationException` 落入系统异常 500。
30. 一个业务域只能由本地 starter 提供唯一 module metadata；sync/support/remote 不得重复声明。定向架构 Reactor 若检查同域适配器，必须同时纳入本地 starter，不能用新增元数据修补扫描范围缺失。
31. 跨模块共享不自动等于 API；request attribute key 等纯 JVM 契约应放无数据库、无 HTTP 的 support，并通过真实消费者编译证明迁移完整。
32. 任意 JSON 输入应使用具名值对象固定 Java 边界，同时用序列化兼容测试证明外部仍是普通 JSON object，不能以裸 Map 和 unchecked cast 换取表面灵活。
33. Core 不处理远程 `R<T>`；Feign 响应解包、空响应和失败消息归 starter/adapter，Core 只消费本域 Provider。
34. Fresh DB 需分两套验收：demo 关闭证明生产初始化纯净，demo 开启证明演示角色、菜单和页面可用；两者不能互相替代。
35. 跨 JVM 直连测试必须使用 Mango 内部上下文头或真实网关，不能设置默认租户、关闭租户拦截器来伪造通过。
36. E2E 网络等待必须在触发动作前注册；对已由自动填充触发的前置请求应断言最终页面状态，避免响应竞态。
37. 历史债务任务即使使用 changed-only/no-new 门禁，也必须审计目标模块总问题数；baseline 不是验收通过，目标范围内的 `issues/newIssues/baselineIssues/toolFailures` 应全部清空。
38. API/领域对象持有 List、Map、JSON wrapper 或数组时，输入端和输出端都要防御复制；只复制一侧仍会暴露可变状态，`byte[]` 尤其不能直接返回。
39. 静态工具误报 Spring 构造器注入时，禁止加抑制注解或破坏 `private final I*Service` 规范；应缩小构造器可见性、移除不必要的可变依赖，并用 Spring 装配测试证明行为。
40. 独立能力应用 E2E 必须逐进程确认真实数据源；不能假设单体使用的环境别名会被所有 capability app 读取。
41. 无注册中心的 Feign 直连必须按实际客户端 `contextId` 配置 URL，并保留服务 base path 与内部租户传播；单体本地 Provider 通过不能替代这项验证。

## 4. 后续模块处理节奏

每次只处理一个模块：从最新 `main` 创建专用工作区，完成改前基线、根因治理、改后同套验证、真实新库启动和浏览器 E2E；验收通过后提交一个 PR，合并并清理工作区，再选择下一个模块。优先级综合权限/资金/流程等失败后果、下游依赖数量、空库启动风险和正式债务规模决定，不以修改文件数决定。
