# Mango 前端规范生产毕业执行记录

## 1. 目的与结论口径

本记录把前端规范从“规则文档”推进为可被 Mango 自身与业务项目共同执行的生产候选。唯一 Owner 是 `Frontend Standards Owner`，采用单拥有者模式；专家复核和机器报告提供证据，不形成第二审批链。

状态分为三层：

- `IMPLEMENTED`：代码、模板和门禁已落地。
- `PRODUCTION_CANDIDATE_LOCAL`：固定工具链、发布 tarball、封闭业务消费者、灰度/回退决策演练全部通过。
- `PRODUCTION_GRADUATED`：候选已合并并从 Nexus hosted/group 回读，真实单体与 Wujie 环境完成授权灰度和回退复验。

本分支当前只能根据实际证据进入前两层。push、PR、合并、Nexus 发布和生产流量操作不由本地检查替代。

## 2. 交付范围

1. Vue 3、Element Plus、Vite、TypeScript、ESLint、Prettier、Stylelint、Vitest 与 Playwright 的根级命令和棘轮门禁。
2. FE0 `HttpClient/HttpError/HttpProgress` 中立契约与 FE1 `@mango/http-client` Axios 适配器。
3. 业务 API、页面、组件和 CSS 的目录边界；CLI/starter 默认生成 host 注入式业务 API。
4. Wujie 只留在 runtime adapter 的厂商隔离边界，可单体、可微前端，开发配置与部署制品分离。
5. 29 个 npm 候选包的精确版本锁、干净消费者和回退矩阵。

## 3. 业务引用合同

```text
host/runtime
  -> createMangoHttpClient(baseUrl, token, tenant, trace)
  -> registerBusinessPages(client)
  -> business UI composition layer
  -> createBusinessApi(HttpClient)
  -> relative HTTP endpoint
```

- `frontend/packages/<module>-api`：DTO、Query、Command、VO 和 `createXxxApi(client)`；不依赖 Vue、Element Plus、Axios、环境变量或路由。
- `frontend/packages/<module>`：页面、页面私有组件、业务组合层和 package `style.css`。
- `frontend/src/main.ts`：创建 host 级请求实例并注入；base URL、登录态和租户属于 host。
- 页面私有 CSS 使用 scoped/module；跨页面样式随业务 package 显式导出；主题 token 位于平台主题层，微应用不依赖宿主穿透样式。

## 4. 灰度与回退

机器合同为 `mango-ui/frontend-release-rollout.json`，执行入口为：

```bash
cd mango-ui
pnpm release:rollout:check
```

流量固定按 internal 0% -> canary 5% -> limited 25% -> general 100% 推进。每阶段必须满足最短观察窗口；JavaScript 错误率、API 5xx 增量、刷新失败率、微应用 mount 失败率、白屏和消费者门禁任一越界，自动决策为 `rollback`。

回退顺序：停止放量 -> runtime registry/entry 恢复稳定制品 -> package lock 精确 pin 到 `origin/main` 矩阵 -> 新增的 `@mango/http-client` 从旧批次移除 -> 保留不可变稳定资产并清理入口缓存 -> 重跑 full gate 与业务 smoke。该批不含数据库迁移，API 必须向后兼容，不执行数据回退。

## 5. 执行门禁

| 阶段 | 必须通过 | 失败处理 |
| --- | --- | --- |
| 源码 | architecture/boundary/component、lint/format/style/type ratchet、unit/build | 修复或保持阻断，不抬高基线 |
| 发布包 | exports/style、release impact/notes、29 包 pack、公开声明无 Axios | 修复制品并重新 pack |
| 业务消费 | CLI full/module add、tarball consumer、封闭 offline Business Lab | 从生成和安装阶段重建 |
| 运行时 | 单体/Wujie、请求取消、刷新、卸载与样式 | 阻断对应部署模式 |
| 发布 | hosted/group exact-version 回读、0/5/25/100 灰度与失败回退 | 恢复稳定 registry/lock |

本地生产候选使用单一入口 `pnpm -C mango-ui check:production-candidate`。该入口先执行完整源码与 tarball 消费门禁，再在固定 Linux 镜像中准备 Business Lab，并通过独立 `--network none` 容器执行封闭安装、检查、构建、启动和停止；最终报告统一回写到 `.runtime/frontend-quality/business-lab`。

## 6. 完成定义

本地候选完成定义：工作树候选版本一致、所有本地和固定容器门禁退出码为 0、封闭业务项目不引用 workspace 源码、健康样本推进且异常样本回退、证据绑定 commit 与制品 hash。

生产毕业完成定义：在本地候选之上，远端 required check 成功，候选合并；Nexus hosted/group 回读全部精确版本；授权环境完成真实单体和 Wujie 浏览器 smoke；至少执行一次 5%/25% 灰度和故障注入回退，恢复后指标与 full gate 正常。任何一项没有真实证据，状态保持 `PRODUCTION_CANDIDATE_LOCAL`。
