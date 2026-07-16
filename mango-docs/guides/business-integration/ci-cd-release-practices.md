# Mango 业务项目 CI/CD 发布实践

## 1. 定位

本文面向基于 Mango 开发的业务项目，提供一套可以直接采用的 CI/CD 推荐实践和新项目默认基线。它不是 PMO 强制规则，也不要求所有项目使用相同工具；项目可以根据网络、合规、发布频率和团队职责调整，并建议在项目部署说明中记录偏离项。

本文使用以下通用占位符：

| 占位符 | 含义 |
|--------|------|
| `<project>` | 业务项目标识 |
| `<app>` | 一个可独立构建和部署的应用 |
| `<registry>` | OCI 镜像仓库地址 |
| `<git-repository>` | Git 仓库地址 |
| `<target>` | 测试或生产部署目标 |

示例不对应任何真实业务、环境或凭据。

## 2. 适用范围

这套默认模型适合：

- 源码保存在 GitHub、Gitea、GitLab 或其它 Git 服务。
- 一个仓库包含一个后端和一个或多个前端，或者包含多个可独立部署的应用。
- 应用最终以 OCI/Docker 镜像发布。
- 测试和生产环境可以从镜像仓库拉取镜像。
- 单机或少量服务器使用 Docker Compose 部署。

项目使用 Kubernetes、云原生发布控制器或其它 CI 平台时，仍可以复用版本、digest、制品晋级和证据模型，只替换执行工具。

## 3. 默认工具组合

| 职责 | 默认选择 | 可替换项 |
|------|----------|----------|
| 源码与代码 Tag | Git + Gitea/GitHub | GitLab、其它 Git 服务 |
| Pipeline 编排 | Jenkins Pipeline | GitHub Actions、GitLab CI、Tekton 等 |
| 依赖仓库 | Nexus 或等价 Maven/npm 仓库 | 企业制品服务 |
| 镜像仓库 | Harbor | 其它 OCI Registry |
| 镜像构建 | Docker BuildKit / Buildx | Kaniko、Buildah 等 |
| 目标环境部署 | Docker Compose | Kubernetes、Nomad 等 |
| 凭据 | Jenkins Credentials + 目标机运行时环境文件 | Vault、云密钥服务 |

默认工具只是接入参考。真正需要长期保持一致的是：来源提交可追溯、镜像不可变、环境按同一 digest 晋级、发布失败可恢复。

## 4. 三段式发布模型

推荐把发布链路拆成三个独立 Pipeline：

```text
Git 分支与提交
      │
      ▼
构建候选镜像 ──► OCI Registry
                      │
                      ▼
                发布测试环境
                      │
                健康检查通过
                      │
                      ▼
                发布生产环境
                      │
          正式镜像 Tag + Git Tag + Release
```

三个任务默认不自动串行触发：

1. **构建**只生成候选镜像和发布清单，不部署环境。
2. **发布测试**只选择已经存在的候选镜像，测试通过后追加测试晋级 Tag。
3. **发布生产**只选择已经通过测试的同一 digest，部署成功后追加正式 Tag 并记录代码 Release。

这种模型的核心是“一次构建，多环境晋级”。测试和生产不分别重新编译，从而可以证明生产运行的二进制就是测试通过的二进制。

## 5. 版本、Tag 与 digest

### 5.1 应用版本

项目根目录建议保存一个统一部署版本：

```text
VERSION
```

内容采用语义版本：

```text
1.2.0
```

它表示整套可运行应用的发布线，不等同于 Maven JAR 或 npm package 的版本。依赖包可以保持自己的版本节奏，部署版本只描述本次应用镜像集合。

版本提升建议通过代码变更进入评审，不额外提供可以绕过仓库记录的手工版本参数。

### 5.2 候选构建号

每次构建建议生成北京时间短构建号：

```text
yyMMddHHmm
```

完整候选版本为：

```text
<VERSION>.<yyMMddHHmm>
```

例如：

```text
1.2.0.2607162315
```

