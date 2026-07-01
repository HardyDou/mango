import{_ as n,o as s,c as e,a2 as t}from"./chunks/framework.Fbjs98zA.js";const u=JSON.parse('{"title":"保函业务系统租户模型调研与 Mango 差距分析","description":"","frontmatter":{"title":"保函业务系统租户模型调研与 Mango 差距分析","type":"architecture-research","status":"draft","date":"2026-05-09T00:00:00.000Z"},"headers":[],"relativePath":"mango-docs/tenant-model-research-guarantee-business.md","filePath":"mango-docs/tenant-model-research-guarantee-business.md"}'),p={name:"mango-docs/tenant-model-research-guarantee-business.md"};function l(i,a,d,c,o,r){return s(),e("div",null,[...a[0]||(a[0]=[t(`<h1 id="保函业务系统租户模型调研与-mango-差距分析" tabindex="-1">保函业务系统租户模型调研与 Mango 差距分析 <a class="header-anchor" href="#保函业务系统租户模型调研与-mango-差距分析" aria-label="Permalink to &quot;保函业务系统租户模型调研与 Mango 差距分析&quot;">​</a></h1><h2 id="_1-结论先行" tabindex="-1">1. 结论先行 <a class="header-anchor" href="#_1-结论先行" aria-label="Permalink to &quot;1. 结论先行&quot;">​</a></h2><p>保函业务系统里的“租户”不应该等同于用户、部门、菜单、系统模块，也不应该简单等同于每一笔业务的参与方。</p><p>在本系统里，租户应定义为：</p><blockquote><p>一个拥有独立组织架构、人员账号、角色权限、审批流程、业务数据边界、审计边界和配置边界的企业级使用主体。</p></blockquote><p>结合当前业务，元丰行、智诚等融资性担保公司、使用本系统处理业务的银行，都应作为企业租户。客户目前以个人账号为准，原则上不是租户，而是外部客户身份；只有当客户企业需要自己的组织、员工、权限、审批、门户配置时，才升级为客户企业租户。</p><p>最重要的一点是：保函业务是多企业协同，不是单企业内部 OA。租户之间默认隔离，但同一笔保函业务需要让元丰行、下游担保公司、银行在授权范围内共享资料、状态、任务和审批结论。因此系统不能只靠 <code>tenant_id = 当前租户</code> 的简单过滤解决所有问题，还需要“业务参与方”和“跨租户授权共享”模型。</p><h2 id="_2-外部调研结论" tabindex="-1">2. 外部调研结论 <a class="header-anchor" href="#_2-外部调研结论" aria-label="Permalink to &quot;2. 外部调研结论&quot;">​</a></h2><h3 id="_2-1-通用-saas-租户定义" tabindex="-1">2.1 通用 SaaS 租户定义 <a class="header-anchor" href="#_2-1-通用-saas-租户定义" aria-label="Permalink to &quot;2.1 通用 SaaS 租户定义&quot;">​</a></h3><p>Microsoft Azure 架构文档对多租户的定义是：一个解决方案服务多个客户或租户；租户不同于用户，一个组织、公司或用户组通常形成一个租户。B2B 软件里，租户通常映射到客户企业；但如果客户企业内部存在国家、区域、事业部等强隔离诉求，也可以拆为多个租户。</p><p>AWS SaaS 文档强调，租户隔离不同于普通认证授权。用户登录成功、有角色权限，并不自动意味着租户隔离已经成立。系统必须基于当前 tenant context 限制资源访问，阻止访问其他租户的数据。</p><p>Salesforce 的多租户平台以 org 作为核心隔离单元，数据、元数据、权限、安全域和查询优化都围绕 org 进行。这个思路对 Mango 有参考价值：租户不只是数据字段，而是配置、权限、流程、审计、扩展能力的边界。</p><p>AWS 还区分 data partitioning 和 tenant isolation：数据分区只是“怎么存”，隔离是“如何确保不能越权访问”。这对 Mango 很关键，因为当前使用 <code>tenant_id</code> 行级过滤只是分区/过滤手段的一部分，不等于完整租户能力。</p><p>参考资料：</p><ul><li>Microsoft Azure: <a href="https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/overview" target="_blank" rel="noreferrer">Architect multitenant solutions on Azure</a></li><li>Microsoft Azure: <a href="https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models" target="_blank" rel="noreferrer">Tenancy models for a multitenant solution</a></li><li>AWS: <a href="https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/tenant-isolation.html" target="_blank" rel="noreferrer">Tenant isolation</a></li><li>AWS: <a href="https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/saas-identity.html" target="_blank" rel="noreferrer">SaaS identity</a></li><li>AWS: <a href="https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/data-partitioning.html" target="_blank" rel="noreferrer">Data partitioning</a></li><li>Salesforce: <a href="https://architect.salesforce.com/docs/architect/fundamentals/guide/platform-multitenant-architecture.html" target="_blank" rel="noreferrer">Platform Multitenant Architecture</a></li></ul><h3 id="_2-2-erp-oa-saas-中租户真实解决的问题" tabindex="-1">2.2 ERP/OA/SaaS 中租户真实解决的问题 <a class="header-anchor" href="#_2-2-erp-oa-saas-中租户真实解决的问题" aria-label="Permalink to &quot;2.2 ERP/OA/SaaS 中租户真实解决的问题&quot;">​</a></h3><p>租户真正解决的是企业级隔离和企业级自治：</p><table tabindex="0"><thead><tr><th>能力</th><th>租户要解决的问题</th></tr></thead><tbody><tr><td>数据隔离</td><td>A 公司不能看到 B 公司的客户、合同、审批、附件、业务单据</td></tr><tr><td>组织隔离</td><td>每个企业维护自己的部门、岗位、人员关系</td></tr><tr><td>权限隔离</td><td>每个企业有自己的角色、用户授权、菜单授权</td></tr><tr><td>流程隔离</td><td>每个企业有自己的审批流程、风控节点、印章/签约规则</td></tr><tr><td>配置隔离</td><td>每个企业有自己的参数、通知模板、业务规则、材料清单</td></tr><tr><td>审计隔离</td><td>每个企业只能审计本企业操作；平台可按授权审计全局</td></tr><tr><td>运营隔离</td><td>租户可开通、停用、计费、限额、分配套餐</td></tr><tr><td>合规隔离</td><td>金融、担保、银行等机构可能需要更强数据边界和日志留存</td></tr></tbody></table><p>什么时候需要租户：</p><ul><li>一套系统服务多个企业客户。</li><li>每个企业有独立组织、角色、审批、业务数据。</li><li>企业之间不能默认互相看数据。</li><li>需要按企业开通、停用、计费、审计。</li><li>需要平台统一升级，但企业独立使用。</li></ul><p>什么时候不应该用租户：</p><ul><li>公司内部部门不是租户，是组织。</li><li>系统模块不是租户，是应用/菜单/功能。</li><li>用户身份不是租户，是账号。</li><li>一笔业务的参与方也不天然是租户；参与方是业务关系，只有参与方企业本身需要独立后台能力时才是租户。</li></ul><h2 id="_3-保函业务场景拆解" tabindex="-1">3. 保函业务场景拆解 <a class="header-anchor" href="#_3-保函业务场景拆解" aria-label="Permalink to &quot;3. 保函业务场景拆解&quot;">​</a></h2><h3 id="_3-1-当前业务描述" tabindex="-1">3.1 当前业务描述 <a class="header-anchor" href="#_3-1-当前业务描述" aria-label="Permalink to &quot;3.1 当前业务描述&quot;">​</a></h3><p>目前涉及两类主链路：</p><ol><li><p>元丰行直接开商保：</p><ul><li>客户申请开保函。</li><li>元丰行第一手接单。</li><li>元丰行整理签约资料、风控、审批。</li><li>元丰行直接出具商业保函。</li></ul></li><li><p>元丰行将银行保函业务倒流给融资性担保公司：</p><ul><li>客户申请银行保函。</li><li>元丰行接单、整理资料、签约、初步风控。</li><li>元丰行递交给智诚等融资性担保公司。</li><li>智诚等担保公司内部风控、审批。</li><li>担保公司将资料提交银行。</li><li>银行最终开具银行保函。</li><li>多数银行使用自有系统或线下处理，少数银行可能使用本系统处理。</li></ul></li></ol><h3 id="_3-2-参与方分类" tabindex="-1">3.2 参与方分类 <a class="header-anchor" href="#_3-2-参与方分类" aria-label="Permalink to &quot;3.2 参与方分类&quot;">​</a></h3><table tabindex="0"><thead><tr><th>参与方</th><th style="text-align:right;">是否租户</th><th>原因</th></tr></thead><tbody><tr><td>客户个人</td><td style="text-align:right;">否，默认是外部个人账号</td><td>目前客户以个人账号为准，没有独立组织、菜单、审批、员工管理诉求</td></tr><tr><td>客户企业</td><td style="text-align:right;">视情况</td><td>如果只是申请主体资料，不是租户；如果客户企业也要多人协同、审批、授权，则可以成为客户租户</td></tr><tr><td>元丰行</td><td style="text-align:right;">是</td><td>第一手接单方，需要组织、角色、风控流程、业务台账、签约资料管理</td></tr><tr><td>智诚等融资性担保公司</td><td style="text-align:right;">是</td><td>独立企业，独立风控、审批、资料管理、角色权限、审计</td></tr><tr><td>银行</td><td style="text-align:right;">视接入方式</td><td>使用本系统处理业务的银行应作为租户；只通过接口/线下接收资料的银行可先作为外部机构档案</td></tr><tr><td>平台运营方</td><td style="text-align:right;">是，平台空间/平台租户</td><td>负责租户开通、全局应用、菜单、字典、运营配置、审计运维</td></tr></tbody></table><h2 id="_4-推荐租户模型" tabindex="-1">4. 推荐租户模型 <a class="header-anchor" href="#_4-推荐租户模型" aria-label="Permalink to &quot;4. 推荐租户模型&quot;">​</a></h2><h3 id="_4-0-三个概念必须分开" tabindex="-1">4.0 三个概念必须分开 <a class="header-anchor" href="#_4-0-三个概念必须分开" aria-label="Permalink to &quot;4.0 三个概念必须分开&quot;">​</a></h3><p>保函业务里最容易混淆的是“租户、业务参与方、权限角色”。</p><table tabindex="0"><thead><tr><th>概念</th><th>回答的问题</th><th>示例</th></tr></thead><tbody><tr><td>租户 / 企业空间</td><td>谁拥有独立后台、组织、成员、权限、流程、数据边界</td><td>元丰行企业空间、智诚担保企业空间、某银行企业空间</td></tr><tr><td>业务参与方</td><td>一笔保函业务中谁参与、承担什么职责</td><td>接单方、担保方、开函银行、申请人</td></tr><tr><td>权限角色</td><td>某个人在某个企业空间内能做什么</td><td>客户经理、风控员、审批人、资料员、管理员</td></tr></tbody></table><p>所以元丰行、智诚、银行、客户不是 RBAC 角色。它们首先是业务参与方类型或机构类型；当这个机构需要自己的员工登录、组织架构、菜单权限和审批流程时，它同时也是一个租户。</p><p>不要建这种角色：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>role = 元丰行</span></span>
<span class="line"><span>role = 智诚</span></span>
<span class="line"><span>role = 银行</span></span></code></pre></div><p>应该建：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>tenant = 元丰行</span></span>
<span class="line"><span>tenant_type = CHANNEL_COMPANY</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant = 智诚担保</span></span>
<span class="line"><span>tenant_type = GUARANTEE_COMPANY</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant = 某银行</span></span>
<span class="line"><span>tenant_type = BANK</span></span>
<span class="line"><span></span></span>
<span class="line"><span>role = 客户经理 / 风控员 / 审批人 / 管理员</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_participant = 某笔业务中，元丰行是接单方，智诚是担保方，某银行是开函方</span></span></code></pre></div><h3 id="_4-1-核心模型" tabindex="-1">4.1 核心模型 <a class="header-anchor" href="#_4-1-核心模型" aria-label="Permalink to &quot;4.1 核心模型&quot;">​</a></h3><p>建议采用“平台统一部署 + 企业租户隔离 + 业务协同授权”的模型。</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>platform</span></span>
<span class="line"><span>  管理租户、应用、菜单、接口资源、基础字典、行政区划、套餐、开通状态</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant</span></span>
<span class="line"><span>  元丰行</span></span>
<span class="line"><span>  智诚担保</span></span>
<span class="line"><span>  其他融资性担保公司</span></span>
<span class="line"><span>  接入本系统的银行</span></span>
<span class="line"><span>  可选：需要企业门户能力的客户企业</span></span>
<span class="line"><span></span></span>
<span class="line"><span>identity_user</span></span>
<span class="line"><span>  全局登录身份。一个手机号/账号可以加入多个租户。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant_member</span></span>
<span class="line"><span>  某个身份账号在某个租户里的成员身份。</span></span>
<span class="line"><span>  例如 admin 在元丰行是平台管理员，在智诚是实施顾问。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>org / post</span></span>
<span class="line"><span>  租户内部组织和岗位。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>role / permission</span></span>
<span class="line"><span>  租户内部角色授权。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case / application</span></span>
<span class="line"><span>  保函申请主业务。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_participant</span></span>
<span class="line"><span>  一笔业务里有哪些企业参与，每个企业是什么角色。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_share / document_share</span></span>
<span class="line"><span>  跨租户共享资料、任务、审批结论和状态的授权边界。</span></span></code></pre></div><h3 id="_4-2-为什么需要-tenant-member" tabindex="-1">4.2 为什么需要 tenant_member <a class="header-anchor" href="#_4-2-为什么需要-tenant-member" aria-label="Permalink to &quot;4.2 为什么需要 tenant_member&quot;">​</a></h3><p><code>identity_user</code> 只回答“谁能登录”，不能回答“这个人在某个租户里是什么身份”。</p><p>同一个人可能在多个企业中出现：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>张三账号</span></span>
<span class="line"><span>  在元丰行：客户经理</span></span>
<span class="line"><span>  在智诚担保：外部协作顾问</span></span>
<span class="line"><span>  在平台：实施支持人员</span></span></code></pre></div><p>所以正式模型应拆开：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>identity_user      全局账号</span></span>
<span class="line"><span>tenant_member      账号在某个租户内的成员身份</span></span>
<span class="line"><span>member_org_post    成员在租户内属于哪个组织/岗位</span></span>
<span class="line"><span>subject_role       成员或账号在租户内绑定哪些角色</span></span></code></pre></div><p>没有 <code>tenant_member</code>，系统会长期混淆：</p><ul><li>账号是否属于某个租户。</li><li>登录时是否允许选择某个租户。</li><li>用户管理列表到底列租户成员还是全局身份账号。</li><li>离职/禁用是禁用全局账号，还是禁用某个租户内成员身份。</li><li>一个客户经理跨多个租户时，权限和组织如何表达。</li></ul><h3 id="_4-3-保函业务中的跨租户协同" tabindex="-1">4.3 保函业务中的跨租户协同 <a class="header-anchor" href="#_4-3-保函业务中的跨租户协同" aria-label="Permalink to &quot;4.3 保函业务中的跨租户协同&quot;">​</a></h3><p>不能把跨租户协同理解成“关闭租户隔离”。正确做法是：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>默认隔离</span></span>
<span class="line"><span>  元丰行看不到智诚全部业务。</span></span>
<span class="line"><span>  智诚看不到元丰行全部业务。</span></span>
<span class="line"><span>  银行看不到担保公司全部业务。</span></span>
<span class="line"><span></span></span>
<span class="line"><span>显式共享</span></span>
<span class="line"><span>  对某一笔保函申请，系统创建 case_participant。</span></span>
<span class="line"><span>  元丰行、智诚、银行只在该业务范围内看到被授权的字段、附件、任务、审批结论。</span></span></code></pre></div><p>推荐模型：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>guarantee_case</span></span>
<span class="line"><span>  id</span></span>
<span class="line"><span>  case_no</span></span>
<span class="line"><span>  applicant_user_id</span></span>
<span class="line"><span>  applicant_customer_id</span></span>
<span class="line"><span>  product_type: COMMERCIAL_GUARANTEE / BANK_GUARANTEE</span></span>
<span class="line"><span>  source_tenant_id: 元丰行</span></span>
<span class="line"><span>  status</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_participant</span></span>
<span class="line"><span>  case_id</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  participant_type:</span></span>
<span class="line"><span>    SOURCE_CHANNEL       元丰行</span></span>
<span class="line"><span>    GUARANTEE_COMPANY    智诚等融资性担保公司</span></span>
<span class="line"><span>    BANK                 银行</span></span>
<span class="line"><span>    CUSTOMER_PORTAL      客户企业，可选</span></span>
<span class="line"><span>  role_in_case</span></span>
<span class="line"><span>  access_level</span></span>
<span class="line"><span>  joined_at</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_task</span></span>
<span class="line"><span>  case_id</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  assignee_member_id</span></span>
<span class="line"><span>  task_type</span></span>
<span class="line"><span>  status</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_document</span></span>
<span class="line"><span>  case_id</span></span>
<span class="line"><span>  owner_tenant_id</span></span>
<span class="line"><span>  document_type</span></span>
<span class="line"><span>  storage_id</span></span>
<span class="line"><span>  visibility_scope</span></span>
<span class="line"><span></span></span>
<span class="line"><span>case_document_share</span></span>
<span class="line"><span>  document_id</span></span>
<span class="line"><span>  target_tenant_id</span></span>
<span class="line"><span>  permission: VIEW / DOWNLOAD / UPDATE / SIGN</span></span></code></pre></div><p>这个模型的关键点：</p><ul><li>业务主单可以有一个发起租户，但不代表只有发起租户能参与。</li><li>每个参与租户看到的是“自己参与的业务视图”，不是全量业务库。</li><li>资料共享要按文件、字段、阶段授权，不能简单全量开放。</li><li>审批流按租户内部执行，但跨租户流转由业务编排连接。</li></ul><h2 id="_5-租户内与平台级数据边界" tabindex="-1">5. 租户内与平台级数据边界 <a class="header-anchor" href="#_5-租户内与平台级数据边界" aria-label="Permalink to &quot;5. 租户内与平台级数据边界&quot;">​</a></h2><h3 id="_5-1-建议不按租户隔离的数据" tabindex="-1">5.1 建议不按租户隔离的数据 <a class="header-anchor" href="#_5-1-建议不按租户隔离的数据" aria-label="Permalink to &quot;5.1 建议不按租户隔离的数据&quot;">​</a></h3><p>这些是平台元数据，应全局维护，租户按授权使用：</p><table tabindex="0"><thead><tr><th>数据</th><th>原因</th></tr></thead><tbody><tr><td>应用定义</td><td>系统能力由平台统一发布</td></tr><tr><td>菜单定义</td><td>菜单是产品能力，不应每个租户复制一份</td></tr><tr><td>接口资源</td><td>API 是平台能力</td></tr><tr><td>基础行政区划</td><td>全国行政区划是公共基础数据</td></tr><tr><td>系统级字典</td><td>如状态、操作者类型、登录域等基础字典</td></tr><tr><td>产品包/套餐</td><td>平台运营配置</td></tr><tr><td>机构类型基础档案</td><td>银行、担保公司类型等基础枚举</td></tr></tbody></table><h3 id="_5-2-建议按租户隔离的数据" tabindex="-1">5.2 建议按租户隔离的数据 <a class="header-anchor" href="#_5-2-建议按租户隔离的数据" aria-label="Permalink to &quot;5.2 建议按租户隔离的数据&quot;">​</a></h3><p>这些数据属于租户内部治理，应按 <code>tenant_id</code> 隔离：</p><table tabindex="0"><thead><tr><th>数据</th><th>说明</th></tr></thead><tbody><tr><td>组织架构</td><td>每个企业不同</td></tr><tr><td>岗位</td><td>每个企业不同</td></tr><tr><td>租户成员</td><td>每个企业自己的成员</td></tr><tr><td>角色</td><td>每个企业自己的角色</td></tr><tr><td>角色菜单授权</td><td>同一菜单能力，不同企业授权不同</td></tr><tr><td>审批流程定义</td><td>元丰行、智诚、银行流程不同</td></tr><tr><td>业务规则配置</td><td>材料清单、风控规则、额度规则等</td></tr><tr><td>通知模板</td><td>每个企业可有自己的短信/邮件/系统消息模板</td></tr><tr><td>操作日志</td><td>按租户审计</td></tr><tr><td>保函业务内部处理数据</td><td>每个企业自己的处理意见、审批记录、任务</td></tr></tbody></table><h3 id="_5-3-需要跨租户授权的数据" tabindex="-1">5.3 需要跨租户授权的数据 <a class="header-anchor" href="#_5-3-需要跨租户授权的数据" aria-label="Permalink to &quot;5.3 需要跨租户授权的数据&quot;">​</a></h3><p>这些数据不能简单“全局共享”或“完全隔离”，必须由业务参与关系控制：</p><table tabindex="0"><thead><tr><th>数据</th><th>推荐控制方式</th></tr></thead><tbody><tr><td>保函申请主信息</td><td><code>case_participant</code> 控制可见</td></tr><tr><td>客户资料</td><td>按申请、字段、资料类型授权</td></tr><tr><td>签约资料</td><td>按当前办理阶段和接收方授权</td></tr><tr><td>风控结论</td><td>可共享结论，不一定共享内部评分细节</td></tr><tr><td>银行递交资料</td><td>只给目标银行或接口通道</td></tr><tr><td>审批状态</td><td>可共享节点状态，内部审批意见按规则脱敏</td></tr><tr><td>附件</td><td><code>document_share</code> 控制查看/下载/更新/签署</td></tr></tbody></table><h2 id="_6-登录与身份建议" tabindex="-1">6. 登录与身份建议 <a class="header-anchor" href="#_6-登录与身份建议" aria-label="Permalink to &quot;6. 登录与身份建议&quot;">​</a></h2><h3 id="_6-1-个人客户登录" tabindex="-1">6.1 个人客户登录 <a class="header-anchor" href="#_6-1-个人客户登录" aria-label="Permalink to &quot;6.1 个人客户登录&quot;">​</a></h3><p>客户以个人账号为准，建议进入客户门户，不进入企业管理后台。</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>realm = CUSTOMER</span></span>
<span class="line"><span>actor_type = CUSTOMER_USER</span></span>
<span class="line"><span>tenant_id = 可为空或绑定到平台客户域</span></span></code></pre></div><p>客户申请保函时，创建客户档案和保函申请，不创建企业租户，除非客户企业需要多人协同。</p><h3 id="_6-2-企业员工登录" tabindex="-1">6.2 企业员工登录 <a class="header-anchor" href="#_6-2-企业员工登录" aria-label="Permalink to &quot;6.2 企业员工登录&quot;">​</a></h3><p>元丰行、智诚、银行员工应先选择租户，或系统根据账号所属租户列出可选项。</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>账号登录</span></span>
<span class="line"><span>  -&gt; 查询该账号可进入的 tenant_member</span></span>
<span class="line"><span>  -&gt; 用户选择企业</span></span>
<span class="line"><span>  -&gt; token 写入 tenantId、memberId、realm、actorType、partyType、partyId、appCode</span></span></code></pre></div><p>重点：登录时不能只校验租户存在，还必须校验账号是该租户成员，或拥有平台授权的跨租户支持身份。</p><h3 id="_6-3-平台运维-实施账号" tabindex="-1">6.3 平台运维/实施账号 <a class="header-anchor" href="#_6-3-平台运维-实施账号" aria-label="Permalink to &quot;6.3 平台运维/实施账号&quot;">​</a></h3><p>平台人员可能需要进入多个租户处理问题。不要用全局超级权限绕过模型，应该显式授权：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>platform_support_grant</span></span>
<span class="line"><span>  user_id</span></span>
<span class="line"><span>  target_tenant_id</span></span>
<span class="line"><span>  scope</span></span>
<span class="line"><span>  reason</span></span>
<span class="line"><span>  start_time</span></span>
<span class="line"><span>  expire_time</span></span>
<span class="line"><span>  approved_by</span></span></code></pre></div><p>这样可以保证进入租户的行为可审计、可过期、可追责。</p><h2 id="_7-mango-当前租户实现现状" tabindex="-1">7. Mango 当前租户实现现状 <a class="header-anchor" href="#_7-mango-当前租户实现现状" aria-label="Permalink to &quot;7. Mango 当前租户实现现状&quot;">​</a></h2><p>基于当前代码和数据库，Mango 已有这些能力：</p><table tabindex="0"><thead><tr><th>能力</th><th>当前状态</th></tr></thead><tbody><tr><td>租户表</td><td><code>sys_tenant</code> 已有，当前有芒果集团、A公司、B公司、C公司</td></tr><tr><td>登录选择租户</td><td><code>LoginCommand</code> 支持 <code>tenantId/tenantCode</code>，登录 token 写入租户上下文</td></tr><tr><td>租户上下文</td><td><code>MangoContextHolder</code> 持有 <code>tenantId/appCode/realm/actorType/partyType/partyId</code></td></tr><tr><td>行级租户插件</td><td>MyBatis-Plus TenantLine 拦截器已启用</td></tr><tr><td>排除平台元数据表</td><td><code>sys_tenant</code>、字典、行政区划、应用、菜单、接口资源、身份用户等已排除</td></tr><tr><td>角色租户隔离</td><td><code>authorization_role</code> 已按 <code>tenant_id + app_code + role_code</code> 唯一</td></tr><tr><td>主体角色绑定</td><td><code>authorization_subject_role</code> 已带 <code>tenant_id/app_code/realm/actor_type/party_type/party_id</code></td></tr><tr><td>组织/岗位隔离</td><td><code>sys_org</code>、<code>org_post</code> 已带租户字段</td></tr><tr><td>用户菜单</td><td><code>/authorization/menus/user?fmt=tree</code> 按当前登录上下文返回菜单</td></tr></tbody></table><p>当前测试数据现状：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>租户：</span></span>
<span class="line"><span>  1 芒果集团</span></span>
<span class="line"><span>  2 A公司</span></span>
<span class="line"><span>  3 B公司</span></span>
<span class="line"><span>  4 C公司</span></span>
<span class="line"><span></span></span>
<span class="line"><span>身份账号：</span></span>
<span class="line"><span>  admin 只有一个全局身份账号</span></span>
<span class="line"><span></span></span>
<span class="line"><span>角色：</span></span>
<span class="line"><span>  每个租户都有 ROLE_ADMIN</span></span>
<span class="line"><span></span></span>
<span class="line"><span>角色绑定：</span></span>
<span class="line"><span>  同一个 admin 被绑定到 4 个租户的 ROLE_ADMIN</span></span></code></pre></div><p>这说明当前不是“每个租户一个独立超级账号”，而是“同一个全局账号可进入多个租户并获得对应租户角色”。</p><p>这个方向可以成立，但缺少正式的租户成员模型。</p><h2 id="_8-mango-与理论最佳模型的差距" tabindex="-1">8. Mango 与理论最佳模型的差距 <a class="header-anchor" href="#_8-mango-与理论最佳模型的差距" aria-label="Permalink to &quot;8. Mango 与理论最佳模型的差距&quot;">​</a></h2><h3 id="_8-1-缺少-tenant-member" tabindex="-1">8.1 缺少 tenant_member <a class="header-anchor" href="#_8-1-缺少-tenant-member" aria-label="Permalink to &quot;8.1 缺少 tenant_member&quot;">​</a></h3><p>当前 <code>identity_user</code> 被排除在租户插件之外，但表上又存在 <code>tenant_id</code>、<code>party_type</code>、<code>party_id</code> 字段。这会造成概念混乱：</p><ul><li><code>identity_user</code> 到底是全局账号，还是租户内用户？</li><li><code>identity_user.tenant_id</code> 是否可信？</li><li>用户管理页面管理的是全局账号还是租户成员？</li><li>一个账号加入多个企业时，成员状态、组织、岗位、离职如何表达？</li></ul><p>建议新增：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>tenant_member</span></span>
<span class="line"><span>  id</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  user_id</span></span>
<span class="line"><span>  member_no</span></span>
<span class="line"><span>  display_name</span></span>
<span class="line"><span>  member_type: EMPLOYEE / EXTERNAL / SUPPORT</span></span>
<span class="line"><span>  status: ENABLED / DISABLED / LEFT</span></span>
<span class="line"><span>  primary_org_id</span></span>
<span class="line"><span>  primary_post_id</span></span></code></pre></div><p>并逐步把租户内用户管理从 <code>identity_user</code> 迁移为“成员管理”。</p><h3 id="_8-2-登录时未强校验账号是否属于租户" tabindex="-1">8.2 登录时未强校验账号是否属于租户 <a class="header-anchor" href="#_8-2-登录时未强校验账号是否属于租户" aria-label="Permalink to &quot;8.2 登录时未强校验账号是否属于租户&quot;">​</a></h3><p>当前登录流程会校验租户存在且启用，然后加载该租户下的角色权限。但模型上还缺少“账号是否允许进入该租户”的独立校验。</p><p>短期看，未绑定角色的账号可能登录成功但没有权限；长期看，这会让登录语义不严谨。</p><p>建议：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>登录选择租户</span></span>
<span class="line"><span>  -&gt; 校验租户启用</span></span>
<span class="line"><span>  -&gt; 校验 identity_user 与 tenant_member 关系存在且启用</span></span>
<span class="line"><span>  -&gt; 校验成员可进入 appCode/realm/actorType</span></span>
<span class="line"><span>  -&gt; 生成 token</span></span></code></pre></div><h3 id="_8-3-用户、成员、主体角色边界不清" tabindex="-1">8.3 用户、成员、主体角色边界不清 <a class="header-anchor" href="#_8-3-用户、成员、主体角色边界不清" aria-label="Permalink to &quot;8.3 用户、成员、主体角色边界不清&quot;">​</a></h3><p>当前 <code>authorization_subject_role.subject_id</code> 指向的是 <code>identity_user.id</code>。如果未来引入 <code>tenant_member</code>，需要明确角色到底绑定给谁：</p><p>推荐：</p><ul><li>登录账号级角色极少使用，只用于平台级特殊账号。</li><li>普通企业权限绑定给 <code>tenant_member</code>。</li><li>客户个人门户权限可绑定给 <code>customer_user</code> 或 <code>identity_user + CUSTOMER realm</code>。</li></ul><p>否则同一个账号跨租户、跨身份时，权限边界会越来越难解释。</p><h3 id="_8-4-缺少租户类型和业务参与方建模" tabindex="-1">8.4 缺少租户类型和业务参与方建模 <a class="header-anchor" href="#_8-4-缺少租户类型和业务参与方建模" aria-label="Permalink to &quot;8.4 缺少租户类型和业务参与方建模&quot;">​</a></h3><p>当前 <code>sys_tenant</code> 只有基础字段，无法区分：</p><ul><li>平台运营方</li><li>渠道/接单方，元丰行</li><li>融资性担保公司，智诚等</li><li>银行</li><li>客户企业</li><li>外部合作方</li></ul><p>建议扩展：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>tenant_type:</span></span>
<span class="line"><span>  PLATFORM</span></span>
<span class="line"><span>  CHANNEL_COMPANY</span></span>
<span class="line"><span>  GUARANTEE_COMPANY</span></span>
<span class="line"><span>  BANK</span></span>
<span class="line"><span>  CUSTOMER_COMPANY</span></span>
<span class="line"><span>  SERVICE_PROVIDER</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant_capability:</span></span>
<span class="line"><span>  CAN_RECEIVE_APPLICATION</span></span>
<span class="line"><span>  CAN_ISSUE_COMMERCIAL_GUARANTEE</span></span>
<span class="line"><span>  CAN_REVIEW_FINANCING_GUARANTEE</span></span>
<span class="line"><span>  CAN_SUBMIT_TO_BANK</span></span>
<span class="line"><span>  CAN_PROCESS_BANK_GUARANTEE</span></span></code></pre></div><h3 id="_8-5-缺少业务参与关系和跨租户共享模型" tabindex="-1">8.5 缺少业务参与关系和跨租户共享模型 <a class="header-anchor" href="#_8-5-缺少业务参与关系和跨租户共享模型" aria-label="Permalink to &quot;8.5 缺少业务参与关系和跨租户共享模型&quot;">​</a></h3><p>当前租户隔离主要靠 <code>tenant_id</code>，但保函业务一定存在跨租户协同。</p><p>缺少这些核心表/概念：</p><ul><li><code>guarantee_case</code></li><li><code>case_participant</code></li><li><code>case_task</code></li><li><code>case_document</code></li><li><code>case_document_share</code></li><li><code>case_audit_timeline</code></li><li><code>case_handover</code></li></ul><p>没有这些模型，就会在实现业务时出现两种错误：</p><ol><li>为了协同绕过租户隔离。</li><li>为了隔离导致业务流转不了。</li></ol><h3 id="_8-6-缺少租户级流程配置" tabindex="-1">8.6 缺少租户级流程配置 <a class="header-anchor" href="#_8-6-缺少租户级流程配置" aria-label="Permalink to &quot;8.6 缺少租户级流程配置&quot;">​</a></h3><p>元丰行、智诚、银行的审批流程明显不同：</p><ul><li>元丰行：接单、资料整理、签约、初审、递交下游。</li><li>智诚：尽调、风控、审批、反担保、递交银行。</li><li>银行：资料接收、授信/审查、开函、回传。</li></ul><p>当前 Mango 尚未形成租户级流程定义、流程版本、流程授权、流程实例隔离能力。</p><p>建议后续引入：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>workflow_definition</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  business_type</span></span>
<span class="line"><span>  version</span></span>
<span class="line"><span>  status</span></span>
<span class="line"><span></span></span>
<span class="line"><span>workflow_instance</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  case_id</span></span>
<span class="line"><span>  definition_id</span></span>
<span class="line"><span></span></span>
<span class="line"><span>workflow_task</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  case_id</span></span>
<span class="line"><span>  assignee_member_id</span></span></code></pre></div><h3 id="_8-7-缺少租户开通初始化" tabindex="-1">8.7 缺少租户开通初始化 <a class="header-anchor" href="#_8-7-缺少租户开通初始化" aria-label="Permalink to &quot;8.7 缺少租户开通初始化&quot;">​</a></h3><p>当前创建租户只是插入 <code>sys_tenant</code>，不会完整初始化：</p><ul><li>默认组织根节点。</li><li>默认岗位。</li><li>租户管理员成员。</li><li>默认角色。</li><li>默认角色菜单授权。</li><li>默认流程模板。</li><li>默认业务参数。</li><li>默认材料清单。</li></ul><p>建议创建租户时进入 provisioning 流程，而不是普通 CRUD：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>create tenant</span></span>
<span class="line"><span>  -&gt; create tenant profile</span></span>
<span class="line"><span>  -&gt; create root org</span></span>
<span class="line"><span>  -&gt; create admin member</span></span>
<span class="line"><span>  -&gt; create tenant admin role</span></span>
<span class="line"><span>  -&gt; grant default menus</span></span>
<span class="line"><span>  -&gt; initialize workflow templates</span></span>
<span class="line"><span>  -&gt; initialize business parameters</span></span>
<span class="line"><span>  -&gt; write provisioning audit</span></span></code></pre></div><h3 id="_8-8-租户插件默认租户存在风险" tabindex="-1">8.8 租户插件默认租户存在风险 <a class="header-anchor" href="#_8-8-租户插件默认租户存在风险" aria-label="Permalink to &quot;8.8 租户插件默认租户存在风险&quot;">​</a></h3><p>当前租户插件无上下文时使用默认租户 <code>1</code>。这对开发方便，但生产上有风险：</p><ul><li>某些接口忘记写入上下文时，可能落到平台租户。</li><li>定时任务、异步任务、消息消费如果没有显式租户上下文，可能污染默认租户数据。</li></ul><p>建议：</p><ul><li>Web 请求缺少租户上下文时，对需要租户隔离的接口直接失败。</li><li>定时任务和异步任务必须显式声明 tenant scope。</li><li>平台级任务使用 <code>TenantScope.none()</code> 或 <code>TenantScope.platform()</code>，不能隐式使用默认值。</li></ul><h3 id="_8-9-缺少跨租户审计和支持访问模型" tabindex="-1">8.9 缺少跨租户审计和支持访问模型 <a class="header-anchor" href="#_8-9-缺少跨租户审计和支持访问模型" aria-label="Permalink to &quot;8.9 缺少跨租户审计和支持访问模型&quot;">​</a></h3><p>保函业务涉及金融、担保、银行资料，平台人员跨租户查看资料必须受控。</p><p>建议补：</p><ul><li>平台支持授权。</li><li>跨租户访问原因。</li><li>临时授权有效期。</li><li>敏感资料查看水印。</li><li>下载审计。</li><li>资料脱敏规则。</li></ul><h2 id="_9-投产前一步到位重构方案" tabindex="-1">9. 投产前一步到位重构方案 <a class="header-anchor" href="#_9-投产前一步到位重构方案" aria-label="Permalink to &quot;9. 投产前一步到位重构方案&quot;">​</a></h2><p>当前系统尚未投产，不需要兼容历史租户数据，也不需要为了短期页面可用继续保留模糊模型。建议直接按正式模型重构，不再走“先凑合、后迁移”的路线。</p><h3 id="p0-冻结正式概念边界" tabindex="-1">P0：冻结正式概念边界 <a class="header-anchor" href="#p0-冻结正式概念边界" aria-label="Permalink to &quot;P0：冻结正式概念边界&quot;">​</a></h3><p>立即明确：</p><ul><li><code>identity_user</code> 是全局身份账号。</li><li><code>tenant</code> 是企业级隔离单元。</li><li><code>tenant_member</code> 是账号在租户里的成员身份。</li><li><code>org/post/role/workflow/business_config</code> 属于租户内部。</li><li><code>app/menu/api/base_dict/area</code> 属于平台元数据。</li><li>跨租户业务协作必须通过业务参与关系授权。</li></ul><p>同时统一产品语言：</p><ul><li>后台产品可显示“企业空间”或“机构空间”。</li><li>技术模型仍使用 <code>tenant</code>。</li><li>文档、代码、接口里必须明确：tenant 不是部门、不是角色、不是业务参与方。</li></ul><h3 id="p1-直接建立正式租户成员模型" tabindex="-1">P1：直接建立正式租户成员模型 <a class="header-anchor" href="#p1-直接建立正式租户成员模型" aria-label="Permalink to &quot;P1：直接建立正式租户成员模型&quot;">​</a></h3><p>新增 <code>tenant_member</code>，并以它作为租户内用户管理的主模型：</p><ul><li>登录租户列表只返回账号已加入的租户。</li><li>登录时校验成员状态。</li><li>用户管理页面改为“成员管理”，管理 <code>tenant_member</code>。</li><li>平台侧单独提供“身份账号管理”，管理 <code>identity_user</code>。</li><li>角色绑定主体改为 <code>tenant_member</code>，不再直接绑定 <code>identity_user</code>。</li><li>组织、岗位、上级、在职状态都挂到 <code>tenant_member</code> 或成员组织岗位关系。</li></ul><p>建议表：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>tenant_member</span></span>
<span class="line"><span>  id</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  user_id</span></span>
<span class="line"><span>  member_no</span></span>
<span class="line"><span>  display_name</span></span>
<span class="line"><span>  member_type</span></span>
<span class="line"><span>  status</span></span>
<span class="line"><span>  primary_org_id</span></span>
<span class="line"><span>  primary_post_id</span></span>
<span class="line"><span>  joined_at</span></span>
<span class="line"><span>  left_at</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant_member_org</span></span>
<span class="line"><span>  id</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  member_id</span></span>
<span class="line"><span>  org_id</span></span>
<span class="line"><span>  post_id</span></span>
<span class="line"><span>  primary_flag</span></span></code></pre></div><h3 id="p2-重构登录与授权主链路" tabindex="-1">P2：重构登录与授权主链路 <a class="header-anchor" href="#p2-重构登录与授权主链路" aria-label="Permalink to &quot;P2：重构登录与授权主链路&quot;">​</a></h3><p>登录必须改为：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>输入账号密码</span></span>
<span class="line"><span>  -&gt; 校验 identity_user</span></span>
<span class="line"><span>  -&gt; 查询可进入的 tenant_member 列表</span></span>
<span class="line"><span>  -&gt; 用户选择企业空间</span></span>
<span class="line"><span>  -&gt; 校验 member 状态、租户状态、应用开通状态</span></span>
<span class="line"><span>  -&gt; token 写入 tenantId、memberId、userId、appCode、realm、actorType</span></span>
<span class="line"><span>  -&gt; 权限按 memberId + tenantId 加载</span></span></code></pre></div><p>授权模型同步改造：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>authorization_subject_role.subject_type = TENANT_MEMBER</span></span>
<span class="line"><span>authorization_subject_role.subject_id = tenant_member.id</span></span></code></pre></div><p>保留 <code>identity_user.id</code> 在 token 中用于识别登录账号，但业务权限不要再直接绑定全局账号。</p><h3 id="p3-租户开通改为-provisioning" tabindex="-1">P3：租户开通改为 provisioning <a class="header-anchor" href="#p3-租户开通改为-provisioning" aria-label="Permalink to &quot;P3：租户开通改为 provisioning&quot;">​</a></h3><p>租户管理不能只是 CRUD。创建企业空间时必须一次性初始化：</p><ul><li>企业基础信息和 <code>tenant_type</code>。</li><li>根组织。</li><li>默认岗位。</li><li>租户管理员身份账号和成员。</li><li>租户管理员角色。</li><li>默认菜单授权。</li><li>默认流程模板。</li><li>默认业务参数。</li><li>默认材料清单。</li><li>开通审计记录。</li></ul><p>建议表：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>tenant_profile</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  tenant_type</span></span>
<span class="line"><span>  institution_license_no</span></span>
<span class="line"><span>  contact_info</span></span>
<span class="line"><span>  status</span></span>
<span class="line"><span></span></span>
<span class="line"><span>tenant_capability</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  capability_code</span></span>
<span class="line"><span>  enabled</span></span></code></pre></div><h3 id="p4-一次性设计保函业务协同模型" tabindex="-1">P4：一次性设计保函业务协同模型 <a class="header-anchor" href="#p4-一次性设计保函业务协同模型" aria-label="Permalink to &quot;P4：一次性设计保函业务协同模型&quot;">​</a></h3><p>优先建立：</p><ul><li><code>guarantee_case</code></li><li><code>case_participant</code></li><li><code>case_document</code></li><li><code>case_document_share</code></li><li><code>case_task</code></li></ul><p>用它解决元丰行、智诚、银行之间的资料流转和权限可见范围。</p><p>业务参与方不要复用 RBAC 角色，也不要复用租户本身：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>case_participant</span></span>
<span class="line"><span>  case_id</span></span>
<span class="line"><span>  tenant_id</span></span>
<span class="line"><span>  participant_type</span></span>
<span class="line"><span>  participant_role</span></span>
<span class="line"><span>  access_level</span></span>
<span class="line"><span>  status</span></span></code></pre></div><h3 id="p5-建立租户级流程和配置" tabindex="-1">P5：建立租户级流程和配置 <a class="header-anchor" href="#p5-建立租户级流程和配置" aria-label="Permalink to &quot;P5：建立租户级流程和配置&quot;">​</a></h3><p>按租户维护：</p><ul><li>商保流程。</li><li>银行保函流程。</li><li>担保公司内部审批流程。</li><li>银行资料递交流程。</li><li>材料清单。</li><li>风控规则。</li><li>通知模板。</li></ul><h3 id="p6-补安全审计和跨租户支持" tabindex="-1">P6：补安全审计和跨租户支持 <a class="header-anchor" href="#p6-补安全审计和跨租户支持" aria-label="Permalink to &quot;P6：补安全审计和跨租户支持&quot;">​</a></h3><p>尤其针对金融资料：</p><ul><li>跨租户访问授权。</li><li>敏感附件下载审计。</li><li>数据脱敏。</li><li>操作水印。</li><li>支持人员临时授权。</li><li>租户级日志导出。</li></ul><h2 id="_10-对当前-mango-的直接处理建议" tabindex="-1">10. 对当前 Mango 的直接处理建议 <a class="header-anchor" href="#_10-对当前-mango-的直接处理建议" aria-label="Permalink to &quot;10. 对当前 Mango 的直接处理建议&quot;">​</a></h2><p>因为尚未投产，建议不要保留当前“身份用户直接承担租户用户管理”的模型。当前 T5 完成的身份用户 CRUD 可以作为平台账号管理的基础，但不应继续作为租户内成员管理的最终形态。</p><p>建议直接创建新的系统基础任务：</p><ol><li>新增 <code>tenant_member</code>、<code>tenant_member_org</code> 迁移。</li><li>改造登录：租户选项从“所有启用租户”改为“当前账号可进入的企业空间”。</li><li>改造 token：增加 <code>memberId</code>，保留 <code>userId</code>。</li><li>改造权限：角色绑定主体切到 <code>tenant_member</code>。</li><li>页面拆分： <ul><li>平台运营 / 账号管理：管理全局 <code>identity_user</code>。</li><li>账号权限 / 成员管理：管理当前租户 <code>tenant_member</code>。</li></ul></li><li>改造租户创建为 provisioning。</li><li>新增 <code>case_participant</code>、<code>case_document_share</code> 的设计和迁移，为保函业务做准备。</li><li>清理旧测试数据：不要再使用一个 <code>admin</code> 直接绑定所有租户超级角色的模式，改为平台管理员账号 + 每个企业空间自己的管理员成员。</li></ol><p>最终目标不是“所有表都加 tenant_id”，而是：</p><div class="language-text vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span>平台元数据全局共享</span></span>
<span class="line"><span>租户内部治理数据严格隔离</span></span>
<span class="line"><span>跨租户保函业务通过显式参与关系共享</span></span>
<span class="line"><span>身份账号全局唯一</span></span>
<span class="line"><span>租户成员表达账号在企业内的身份</span></span>
<span class="line"><span>权限绑定到租户成员而不是全局账号</span></span></code></pre></div>`,174)])])}const _=n(p,[["render",l]]);export{u as __pageData,_ as default};
