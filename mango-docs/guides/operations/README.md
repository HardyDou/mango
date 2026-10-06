# 运维、升级与交付

本章面向需要构建、发布、升级或治理业务项目的工程人员。它描述当前可执行的操作路径，不替代 `mango-pmo` 中的流程规则、审批和质量门禁。

## 按任务选择

### 构建与发布

| 任务 | 入口 | 输出 |
|---|---|---|
| 规划业务项目的镜像、测试、生产发布和回滚 | [CI/CD 发布实践](./ci-cd-release-practices.md) | Pipeline 阶段、制品、凭据、健康检查和回滚证据 |
| 为 API 制品生成 cold baseline | [构建期 cold baseline](./build-time-cold-baseline.md) | 每模块 B、Resource manifest、文件 manifest 和可复现 JAR |
| 区分 Flyway、Resource、Demo 和运行期数据 | [Resource 重置与增量发布](./resource-reset-incremental-release.md) | reset/incremental 发布选择和验收矩阵 |

### 升级与兼容

| 任务 | 入口 | 适用对象 |
|---|---|---|
| 从 Maven 1.0.21 升到 1.0.22 | [Java API 升级](./maven-1.0.21-to-1.0.22-java-api-upgrade.md) | 需要处理 Java 类型或公开 API 迁移的项目 |
| 从 1.0.30/1.0.3x 升到 1.0.31 | [Mango 1.0.31 升级](./mango-1.0.30-to-1.0.31-upgrade.md) | 需要整体对齐 Maven、npm、CLI、PMO 和 Bootstrap 的项目 |
| 升级 Workflow 办理人身份契约 | [办理人身份升级](./workflow-assignee-identity-upgrade.md) | 使用 `assigneeDisplayName`、参与关系或历史身份数据的项目 |

### 治理与修复

- [业务模块历史债务修复](./history-debt-remediation.md)：在不改变外部契约的前提下处理模块债务。

## 标准阅读顺序

1. 先确认目标版本、部署拓扑和数据分类。
2. 再阅读对应模块 README 和发布/升级指南。
3. 执行 PMO preflight、交付契约和验证门禁。
4. 最后保存 release manifest、Bootstrap receipt、测试结果和回滚方案。

## 关联入口

- [业务接入场景](../business-integration/README.md)
- [常见问题与排障](../faq/README.md)
- [业务项目开发指南](../../designs/business-project-development-guide.md)
- [交付与发布规则](../../../mango-pmo/rules/10-release-artifacts.md)
- [PMO 总流程](../../../mango-pmo/rules/00-dev-flow.md)