同一分钟存在并发构建时，可以追加 Jenkins Build Number 或两位序号，避免候选版本冲突。

### 5.3 镜像命名

业务镜像默认按以下形式组织：

```text
<registry>/biz-<project>/<app>:<tag>
```

推荐的 Tag 及其语义：

| Tag | 创建阶段 | 默认语义 | 是否移动 |
|-----|----------|----------|----------|
| `<VERSION>.<build-number>` | 构建 | 候选构建 | 不移动 |
| `<full-git-sha>` | 构建 | 精确源码提交 | 不移动 |
| `test-<VERSION>.<build-number>` | 测试通过 | 已通过测试晋级 | 不移动 |
| `prod-<VERSION>` | 生产成功 | 正式生产版本 | 不移动 |
| `<VERSION>` | 生产成功 | 便于人工识别的正式版本 | 不移动 |

发布和回滚建议使用完整 digest：

```text
<registry>/biz-<project>/<app>@sha256:<digest>
```

Tag 用于查找和表达阶段，digest 用于证明运行的是哪一份不可变内容。

### 5.4 OCI 元数据

镜像建议写入以下 OCI Label：

```text
org.opencontainers.image.version
org.opencontainers.image.revision
org.opencontainers.image.created
org.opencontainers.image.source
```

这些元数据只包含版本和来源，不放密码、Token、内部密钥或运行时配置。

## 6. 构建 Pipeline

### 6.1 默认参数

| 参数 | 类型 | 用途 |
|------|------|------|
| `RELEASE_BRANCH` | Git 分支参数 | 选择需要构建的分支 |
| `APP_<NAME>` | 布尔参数 | 选择一个或多个应用 |

应用参数默认全选，并在 Pipeline 开始阶段校验至少选择一个应用。

Jenkinsfile 建议从受信任的默认分支加载，实际源码再显式检出用户选择的业务分支。这样业务分支不能通过修改自身 Pipeline 绕过发布边界。

### 6.2 默认阶段

```text
代码检出
→ 版本与提交校验
→ 所选应用并行构建
→ 推送候选 Tag 和 Git SHA Tag
→ 生成发布清单
→ 输出构建结果
```

每个应用保留独立阶段和独立失败结果。资源允许时并行构建；资源不足时可以限制并发，但不改变每个应用独立可观察的结果。

### 6.3 构建清单

构建成功后建议生成机器可读清单，例如：

```dotenv
RELEASE_VERSION=1.2.0
BUILD_VERSION=1.2.0.2607162315
GIT_SHA=<full-git-sha>
APP_<NAME>_IMAGE=<registry>/biz-<project>/<app>
APP_<NAME>_DIGEST=sha256:<digest>
```

清单可以归档到 Jenkins，也可以保存为带校验和的短期制品。镜像仓库中的 digest 仍是最终制品事实。

### 6.4 构建性能默认值

推荐依次采用以下优化：

1. 使用 `.dockerignore` 排除 `.git`、本地依赖、构建目录、运行日志和无关文档。
2. 使用多阶段 Dockerfile，把依赖下载、应用编译和运行时镜像分开。
3. 使用 BuildKit cache mount 缓存 Maven、npm 或 pnpm 依赖。
4. 先复制依赖描述和锁文件，再复制业务源码，减少依赖层失效。
5. 内网环境通过 Harbor 代理基础镜像，通过 Nexus 代理 Maven/npm 依赖。
6. 多应用共享同一 Docker daemon 或远端缓存时，避免每个应用重复下载同一依赖。

并行数量以实际 CPU、内存和磁盘吞吐为准。单个 Jenkins 节点可以同时执行一个 Pipeline 内的并行 stage，不要求为每个应用单独部署 Agent；只有资源隔离或构建规模需要时再增加 Agent。

## 7. 测试发布 Pipeline

### 7.1 默认参数

