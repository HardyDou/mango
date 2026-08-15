# Maven 发布适配器与停用的 Jenkins 方案

GitHub 只负责公开仓库的 PR、合并、tag 和 Release。GitHub Release 发布后，公网 GitHub-hosted Runner 在数秒内校验 tag、精确 SHA、`main` 可达性、版本锁和 CHANGELOG，并给该 Release 附加唯一机器授权清单 `mango-internal-release-request-v1.json`。GitHub 不再连接内网，也不再依赖私有 Self-hosted Runner。

内网 Jenkins Job `mango-github-release-watcher` 每两分钟读取公开 Release feed，只消费带上述授权清单的新 Release。它先校验清单、tag、SHA、版本锁和防倒退规则，再以精确参数调用 `mango-maven-release`。Mango 的事实源始终是 GitHub；Jenkins 从 GitHub 读取精确 SHA。Gitea 镜像仅可作为以后按需启用的缓存，不参与发布授权判断。

## 当前运行策略（2026-08-15）

Jenkins 自动 Maven 发布不作为正式入口。Mango 主仓正式发布唯一入口是仓库内 `mango-release`：发布负责人先在本地执行统一重型检查和 `mango release prepare`，由 prepare 调用 Maven batch 一次构建到本地 file repository 并封存 POM/JAR；Release PR 合并后才由 `mango release publish/repair` 发布这些精确文件。Jenkins Pipeline、Job 和状态账本只作为停用的历史方案保留，不得与本地状态机同时发布。

底层 batch 入口仍支持 dry-run 和本地 file repository 适配器验证，但不得绕过统一状态机直接完成正式发布：

```bash
scripts/publish-maven-batch.sh \
  --all-non-app \
  --release-version <version> \
  --repository-id mango-release-stage \
  --repository-url file:///absolute/runtime/stage \
  --skip-verify \
  --dry-run
```

发布成功必须以 publish/consume 双仓逐文件 SHA-256 回查和干净 Maven consumer 为准，不能用发布前的 `~/.m2` 缓存代替仓库证据。不可变版本不删除、不覆盖、不重发。

## 入口

- GitHub 请求 Workflow：`.github/workflows/maven-release.yml`
- 内网轮询器：`scripts/ci/github-release-jenkins-poller.sh`
- 轮询 Pipeline：`jenkins/mango-github-release-watcher.Jenkinsfile`
- 轮询 Job 配置：`jenkins/mango-github-release-watcher-job.xml`
- Maven Pipeline：`jenkins/mango-maven-release.Jenkinsfile`
- Maven Job 配置：`jenkins/mango-maven-release-job.xml`
- Jenkins Job：`mango-github-release-watcher` -> `mango-maven-release`

两个 Job XML 都内联对应 Jenkinsfile 的完全一致快照，测试会阻止漂移。Jenkins 启动构建时无需为了读取 Pipeline 再克隆整仓。

## 幂等、防倒退和失败处理

Jenkins 状态保存在 `${JENKINS_HOME}/release-state/mango/ledger.tsv`：

- 部署时先用 `bootstrap` 把既有 Release 记为基线，历史版本不会重放。
- 没有机器授权清单的 Release 永远不会发布。
- 不含 Maven 版本的 Release 记为 `ignored-no-maven`，不会误触发后端发布。
- 只有精确 tag/SHA 和版本合同校验完成后才写入 `claimed`。
- Nexus 回查全部成功后写入 `success`；发布失败写入 `failed`。
- `claimed`、`failed` 和 `success` 都不会被定时任务自动重试，避免不可变 Maven 制品在部分 deploy 后被覆盖。
- 低于或等于最近成功 Maven 版本的候选记为 `blocked-rollback`。

失败后的修复统一执行 `mango release status` 和取得当次授权后的 `mango release repair`：已经上传且 hash 一致的坐标只读回查，只有 publish/consume 双侧都证明不存在的坐标才允许首次发布；禁止整批盲目重跑或重新构建。

## Jenkins 运行时配置

Jenkins 容器或节点必须提供：

```text
MANGO_RELEASE_REPO_URL
MANGO_MAVEN_VERIFY_BASE_URL
```

`MANGO_RELEASE_REPO_URL` 当前指向公开主仓 `https://github.com/HardyDou/mango.git`。watcher 通过 GitHub Raw CDN 读取单个约 9KB 的 poller 脚本，不再为每次轮询 fetch/checkout 整仓。轮询公开 Atom feed 和公开 Release asset 不使用 GitHub API，因此不需要 GitHub Token，也不会消耗匿名 API 配额。Maven 发布凭据继续只保存在 Jenkins 的 `${JENKINS_HOME}/.m2/settings.xml`。

正式 Maven 发布由同一准备批次固定调用：

```bash
mango release publish \
  --maven-publish-registry <internal-maven-hosted-url> \
  --maven-consume-registry <internal-maven-group-url> \
  --maven-publish-server-id <settings-server-id> \
  --maven-consume-server-id <settings-server-id> \
  --authorize
```

默认不发布 `mango-app/**`、`*-app` 或 capability app 部署制品。

## GitHub Release 与最终完成语义

Tag 和 GitHub Release 只在 npm/Maven publish 与 consume 双仓回查、纯消费仓验证全部通过后创建。创建成功表示同一 manifest 已完成，不再作为触发 Nexus 发布的前置请求。若未来重新启用 Jenkins，必须先作为独立治理变更重新定义授权和状态归属，不能直接恢复历史 watcher 语义。

## 验证与部署

```bash
bash -n scripts/ci/github-release-jenkins-poller.sh
node --test scripts/tests/jenkins-release-bridge.test.mjs
node mango-pmo/tools/workspace-layout-check.mjs --root .
```

部署顺序固定为：停用旧 GitHub 私有 Runner 发布入口；安装 watcher Job 但先不启用；执行 `bootstrap`；启用两分钟定时器；用一个不含 Maven 版本的唯一 prerelease 验证发现、去重和精确 SHA（只会记录 `ignored-no-maven`，不会调用 Maven 发布 Job）；最后才允许正式 Release 自动发布。禁止双入口同时正式 deploy。需要验证 Maven dry-run 时，只能从 Jenkins 手工参数入口执行。
