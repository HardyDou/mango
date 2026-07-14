# GitHub 到内网 Jenkins 发布桥接

GitHub 保留 PR、主分支和手工发布入口。公网前置 Job 校验 main 后只把受批准的桥接脚本作为短期 artifact 交给内网 `mango-release` Self-hosted Runner，Runner 无需克隆 Mango 仓库，主动调用只在内网可达的 Jenkins；Jenkins 再使用精确 Git SHA 执行 Maven 非 app 发布批次，并把最终状态经 Runner 返回 GitHub。

## 入口

- GitHub Workflow：`.github/workflows/maven-release.yml`
- Runner 桥接脚本：`scripts/ci/jenkins-release-bridge.sh`
- Jenkins Pipeline：`jenkins/mango-maven-release.Jenkinsfile`
- Jenkins Job 配置：`jenkins/mango-maven-release-job.xml`
- Jenkins Job：`mango-maven-release`

Job XML 内联的是上述 Jenkinsfile 的完全一致快照，单元测试会阻止两者漂移。这样 Jenkins 启动构建时无需先为读取 Jenkinsfile 克隆整仓。Pipeline 首次检出精确 SHA 后保留工作区的 Git 对象库；后续构建先执行 `reset --hard` 和 `clean -ffdx`，只获取新增对象并且不下载无关 tags。

GitHub Workflow 只允许从 `main` 手工执行，默认 `dry_run=true`。正式发布要求输入版本等于 `mango-ui/packages/mango-cli/release-versions.json` 的 `maven.mangoBackend`，并要求 `CHANGELOG.md` 已记录该 Maven 版本。

## 内网 Runner 配置

Runner 使用标签：

```text
self-hosted
linux
x64
mango-release
```

以下配置只保存在 Runner 服务环境中，不提交仓库：

```text
JENKINS_URL
JENKINS_USER
JENKINS_API_TOKEN
JENKINS_JOB=mango-maven-release
```

Jenkins API Token 不写入 Workflow、日志或 GitHub Secret。Runner 通过临时 `netrc` 调用 Jenkins，脚本退出时删除临时凭据文件。

## Jenkins 运行时配置

Jenkins 容器或节点必须提供：

```text
MANGO_RELEASE_REPO_URL
MANGO_MAVEN_VERIFY_BASE_URL
```

`MANGO_RELEASE_REPO_URL` 可以指向内网 Gitea 镜像；Pipeline 最多等待 60 秒让镜像出现 GitHub 指定 SHA，并验证该 SHA 可从镜像 `main` 到达。`MANGO_MAVEN_VERIFY_BASE_URL` 指向内网 Nexus Maven 消费仓库。

Jenkins 首次缺少 Maven 3.9.9 时优先从高速镜像下载，随后使用 Apache 官方 `.sha512` 校验；镜像不可用时回退 Apache 官方归档。安装结果保存在 `${JENKINS_HOME}/.tools`，后续发布不会再次下载 Maven。

正式发布固定调用：

```bash
scripts/publish-maven-batch.sh \
  --all-non-app \
  --release-version <version> \
  --verify-base-url <internal-nexus-url>
```

不会默认发布 `mango-app/**`、`*-app` 或 capability app 部署制品。发布凭据继续由 Jenkins 的 Maven `settings.xml` 管理。

## 验证

```bash
bash -n scripts/ci/jenkins-release-bridge.sh
node --test scripts/tests/jenkins-release-bridge.test.mjs
```

首次联通只执行唯一 prerelease 版本的 dry-run；确认 GitHub、Runner、Jenkins 三段状态一致后，才允许在人工确认下执行 `dry_run=false`。