| 参数 | 类型 | 用途 |
|------|------|------|
| `BUILD_VERSION` | 字符串 | 选择已经构建完成的候选版本 |
| `APP_<NAME>` | 布尔参数 | 选择本次需要更新的应用 |
| `DEPLOY_CONFIRMATION` | 字符串或布尔值 | 确认测试发布动作 |

### 7.2 默认阶段

```text
参数与候选制品校验
→ 读取候选镜像 digest
→ 合并当前环境发布清单
→ 目标机按 digest 拉取镜像
→ Docker Compose 更新所选服务
→ 健康检查
→ 追加 test-* Tag
→ 输出发布结果
```

选择性发布时，目标环境的当前清单与本次选择清单合并。未选择应用继续使用原 digest，避免空参数把运行中的镜像覆盖掉。

测试 Tag 在健康检查通过后创建。这样 `test-*` 表达“已经部署并通过约定检查”，而不仅是“曾经构建成功”。

## 8. 生产发布 Pipeline

### 8.1 默认安全门

生产发布建议至少检查：

- 运行人选择了一个或多个应用。
- 构建版本格式有效。
- 所选镜像存在对应的 `test-*` Tag。
- `test-*` 与候选版本仍指向同一 digest。
- 生产目标、SSH 凭据、部署目录和健康检查地址已经配置。
- 运行人填写明确的生产确认短语。
- 当前没有另一个生产发布任务执行。

### 8.2 默认阶段

```text
生产门禁
→ 测试晋级制品校验
→ 备份生产发布清单
→ 按同一 digest 更新所选服务
→ 健康检查
→ 追加正式镜像 Tag
→ 创建 annotated Git Tag
→ 创建代码 Release
→ 输出发布结果
```

生产部署不会重新构建镜像。正式镜像 Tag、代码 Tag 和 Release 都在生产健康检查成功后创建；部署失败时不留下“已经正式发布”的错误标记。

多应用选择性生产发布会形成混合版本环境，因此发布清单需要逐项记录每个应用的构建版本和 digest。项目希望整套应用原子升级时，可以把“全选”作为自身生产门禁。

## 9. Docker Compose 部署数据流

### 9.1 仓库内文件

项目可以维护以下文件：

```text
deploy/
├── docker-compose.test.yml
├── docker-compose.prod.yml
└── README.md
```

Compose 文件只引用镜像变量、端口、健康检查和卷，不保存真实密码。

### 9.2 目标机文件

目标机默认维护：

```text
/opt/<project>/
├── docker-compose.yml
├── runtime.env
├── images.env
└── releases/
```

| 文件 | 内容 | 是否进入代码仓库 |
|------|------|------------------|
| `docker-compose.yml` | 服务、网络、卷和健康检查 | 模板可以入库 |
| `runtime.env` | 数据库密码、JWT 密钥、对象存储密钥等 | 不入库 |
| `images.env` | 每个应用的镜像 digest、版本和 Git SHA | 目标机运行态文件 |
| `releases/` | 发布前清单备份和最小审计信息 | 目标机保留 |

镜像通过目标机执行 `docker compose pull` 从 Registry 拉取，不使用 `scp`、`cp` 或上传 JAR/前端目录代替镜像发布。Pipeline 如需同步 Compose 模板或发布清单，只同步小型文本配置并校验内容；应用二进制始终来自镜像仓库。

### 9.3 选择性更新

推荐流程：

1. 读取当前 `images.env`。
2. 用本次选择应用的 digest 覆盖对应字段。
3. 保留未选择应用原值。
4. 把旧清单复制到 `releases/<timestamp>/images.env`。
5. 执行 `docker compose pull <selected-services>`。
6. 执行 `docker compose up -d <selected-services>`。
7. 按应用执行健康检查。

## 10. 健康检查与回滚

健康检查建议同时覆盖：

- 容器处于运行或 healthy 状态。
- 后端健康端点返回预期状态。
- 每个前端入口返回成功状态并能加载静态资源。
- 反向代理到后端的关键路径可用。
- 目标容器实际运行 digest 与 `images.env` 一致。

发布失败时，默认回滚流程为：

