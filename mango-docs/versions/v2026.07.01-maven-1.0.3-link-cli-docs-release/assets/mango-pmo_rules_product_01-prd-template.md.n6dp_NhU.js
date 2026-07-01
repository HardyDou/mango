import{_ as i,o as a,c as n,a2 as l}from"./chunks/framework.CE5oItKu.js";const r=JSON.parse('{"title":"PRD 与原型需求规范","description":"","frontmatter":{},"headers":[],"relativePath":"mango-pmo/rules/product/01-prd-template.md","filePath":"mango-pmo/rules/product/01-prd-template.md"}'),p={name:"mango-pmo/rules/product/01-prd-template.md"};function h(k,s,t,e,E,d){return a(),n("div",null,[...s[0]||(s[0]=[l(`<h1 id="prd-与原型需求规范" tabindex="-1">PRD 与原型需求规范 <a class="header-anchor" href="#prd-与原型需求规范" aria-label="Permalink to &quot;PRD 与原型需求规范&quot;">​</a></h1><h2 id="_1-定位" tabindex="-1">1. 定位 <a class="header-anchor" href="#_1-定位" aria-label="Permalink to &quot;1. 定位&quot;">​</a></h2><ul><li>本规范适用于 Mango 框架、平台能力、模板、CLI、前端包等 Mango 开发任务。</li><li>本规范适用于基于 Mango 的企业业务项目、业务模块和业务后台开发任务。</li><li>PRD 首先是 <code>For AI</code>：让 AI 可理解、可开发、可验收。</li><li>PRD 其次是 <code>For Human</code>：帮助人把需求写成 AI 能理解的无歧义输入，降低 AI 落地偏差。</li><li>PRD 是业务需求和页面原型的来源，不是详细设计文档。</li><li>PRD 必须说明用户看到什么、能做什么、按什么业务规则判断成功或失败。</li><li>PRD 不写接口、权限、数据库、模块、路由、组件实现、测试命令和交付台账。</li><li>复杂业务必须先写清关键对象、关键业务流程和业务规则，再写菜单和页面。</li><li>PRD 必须先完成业务展开，再写菜单页面；默认顺序为：目标用户、关键对象、对象生命周期、关键业务流程、业务规则、菜单规划、页面原型、验收标准。</li><li>PRD 的质量标准不是“人读起来顺”，而是 AI 能否据此判断范围、识别缺口、生成详细设计、开发功能并完成验收。</li></ul><h2 id="_2-ai-编写前置规则" tabindex="-1">2. AI 编写前置规则 <a class="header-anchor" href="#_2-ai-编写前置规则" aria-label="Permalink to &quot;2. AI 编写前置规则&quot;">​</a></h2><ul><li>信息不足时，必须先输出“待确认问题”，不得直接生成完整 PRD。</li><li>待确认问题必须按目标用户、关键对象、对象生命周期、关键业务流程、业务规则、菜单页面、验收标准、不处理范围排序。</li><li>待确认问题必须标明 <code>AI动作</code>：<code>STOP</code>、<code>ASK</code>、<code>WRITE</code> 或 <code>NEXT</code>。</li><li><code>STOP</code>：目标用户、关键对象、生命周期状态、关键流程、业务规则、页面入口或用户可见验收断言缺失；PRD 出现详细设计内容；追踪矩阵无法闭环。</li><li><code>ASK</code>：缺少业务判断口径、状态边界、用户类型、异常反馈、验收断言或不处理范围，但可以先输出问题等待确认。</li><li><code>WRITE</code>：不存在阻断项，且所有假设都有来源，可以生成或补全 PRD 正文。</li><li><code>NEXT</code>：PRD 自检全部通过，进入详细设计判定为 <code>READY</code>。</li><li>PRD 输出顺序固定为：编写前检查；如存在阻断项则只输出待确认问题；无阻断项再输出正文；最后输出自检和进入详细设计判定。</li><li>用户未确认的问题只能写入“风险与限制”，不得伪装成已确认需求。</li><li>AI 不得自行发明业务规则、状态、菜单、字段、业务角色或验收标准；只能基于用户输入、既有系统事实或明确标注的假设。</li><li>PRD 中出现接口地址、请求方式、权限码、数据库表字段、路由、component key、模块归属、migration、初始化方式或测试命令时，必须退回改写为用户可见需求，不能进入详细设计评审。</li><li>生成 PRD 时使用 <code>mango-pmo/templates/prd.md</code>；本规范中的“章节说明、编写要求、关联规范”是写作约束，不复制到最终 PRD 正文。</li></ul><h2 id="_3-编号与追踪规则" tabindex="-1">3. 编号与追踪规则 <a class="header-anchor" href="#_3-编号与追踪规则" aria-label="Permalink to &quot;3. 编号与追踪规则&quot;">​</a></h2><ul><li>每个关键对象必须分配稳定编号 <code>BO-xxx</code>。</li><li>每个关键业务流程必须分配稳定编号 <code>BF-xxx</code>。</li><li>每条业务规则必须分配稳定编号 <code>BR-xxx</code>。</li><li>每个页面必须分配稳定编号 <code>PG-xxx</code>。</li><li>每个验收项必须分配稳定编号 <code>AC-xxx</code>。</li><li>验收标准的来源必须引用 <code>BO/BF/BR/PG</code> 编号，禁止只写自然语言来源。</li><li>业务规则必须至少被一个流程、页面或验收项引用；未被引用的规则不得保留在 PRD 中。</li></ul><h2 id="_4-必填项" tabindex="-1">4. 必填项 <a class="header-anchor" href="#_4-必填项" aria-label="Permalink to &quot;4. 必填项&quot;">​</a></h2><p>必须写清：</p><ul><li>需求范围</li><li>不处理范围</li><li>业务术语</li><li>业务用户类型</li><li>关键对象</li><li>对象生命周期与状态口径</li><li>关键业务流程</li><li>业务规则清单</li><li>菜单规划</li><li>页面总览</li><li>菜单级页面原型</li><li>字典与展示项</li><li>追踪矩阵</li><li>验收标准</li><li>风险与限制</li></ul><h2 id="_5-推荐结构" tabindex="-1">5. 推荐结构 <a class="header-anchor" href="#_5-推荐结构" aria-label="Permalink to &quot;5. 推荐结构&quot;">​</a></h2><div class="language-md vp-adaptive-theme"><button title="Copy Code" class="copy"></button><span class="lang">md</span><pre class="shiki shiki-themes github-light github-dark vp-code" tabindex="0"><code><span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;"># &amp;lt;需求名称&amp;gt; PRD</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 1. 需求范围</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">说明本次需求覆盖的业务能力、目标用户和交付边界。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清本次包含哪些业务能力、菜单、页面和主要流程。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清本次不改变哪些既有能力。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 范围必须能被验收，不能只写“优化”“完善”“支持”。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写实现方式。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> Sprint 规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 文档资产归档边界</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 2. 不处理范围</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">明确本次不覆盖的业务能力、页面、流程和场景。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清暂不支持的场景。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清暂不设计的菜单、页面、按钮和流程。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 未来可能扩展但本次不做的内容必须列入本章。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不用“后续优化”代替明确边界。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> Sprint 规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 3. 业务术语</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">解释页面、流程、按钮和字段中出现的业务名词。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 只解释业务含义。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 同一概念只能保留一个主名称。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 有别名时必须标明页面展示名称。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写技术名词。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 4. 关键对象</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">识别本次需求涉及的核心业务对象。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个关键对象必须写清业务含义、用户可见信息、主要动作和生命周期概览。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 关键对象必须使用业务用户能理解的名称。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写表结构、字段类型、接口、权限和技术归属。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 不写数据库、接口和字段类型，但必须写清业务口径，包括唯一性、归属、有效期、金额/数量边界、状态判断、重复判断和统计口径。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 对象ID | 对象 | 业务含义 | 用户可见信息 | 主要动作 | 生命周期概览 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;BO-ID&amp;gt; |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 对象ID | 唯一识别口径 | 归属口径 | 数量/金额/有效期边界 | 重复判断口径 | 统计口径 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;BO-ID&amp;gt; |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 4.1 对象生命周期与状态口径</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">描述关键对象从创建到终止的业务阶段，以及用户可见状态和动作边界。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个有生命周期的关键对象必须写清状态含义、可执行动作、不可执行动作和用户可见原因。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 只写业务状态和用户可见口径，不写状态字段和技术枚举。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 终态、可逆状态、不可逆状态必须明确。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 不写权限码、角色权限实现和授权模型，但必须写清业务用户类型、可见内容、可执行动作、不可执行动作和用户可见原因。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 对象ID | 状态 | 业务含义 | 进入条件 | 退出条件 | 可执行动作 | 不可执行动作 | 是否可逆 | 触发流程ID | 是否终态 | 终态说明 | 引用ID |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;BO-ID&amp;gt; |  |  |  |  |  |  | 是/否 | &amp;lt;BF-ID&amp;gt; | 是/否 |  | &amp;lt;BR/PG/AC-ID&amp;gt; |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 5. 关键业务流程</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">描述用户完成关键业务目标时的完整业务流程。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个流程必须写清流程目标、适用场景、前置条件、操作步骤、业务规则、状态变化、异常限制和验收标准。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 操作步骤从用户视角描述。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 业务规则必须能判断允许/禁止、成功/失败、状态变化和用户反馈。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每条业务规则必须写清适用的业务用户类型。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写系统处理步骤和技术实现。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 5.x 流程：&amp;lt;流程名称&amp;gt;</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 流程ID</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 流程目标</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 适用场景</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 前置条件</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 操作步骤</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 业务规则清单</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">##### 规则 BR-xxx：&amp;lt;规则名称&amp;gt;</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 定义：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 适用业务用户类型：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 触发条件：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 判断口径：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 处理方式：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 用户反馈：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 状态变化：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 影响范围：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 例外情况：</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 验收标准：</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 状态变化</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 操作前 | 操作 | 操作后 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 异常与限制</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 验收标准</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 6. 菜单规划</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">描述菜单层级、名称、顺序和菜单类型。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清一级、二级、三级菜单。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 标明菜单是目录还是页面。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个页面菜单必须对应后续页面章节。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 菜单名称必须符合业务用户认知。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 菜单顺序应符合用户工作流程。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 只定义业务菜单名称、层级、页面用途、按钮可见/可用的业务条件；详细设计负责菜单资源、权限码、默认授权、租户/数据权限、路由和页面 key。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 页面ID | 菜单层级 | 菜单名称 | 菜单类型 | 展示顺序 | 对应页面 | 说明 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;PG-ID&amp;gt; |  |  | 目录/页面 |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 7. 页面总览</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">说明每个菜单对应的页面和页面用途。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个菜单页面必须登记。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 页面用途一句话说清。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 标明页面类型：列表页、详情页、配置页、工作台、表单页、站点页等。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写页面实现方式。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端列表页规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端表单页规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端弹框与抽屉规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 页面ID | 菜单 | 页面名称 | 页面类型 | 页面用途 | 关联关键对象ID | 关联关键流程ID |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;PG-ID&amp;gt; |  |  |  |  | &amp;lt;BO-ID&amp;gt; | &amp;lt;BF-ID&amp;gt; |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 8. 菜单级页面原型</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">按菜单页面逐章描述页面布局、搜索、按钮、列表、表单、详情、交互和反馈。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 章节必须按菜单层级排列。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个页面必须写清页面范围与特性、用户场景、页面布局、搜索区、页面功能按钮、列表内容、行功能按钮、表单内容、详情内容、交互流程、状态与反馈、验收标准。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不能只写“标准列表页”“标准表单”。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写接口、权限码、路由、组件实现和数据库字段。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 控件样式只描述用户交互形态，不指定组件库、路由和组件实现。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个页面必须覆盖正常态、空态、加载态、失败态、无权限/不可操作态；涉及表单时必须覆盖校验失败、取消、关闭、未保存离开；涉及危险操作时必须覆盖确认、成功、失败和不可逆提示。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端列表页规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端表单页规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端弹框与抽屉规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 前端 Admin UI 通用规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 8.x PG-xxx &amp;lt;菜单名称 / 页面名称&amp;gt;</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 页面范围与特性</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">说明页面解决什么业务问题，用户在这里能完成哪些事情，不能完成哪些事情。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 用户场景</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">描述用户进入页面的场景、操作目标和期望结果。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 页面布局</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">描述搜索区、按钮区、列表区、分页区、行操作区、弹窗、抽屉和详情区的显示顺序。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 搜索区</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 字段 | 业务含义 | 控件样式 | 业务来源 | 选项来源 | 联动字段 | 默认值 | 过滤口径 | 是否必填 | 说明 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  | 用户输入/业务字典/关键对象 |  |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 页面功能按钮</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 按钮 | 位置 | 显示条件 | 点击后交互 | 成功反馈 | 失败反馈 | 关联规则ID | 关联验收ID |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  |  |  |  | &amp;lt;BR-ID&amp;gt; | &amp;lt;AC-ID&amp;gt; |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 列表内容</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 列名 | 业务含义 | 展示样式 | 业务来源 | 默认排序 | 空值业务含义 | 空值展示 | 过长处理 | 说明 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  |  |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 行功能按钮</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 按钮 | 显示条件 | 可点击条件 | 点击后交互 | 成功反馈 | 失败反馈 | 关联规则ID | 关联验收ID |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  |  |  |  | &amp;lt;BR-ID&amp;gt; | &amp;lt;AC-ID&amp;gt; |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 表单内容</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 字段 | 业务含义 | 控件样式 | 业务来源 | 选项来源 | 联动字段 | 是否必填 | 默认值 | 输入限制 | 提示文案 | 创建可填 | 编辑可改 | 详情展示 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  | 用户输入/业务字典/关键对象 |  |  |  |  |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 详情内容</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">说明详情展示的信息分组、展示顺序、状态、记录和历史信息。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 交互流程</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">按步骤写清点击、填写、确认、取消、返回、成功、失败、关闭弹窗、未保存离开等交互。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 状态与反馈</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 状态类型 | 场景 | 展示文案 | 展示样式 | 可见按钮 | 可点击按钮 | 禁用原因 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 正常/空/加载/失败/不可操作 |  |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">#### 验收引用</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">页面章节只引用全局验收项，不重复定义验收标准；全局 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`## 验收标准\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 是唯一验收来源。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 验收ID | 覆盖页面能力 | 操作路径 | 用户可见断言摘要 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;AC-ID&amp;gt; |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 9. 字典与展示项</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">登记页面中需要统一展示的业务选项、状态、类型和文案。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清字典项业务含义和页面展示文案。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 写清选项之间是否互斥、是否可多选、是否有默认项。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写编码、存储、初始化和技术来源。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 名称 | 业务含义 | 展示项 | 默认项 | 适用页面 | 说明 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  |  |  |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 10. 追踪矩阵</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">检查关键对象、关键流程、业务规则、页面和验收项是否形成闭环。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个关键对象、关键流程、业务规则和页面必须至少被一个验收项覆盖。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 未覆盖项必须移除、补充验收，或写入风险与限制等待确认。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 禁止用一个“主流程通过”覆盖多个独立能力。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 来源ID | 来源类型 | 覆盖页面ID | 覆盖流程ID | 覆盖规则ID | 验收项ID |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;BO/BF/BR/PG-ID&amp;gt; | 关键对象/关键流程/业务规则/页面 | &amp;lt;PG-ID&amp;gt; | &amp;lt;BF-ID&amp;gt; | &amp;lt;BR-ID&amp;gt; | &amp;lt;AC-ID&amp;gt; |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 11. 验收标准</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">从用户视角说明本次需求什么情况下可验收。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 每个关键对象、关键流程、菜单页面和主要按钮必须有验收项。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 验收标准只写用户可见结果。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写测试命令、脚本、接口断言和技术验证过程。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不允许只写“功能正常”。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 成功、失败、禁止、例外和边界都必须有用户可见断言。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> AI 交付质量门禁</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| ID | 来源 | 验收项 | 用户操作 | 用户可见结果 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| &amp;lt;AC-ID&amp;gt; | &amp;lt;BO/BF/BR/PG-ID&amp;gt; |  |  |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 12. 风险与限制</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">说明需求仍存在的不确定业务问题、规则缺口和验收限制。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 只写业务风险和需求限制。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不写技术风险、实现风险和发布风险。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 不确定项必须写清等待谁确认。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> Sprint 规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 风险/缺口 | 类型 | 影响范围 | 当前处理 | 待确认人 | 是否阻断 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|  | 需求缺口/业务风险/验收限制 |  |  |  | 是/否 |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 13. 进入详细设计判定</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">判断当前 PRD 是否可以进入详细设计。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 存在阻断缺口时，结论必须为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`BLOCKED\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 出现 PRD 禁止技术内容时，结论必须为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`BLOCKED\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">，并回退改写。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 所有 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`BO/BF/BR/PG/AC\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 覆盖闭环后，结论才能为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`READY\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 仅在用户明确确认例外时，结论可写 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`EXCEPTION\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">，并说明确认来源。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 检查项 | 结果 | 证据 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 阻断问题数量 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 未覆盖 BO/BF/BR/PG/AC 数量 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 是否出现 PRD 禁止技术内容 | 是/否 |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 结论 | READY/BLOCKED/EXCEPTION |  |</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">## 14. AI 输出自检</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 章节说明</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">PRD 完成后必须逐项自检，防止 AI 按章节填空但漏掉跨章节一致性。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 编写要求</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 自检结果只能填写 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`PASS\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">、</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`FAIL\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">、</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`BLOCKED\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 或 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`EXCEPTION\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 任一结果为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`FAIL\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 或 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`BLOCKED\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 时，不得声明 PRD 完成；最终动作只能是 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`STOP\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 或 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`ASK\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> \`EXCEPTION\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 必须引用用户确认来源。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 全部为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`PASS\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 且进入详细设计判定为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`READY\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 时，最终动作才可为 </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">\`NEXT\`</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">。</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 自检发现缺口时，必须补充正文或写入风险与限制。</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#005CC5;--shiki-light-font-weight:bold;--shiki-dark:#79B8FF;--shiki-dark-font-weight:bold;">### 关联规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> PRD 与原型需求规范</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">-</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> 详细设计模板规范</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 检查项 | 结果 | 说明 |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">|---|---|---|</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 每个关键对象是否至少关联一个流程、页面或验收项 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 每个关键流程是否包含规则、状态变化、异常限制和验收标准 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 每条业务规则是否至少被一个流程、页面或验收项引用 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 每个页面按钮是否能追溯到关键流程或业务规则 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 每个验收项是否有明确用户操作和可见结果 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| 是否覆盖正常态、空态、加载态、失败态、不可操作态 |  |  |</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">| PRD 中是否没有接口、权限码、数据库、路由、组件实现和测试命令 |  |  |</span></span></code></pre></div><h2 id="_6-业务规则要求" tabindex="-1">6. 业务规则要求 <a class="header-anchor" href="#_6-业务规则要求" aria-label="Permalink to &quot;6. 业务规则要求&quot;">​</a></h2><ul><li>每个关键业务流程必须包含业务规则清单。</li><li>每条业务规则必须写清定义、触发条件、判断口径、处理方式、用户反馈、状态变化、影响范围、例外情况和验收标准。</li><li>重复、拒绝、撤回、作废、启用、停用、发布、下架、冻结、释放、到期、超时、余额不足、金额上限、金额下限等规则不得只写规则名称。</li><li>业务规则只写业务判断，不写技术实现。</li></ul><h2 id="_7-prd-与详细设计边界" tabindex="-1">7. PRD 与详细设计边界 <a class="header-anchor" href="#_7-prd-与详细设计边界" aria-label="Permalink to &quot;7. PRD 与详细设计边界&quot;">​</a></h2><p>PRD 必须写：</p><ul><li>关键对象的业务含义。</li><li>关键业务流程的用户操作和业务规则。</li><li>用户可见状态、动作和反馈。</li><li>菜单、页面、搜索、按钮、列表、表单、详情和验收标准。</li><li>业务用户类型、可见内容、可执行动作、不可执行动作和用户可见原因。</li><li>唯一性、归属、有效期、金额/数量边界、状态判断、重复判断和统计口径等业务口径。</li></ul><p>PRD 禁止写：</p><ul><li>接口地址、请求方式、入参、出参。</li><li>权限码、角色权限实现。</li><li>数据库表、字段类型、索引。</li><li>后端模块、前端包、路由、component key。</li><li>技术架构、服务边界、实现方案。</li><li>测试命令、E2E 脚本细节。</li><li>交付台账、设计决策和开发任务拆分。</li></ul><h2 id="_8-禁止事项" tabindex="-1">8. 禁止事项 <a class="header-anchor" href="#_8-禁止事项" aria-label="Permalink to &quot;8. 禁止事项&quot;">​</a></h2><ul><li>只有目标，没有范围。</li><li>只有页面，没有关键对象。</li><li>只有操作步骤，没有业务规则。</li><li>只有功能描述，没有验收标准。</li><li>只有自然语言来源，没有 <code>BO/BF/BR/PG/AC</code> 追踪编号。</li><li>把设计文档写成 PRD。</li><li>把规范写进 <code>mango-docs</code>。</li><li>在 PRD 中复制长期规则正文；只能链接 <code>mango-pmo</code> 规则源。</li></ul>`,21)])])}const c=i(p,[["render",h]]);export{r as __pageData,c as default};
