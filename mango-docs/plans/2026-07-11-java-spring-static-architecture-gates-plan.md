# Java/Spring 静态架构门禁实施计划

## 1. 交付目标

在不引入 SonarQube Server、数据库、Docker 或后台服务的前提下，用 Maven Enforcer 3.6.3、ArchUnit 1.4.2、PMD 7.26.0 替换 Java/Spring 七类正则硬门禁，并由可重复的构建、正反例、真实仓和性能数据证明可用。

## 2. 实施顺序

1. 建立统一规则编号、违规模型和 JSON 报告模型。
2. 用 MavenProject/Dependency 模型实现模块依赖规则，提供 Enforcer Rule 入口。
3. 用 ArchUnit 导入 Reactor 已编译目录一次，实现类型放置、注解和跨层依赖规则。
4. 用 PMD 7 Java AST 实现 Controller、Service、Mapper 的源码语义规则；解析或类型分析异常 fail-closed。
5. 增加聚合 Maven goal，并绑定验证生命周期；普通路径和 `.mango/worktrees` 使用相同相对输入。
6. 冻结 TC-ARCH-001 至 TC-ARCH-014，断言真实规则编号和构建退出码。
7. 新旧引擎只在迁移测试中对照；新引擎达标后从 CI、`mango:check all` 和不可豁免规则中删除七类旧正则硬判断。历史显式命令仅保留兼容诊断，并明确输出非权威警告。
8. 对 Mango 全 Reactor 和只读 `baohan-system` 验证，执行至少五轮性能基准。

## 3. 兼容与迁移边界

- Mango 根聚合 POM 是 Java 21，发布用 `mango-parent` 是 Java 17。规则 JAR 保持 Java 17 字节码兼容，PMD 7 按被检项目的实际语言版本解析；双基线本身另行治理。
- P3C 2.1.1 基于旧 PMD API，先保留为非阻断通用检查；七类架构红线只认 PMD 7 结果。
- 自托管 Maven 插件必须证明干净本地仓库下的 Reactor 可构建性；若插件依赖形成 bootstrap 环，必须通过 Reactor 模块排序或专用聚合验证模块消除，禁止要求人工预装后才假装 `mvn verify` 可用。
- 不自动冻结当前 698 条候选问题；只有 precision 达到 98% 且合法样例误报为零的规则才能硬阻断。

## 4. 验证命令与证据

交付证据至少包括：

- 规则模块单元测试和真实 Maven fixture 集成测试报告。
- `mvn verify` 在普通路径及包含 `.mango/worktrees` 路径下的退出码、规则编号和差异结果。
- Mango 全量报告、抽样复核表和工具失败统计。
- `baohan-system` 扫描前后 HEAD、tracked/untracked 快照一致性证明。
- 同一 JDK、同一机器、预热后连续五轮的 changed-only 与全量耗时，报告中位数和最大值。

## 5. 切换条件

只有设计文档第 6 节全部阈值达标，才能切断旧正则的交付门禁权威并给出“可全量推广”结论。达标前不发布、不 push、不创建 PR、不合并到 `main`。