```text
恢复发布前 images.env
→ 按旧 digest 拉取镜像
→ Docker Compose 恢复所选服务
→ 重新执行健康检查
→ 保留本次 Pipeline 失败结果
```

回滚成功表示环境已经恢复，不把原发布任务改写为成功。涉及数据库变更时，业务项目还需要单独设计向前兼容、迁移窗口和数据恢复方式；容器回滚本身不能撤销数据库写入。

## 11. Harbor 制品管理

### 11.1 Project 分类

推荐使用业务 Project 前缀：

```text
biz-<project>
```

以下类型建议单独使用 Project，不套用业务镜像保留周期：

- `library` 或企业基础镜像。
- Docker Hub、GHCR 等代理缓存。
- 数据库、中间件、监控和 CI 基础设施镜像。

### 11.2 默认保留周期

| 制品阶段 | Tag 示例 | 默认保留 |
|----------|----------|----------|
| 候选/开发 | `<VERSION>.<build-number>`、Git SHA | 最近 14 天 |
| 测试通过 | `test-<VERSION>.<build-number>` | 最近 30 天 |
| 正式生产 | `<VERSION>`、`prod-<VERSION>` | 永久 |

Harbor Retention 通常按 Artifact/digest 判断。多个 Tag 指向同一 digest 时，只要正式 Tag 的永久规则命中，该 Artifact 就会继续保留。正式启用清理前，建议先执行 Dry Run 并确认规则只覆盖 `biz-*` Project。

### 11.3 查找未晋级制品

没有 `test-*` 或 `prod-*` Tag 的候选 Artifact，可以视为尚未晋级。Harbor 清理规则可以按候选 Tag 模式和最近拉取/推送时间回收这些 Artifact，不需要根据 Jenkins 构建号反推。

## 12. 权限与凭据

推荐把权限拆成三类：

| 身份 | 源码 | Registry | 服务器 | Git Tag/Release |
|------|------|----------|--------|-----------------|
| 构建身份 | 只读 | 推送候选镜像 | 无 | 无 |
| 测试发布身份 | 只读或无 | 读取候选、追加测试 Tag | 仅测试环境 | 无 |
| 生产发布身份 | 只读 | 读取测试制品、追加正式 Tag | 仅生产环境 | 创建正式 Tag 和 Release |

凭据推荐保存在：

- Jenkins Credentials。
- 目标机权限受限的 `runtime.env`。
- Vault 或企业密钥管理系统。

Jenkins 参数、镜像 Label、发布清单、Git 仓库和控制台日志只记录非敏感的版本、SHA、digest、任务号和目标标识。

## 13. Jenkins 任务与构建记录

### 13.1 任务组织

每个业务项目推荐三个主任务：

```text
<project>-build
<project>-deploy-test
<project>-deploy-prod
```

Jenkins 视图可以按业务系统归组。任务页面使用参数化构建，避免“立即构建”在没有确认页面的情况下直接执行发布。

### 13.2 构建历史默认值

| 任务 | 保留天数 | 最多构建数 | Artifact 建议 |
|------|----------|------------|---------------|
| 构建 | 30 天 | 50 次 | 14 天或 30 次 |
| 测试发布 | 90 天 | 100 次 | 30 天或 50 次 |
| 生产发布 | 365 天 | 200 次 | 与审计要求一致 |
| 系统维护 | 90 天 | 100 次 | 只保留小型报告 |

Harbor 保存可运行镜像，Jenkins Artifact 只保存发布清单、测试报告和必要元数据，不重复归档大型镜像或完整依赖目录。

## 14. Jenkins 与 Docker 磁盘维护

### 14.1 清理优先级

推荐按以下顺序回收空间：

1. 已结束任务的无用 Workspace。
2. 已推送到 Registry 且长时间未使用的本地镜像。
3. 停止容器和无引用网络。
4. 超出上限的 BuildKit 缓存。
5. 明确标记为 CI 临时数据的 Volume。

