# 新闻列表与详情多类型展示 E2E 验收证据

## 1. 验收范围

- 页面：企业官网首页、`/news`、`/company-news`、`/news/:id`、`/company-news/:id`。
- 接口：`/cms-api/sites/resolve`、`/cms-api/navigations/list`、`/cms-api/banners/list`、`/cms-api/advertisements/list`、`/cms-api/site-categories/tree`、`/cms-api/contents/page`、`/cms-api/contents/detail`。
- 权限：公开官网匿名访问接口，默认按 `domain=127.0.0.1:5191` 解析站点，不传默认 `siteCode`。
- 数据：Playwright 拦截 `/cms-api` 的仿真数据，覆盖 `ARTICLE`、`IMAGE_TEXT`、`PAGE`、`ATTACHMENT`、`VIDEO`。
- 部署形态：企业官网 App 使用当前运行服务 `http://127.0.0.1:5191`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:5191`。
- 后端地址：本轮专项 E2E 使用浏览器级 `/cms-api` 仿真响应验证前端展示分支；本地后端服务保留在 `http://127.0.0.1:18970`。
- 数据库或租户：本轮不写库；仿真数据只存在于 Playwright route。
- 测试账号：公开官网匿名访问。
- 浏览器：Playwright Chromium。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| NEWS-E2E-001 | `/` 与 `/cms-api/**` | 首页加载 CMS 公开接口数据 | 芒果科技站点、首页导航、HOME_HERO、HOME_FLOAT、新闻内容摘要 | 标题为芒果科技企业官网；导航含首页、新闻动态、公司动态；首页详情链接指向 `/news/content-article-1`；请求包含站点、导航、Banner、广告、栏目、内容分页接口 | 首页首屏、导航选中、广告面板、栏目卡片、底部友情链接可见 | console error 数组为空；CMS API failed response 数组为空；请求使用 `domain=127.0.0.1:5191` 且不传默认 `siteCode` | E2E 日志：`5 passed` | PASS |
| NEWS-E2E-002 | `/news`、`/news/content-image-1` | 新闻动态列表覆盖文章、图文、视频类型并跳转独立详情页 | `ARTICLE`、`IMAGE_TEXT`、`VIDEO` 三条新闻动态 | `/news` 列表为 3 条；列表显示文章、图文、视频标签；点击图文进入 `/news/content-image-1`；详情请求 contentId 为 `content-image-1` | 列表页无详情区嵌入；详情页无列表区；图文详情渲染富文本图片 alt | console error 数组为空；CMS API failed response 数组为空 | `mango-docs/evidence/2026-06-22-issue-214-cms/news-detail-list-e2e/enterprise-news-image-detail.png` | PASS |
| NEWS-E2E-003 | `/company-news`、`/company-news/content-company-attachment-1` | 公司动态列表覆盖单页、附件类型并展示详情元信息 | `PAGE`、`ATTACHMENT` 两条公司动态 | `/company-news` 列表为 2 条；列表显示单页、附件标签；附件详情显示附件、公司动态、品牌中心、芒果科技；附件链接 href 为 `https://example.com/cms/esg-report.pdf` | 公司动态导航选中；列表和详情为独立页面；详情侧边广告区域保持可见 | console error 数组为空；CMS API failed response 数组为空 | `mango-docs/evidence/2026-06-22-issue-214-cms/news-detail-list-e2e/enterprise-company-news-types.png` | PASS |
| NEWS-E2E-004 | `/news/content-article-1` | 富文本详情稳定性与清洗 | 文章详情包含 h2、strong、ul/li、blockquote、table、script、onclick | 详情显示文章标签、标题、富文本 h2、strong、3 个 li、blockquote、table；script 被移除；onclick 被移除；`window.__cms_xss` 未定义 | 详情页最小高度不低于 420px；正文表格、引用块样式可读；列表区未嵌入详情页 | console error 数组为空；CMS API failed response 数组为空 | `mango-docs/evidence/2026-06-22-issue-214-cms/news-detail-list-e2e/enterprise-news-rich-text-detail.png` | PASS |
| NEWS-E2E-005 | `/news/content-video-1` | 视频类型详情展示 | `VIDEO` 内容，正文含 `video controls` | 直接访问视频详情成功；详情显示视频标签、视频内容说明；富文本 video 节点保留 `controls` 属性；详情请求 contentId 为 `content-video-1` | 详情页无列表区；视频富文本在详情内容区内展示 | console error 数组为空；CMS API failed response 数组为空 | `mango-docs/evidence/2026-06-22-issue-214-cms/news-detail-list-e2e/enterprise-news-video-detail.png` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 企业官网 | `/news` | 内容类型标签展示 | 点击列表进入独立详情页 | 列表页不嵌入详情，当前导航高亮 | `enterprise-news-image-detail.png` | PASS |
| 企业官网 | `/company-news` | 单页和附件类型展示 | 附件详情元信息展示 | 列表、详情页面结构分离 | `enterprise-company-news-types.png` | PASS |
| 企业官网 | `/news/content-article-1` | 富文本标签渲染 | 不安全 HTML 清洗 | 最小高度、表格、引用块可读 | `enterprise-news-rich-text-detail.png` | PASS |
| 企业官网 | `/news/content-video-1` | 视频正文渲染 | 详情直达路由 | 视频节点不破坏详情布局 | `enterprise-news-video-detail.png` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 真实后台录入全类型数据后官网联动 | 本轮专项目标是前端列表/详情各种类型展示 E2E，使用浏览器 route 仿真 `/cms-api` 数据，不写数据库 | 不影响前端展示分支覆盖；真实数据链路仍以既有 CMS 管理端 E2E 和公开接口验收为准 | 后续验收可在管理后台用正常功能录入五种内容类型，再用官网真实接口复测 | 未要求 |
