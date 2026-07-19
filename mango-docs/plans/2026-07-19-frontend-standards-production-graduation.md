# Mango 前端规范落地与采用执行记录

## 1. 目的与结论口径

本记录把前端规范从“规则文档”推进为可被 Mango 自身与业务项目共同执行的工程基线。唯一 Owner 是 `Frontend Standards Owner`，采用单拥有者模式；专家复核和机器报告提供缺陷证据，不形成第二审批链。

状态分为三层：

- `STANDARD_VALIDATED_LOCAL`：固定工具链、候选 tarball、Mango 自身代码和封闭业务消费者验证通过。
- `STANDARD_READY_FOR_ADOPTION`：远端 CI、使用说明和至少一个真实业务试点验证通过，可以扩大采用范围。
- `STANDARD_ENFORCED`：目标仓库已启用强制门禁，新增和修改代码受新规则阻断，存量债务只减不增。

规范完成状态不代表任一业务应用已经发布生产。push、PR、合并、Nexus 发布和业务部署仍按各自流程取得授权与证据。

## 2. 交付范围

1. Vue 3、Element Plus、Vite、TypeScript、ESLint、Prettier、Stylelint、Vitest 与 Playwright 的根级命令和棘轮门禁。
2. FE0 `HttpClient/HttpError/HttpProgress` 中立契约与 FE1 `@mango/http-client` Axios 适配器。
3. 业务 API、页面、组件和 CSS 的目录边界；CLI/starter 默认生成 host 注入式业务 API。
4. 新增代码只能通过 runtime adapter 使用 Wujie；存量引用精确登记并持续下降。业务包可单体、可微前端，开发配置不得进入生产制品。
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

## 4. 采用阶段与依赖回退

机器合同为 `mango-ui/frontend-standards-adoption.json`，执行入口为：

```bash
cd mango-ui
pnpm standards:adoption:check
```

规范按 `pilot -> affected -> repository` 推进：先验证 CMS 与业务生成模板，再验证本次受影响 workspace 和独立消费者，最后启用全仓新增代码严格阻断与存量精确棘轮。阶段检查的是代码和消费对象覆盖率，不是用户流量。

npm 依赖回退只验证候选包可以精确 pin 到 `origin/main` 的稳定矩阵，新包可以从旧批次移除，并由干净业务消费者重新安装、类型检查和构建。真实环境的部署回滚不属于代码规范完成定义。

## 5. 执行门禁

| 阶段     | 必须通过                                                                    | 失败处理                   |
| -------- | --------------------------------------------------------------------------- | -------------------------- |
| 源码     | architecture/boundary/component、lint/format/style/type ratchet、unit/build | 修复或保持阻断，不抬高基线 |
| 发布包   | exports/style、release impact/notes、29 包 pack、公开声明无 Axios           | 修复制品并重新 pack        |
| 业务消费 | CLI full/module add、tarball consumer、封闭 offline Business Lab            | 从生成和安装阶段重建       |
| 运行时   | 测试环境单体/Wujie、请求取消、刷新、卸载与样式                              | 标记阻塞或修复后重测       |
| 采用     | pilot/affected/repository 覆盖、精确版本恢复矩阵                            | 停止扩大采用范围           |

本地生产候选使用单一入口 `pnpm -C mango-ui check:production-candidate`。该入口先执行完整源码与 tarball 消费门禁，再在固定 Linux 镜像中准备 Business Lab，并通过独立 `--network none` 容器执行封闭安装、检查、构建、启动和停止；最终报告统一回写到 `.runtime/frontend-quality/business-lab`。

## 6. 完成定义

`STANDARD_VALIDATED_LOCAL` 完成定义：提交版本一致、所有本地和固定容器门禁退出码为 0、封闭业务项目不引用 workspace 源码、三阶段采用合同和精确依赖恢复矩阵通过、证据绑定 commit、Git tree 与制品 hash。

`STANDARD_READY_FOR_ADOPTION` 还要求远端 required check 成功、规范说明可被业务开发直接使用，并在一个真实业务项目完成单体或 Wujie 对应形态的接入验证。`STANDARD_ENFORCED` 要求目标仓库启用强制门禁。Nexus 发布和具体业务生产部署分别使用独立发布证据，任何规范状态都不得表述为业务系统“已投产”。