BuildKit 缓存可以显著缩短 Maven、npm 和前端构建时间，不建议每天全部清空。通过最大占用和最小可用空间控制，比固定全量清理更稳定。

### 14.2 小型 Jenkins 节点参考值

对于磁盘约 50～100GB 的 Jenkins 节点，可以从以下默认值起步：

| 项目 | 默认值 |
|------|--------|
| 启动自动清理 | 磁盘使用率达到 75% |
| 磁盘告警 | 剩余空间低于 20% 或 10GB |
| 阻止继续调度 | 剩余空间低于 10% 或 5GB |
| 停止容器清理 | 超过 24 小时 |
| 未使用本地镜像清理 | 超过 14 天 |
| BuildKit 最低保留 | 4GB |
| BuildKit 最大占用 | 8GB |
| 清理后最小可用空间 | 10GB |

磁盘更大的节点可以按项目数量增加 BuildKit 上限。构建速度明显下降时，先确认缓存是否被过度清理，再增加 Executor 或 Agent。

### 14.3 Volume 安全边界

业务数据库、上传文件和中间件数据卷不参与 Jenkins 自动清理。CI 创建的临时 Volume 可以统一增加标签：

```text
com.example.ci.ephemeral=true
```

维护任务只处理带该标签且当前未被容器引用的 Volume。相比直接执行带 `--volumes` 的全局清理，这种方式更容易审计和回退。

## 15. 可观察性与最低验收证据

每次发布建议可以回读以下事实：

| 系统 | 最低证据 |
|------|----------|
| Git | 分支、完整 SHA、`VERSION`、正式 Tag、Release |
| Jenkins | 参数、运行人、选中应用、阶段结果、构建号 |
| Registry | 候选、SHA、测试和正式 Tag 指向的 digest |
| 测试环境 | 发布前后清单、所选应用 digest、健康检查和回滚结果 |
| 生产环境 | 发布前后清单、实际运行 digest、健康检查和发布时间 |
| 安全 | 日志与仓库中没有凭据，发布身份权限与环境匹配 |

只看到 Pipeline 绿色或接口返回一次成功状态，通常不足以证明发布完成。更可靠的完成条件是：目标环境运行 digest 与发布清单一致，并且约定健康检查全部通过。

## 16. 新项目默认落地顺序

1. 创建 `biz-<project>` 镜像 Project 和最小权限机器人账号。
2. 在仓库增加 `VERSION`、Dockerfile、`.dockerignore`、Compose 模板和部署说明。
3. 配置构建 Pipeline，验证候选版本、Git SHA、OCI Label 和 digest。
4. 配置测试发布 Pipeline，验证选择性发布、健康检查和回滚。
5. 对 Harbor Retention 执行 Dry Run，再启用候选 14 天、测试 30 天、生产永久规则。
6. 配置独立生产权限和生产 Pipeline。
7. 使用非关键版本完成一次受控生产演练。
8. 配置 Jenkins 构建记录、Workspace 和 Docker 缓存维护。
9. 把项目实际选择和偏离项写入项目自己的 `deploy/README.md`。

## 17. 偏离项记录模板

项目不采用某项默认值时，可以在部署说明中记录：

| 默认项 | 项目选择 | 原因 | 风险控制 | 复核日期 |
|--------|----------|------|----------|----------|
| 三段式独立 Pipeline |  |  |  |  |
| 一次构建、多环境晋级 |  |  |  |  |
| Docker Compose 部署 |  |  |  |  |
| 候选 14 天、测试 30 天、生产永久 |  |  |  |  |
| 生产创建 Git Tag 和 Release |  |  |  |  |
| Jenkins 75% 磁盘清理阈值 |  |  |  |  |

偏离记录用于让维护者理解当前选择，不自动成为审批门禁。

## 18. 关联入口

- [Mango 能力地图](../../capabilities/README.md)
- [业务接入场景手册](./README.md)
- [业务项目开发指南](../../designs/business-project-development-guide.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [文档资产归档边界](../../../mango-pmo/rules/06-document-assets.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
