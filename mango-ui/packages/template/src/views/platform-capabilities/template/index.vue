<template>
  <div class="template-guide">
    <section class="guide-hero">
      <div>
        <p class="eyebrow">平台能力介绍</p>
        <h1>模板服务</h1>
        <p class="summary">
          面向合同、通知、清单、审批附件等文档生成场景，提供模板登记、模板编码绑定、版本发布、变量定义、同步/异步渲染和渲染记录追踪能力。
        </p>
      </div>
      <div class="hero-facts" aria-label="模板服务能力概览">
        <div v-for="item in facts" :key="item.label" class="fact-item">
          <span>{{ item.value }}</span>
          <strong>{{ item.label }}</strong>
        </div>
      </div>
    </section>

    <section class="guide-section">
      <div class="section-heading">
        <h2>能力边界</h2>
        <p>模板服务只负责模板生命周期、变量约束和渲染编排；文件存储、文档转换、签章、压缩合并等能力保持在各自模块内。</p>
      </div>
      <el-row :gutter="16">
        <el-col v-for="item in boundaries" :key="item.title" :xs="24" :md="8">
          <div class="info-panel">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="guide-section">
      <div class="section-heading">
        <h2>支持格式</h2>
        <p>源格式和输出格式分开管理，PDF/OFD 是转换目标，不作为模板源格式。</p>
      </div>
      <el-table :data="formats" border>
        <el-table-column prop="format" label="格式" width="110" />
        <el-table-column prop="role" label="定位" width="150" />
        <el-table-column prop="engine" label="引擎/策略" min-width="180" />
        <el-table-column prop="notes" label="说明" min-width="260" />
      </el-table>
    </section>

    <section class="guide-section">
      <div class="section-heading">
        <h2>支持语法</h2>
        <p>变量定义由人工维护作为渲染约束，模板内容提取只作为录入建议。</p>
      </div>
      <div class="syntax-grid">
        <div v-for="item in syntaxes" :key="item.title" class="syntax-panel">
          <div class="syntax-title">
            <h3>{{ item.title }}</h3>
            <el-tag size="small" :type="item.type">{{ item.badge }}</el-tag>
          </div>
          <p>{{ item.description }}</p>
          <pre><code>{{ item.example }}</code></pre>
        </div>
      </div>
    </section>

    <section class="guide-section">
      <div class="section-heading">
        <h2>使用示例</h2>
        <p>业务侧只需要按模板编码调用当前启用版本，不需要关心分类、版本表和渲染器细节。</p>
      </div>
      <el-tabs model-value="text">
        <el-tab-pane label="TEXT/HTML" name="text">
          <pre><code>{{ textExample }}</code></pre>
        </el-tab-pane>
        <el-tab-pane label="按模板编码渲染" name="render">
          <pre><code>{{ renderExample }}</code></pre>
        </el-tab-pane>
        <el-tab-pane label="DOCX 表格循环" name="docx">
          <pre><code>{{ docxExample }}</code></pre>
        </el-tab-pane>
        <el-tab-pane label="渲染变量 JSON" name="json">
          <pre><code>{{ variablesExample }}</code></pre>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="guide-section">
      <div class="section-heading">
        <h2>如何扩展</h2>
        <p>优先在既有边界内扩展，不把文件处理、格式转换或第三方授权逻辑塞进模板 core。</p>
      </div>
      <el-steps direction="vertical" :active="extensionSteps.length">
        <el-step
          v-for="item in extensionSteps"
          :key="item.title"
          :title="item.title"
          :description="item.description"
        />
      </el-steps>
    </section>
  </div>
</template>

<script setup lang="ts" name="TemplateServiceGuide">
const facts = [
  { label: '源格式', value: '4' },
  { label: '渲染模式', value: '同步/异步' },
  { label: '变量策略', value: '人工为准' },
];

const boundaries = [
  {
    title: '基础模板能力',
    description: '维护模板、分类、模板编码绑定、版本、变量 schema、渲染记录和渲染器选择。',
  },
  {
    title: '文件服务',
    description: '只负责模板源文件和渲染产物的存取，模板模块通过适配器调用。',
  },
  {
    title: '文档工具',
    description: '承接 DOCX/HTML 到 PDF/OFD 等转换能力，模板服务只依赖基础能力编排输出目标。',
  },
];

const formats = [
  {
    format: 'TEXT',
    role: '源格式/输出格式',
    engine: 'FreeMarker',
    notes: '适合短信、站内信、纯文本通知、简单拼接内容。',
  },
  {
    format: 'HTML',
    role: '源格式/输出格式',
    engine: 'FreeMarker',
    notes: '适合邮件、富文本通知、可转换 PDF 的 HTML 文档。',
  },
  {
    format: 'DOCX',
    role: '源格式/输出格式',
    engine: 'poi-tl',
    notes: '适合合同、函件、正式文档；支持表格行循环。',
  },
  {
    format: 'XLSX',
    role: '源格式/输出格式',
    engine: 'OOXML 占位符替换',
    notes: '当前支持基础变量替换，复杂表格模板后续接专用 Excel 引擎。',
  },
  {
    format: 'PDF/OFD',
    role: '输出格式',
    engine: 'mango-infra-tools-doc',
    notes: '作为转换目标接入，不作为模板源文件格式登记。',
  },
];

const syntaxes = [
  {
    title: 'FreeMarker',
    badge: 'TEXT/HTML',
    type: 'success' as const,
    description: '用于文本和 HTML 模板，支持插值、条件、列表等常见模板能力。',
    example: '客户：${customer.name}\n<#list items as item>${item.name}: ${item.count}</#list>',
  },
  {
    title: 'poi-tl',
    badge: 'DOCX',
    type: 'warning' as const,
    description: '普通变量使用双花括号；数组变量绑定表格行，行内子变量使用方括号。',
    example: '{{customer.name}}\n表格行：{{items}} [name] [count]',
  },
  {
    title: 'OOXML 占位符',
    badge: 'XLSX',
    type: 'info' as const,
    description: '用于 Excel 基础替换，变量路径使用双花括号。',
    example: '{{customer.name}}\n{{amount}}',
  },
];

const textExample = `合同编号：\${contractNo}
客户名称：\${customer.name}
<#list items as item>
- \${item.name}：\${item.count}
</#list>`;

const renderExample = `POST /api/template/templates/render
{
  "templateCode": "CONTRACT_NOTICE",
  "outputFormat": "TEXT",
  "variables": {
    "customer": { "name": "上海示例科技有限公司" },
    "contractNo": "HT-2026-001"
  }
}`;

const docxExample = `普通段落：
客户：{{customer.name}}

表格模板行：
{{items}} [name] [count]

说明：
items 是数组变量；name/count 是数组元素字段。`;

const variablesExample = `{
  "customer": {
    "name": "上海示例科技有限公司"
  },
  "contractNo": "HT-2026-001",
  "items": [
    { "name": "身份证", "count": 1 },
    { "name": "营业执照", "count": 2 }
  ]
}`;

const extensionSteps = [
  {
    title: '新增源格式渲染器',
    description: '在 template-core 实现 TemplateRenderer，声明 supports/sourceFormat 和 supportsOutput。',
  },
  {
    title: '扩展变量提取',
    description: '在对应渲染器内实现 extractVariables，返回建议变量；最终变量仍以人工定义为准。',
  },
  {
    title: '接入输出转换',
    description: '将 PDF/OFD 等转换放在 mango-infra-tools-doc，由模板服务按输出格式编排调用。',
  },
  {
    title: '注册自动装配',
    description: '在 starter 中注册 renderer、file store adapter 或转换服务，不让业务应用感知底层实现。',
  },
];
</script>

<style scoped lang="scss">
.template-guide {
  padding: 18px;
  color: var(--el-text-color-primary);
}

.guide-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: stretch;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  font-size: 28px;
  font-weight: 700;
}

.summary {
  max-width: 760px;
  margin-top: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.hero-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.fact-item {
  display: flex;
  min-height: 110px;
  flex-direction: column;
  justify-content: center;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.fact-item span {
  color: var(--el-color-primary);
  font-size: 26px;
  font-weight: 700;
}

.fact-item strong {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.guide-section {
  margin-top: 18px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.section-heading {
  margin-bottom: 16px;
}

.section-heading h2 {
  font-size: 18px;
  font-weight: 700;
}

.section-heading p {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}

.info-panel,
.syntax-panel {
  height: 100%;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.info-panel h3,
.syntax-panel h3 {
  font-size: 15px;
  font-weight: 700;
}

.info-panel p,
.syntax-panel p {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  line-height: 1.7;
}

.syntax-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.syntax-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

pre {
  overflow: auto;
  margin: 14px 0 0;
  padding: 14px;
  border-radius: 6px;
  background: #1f2937;
  color: #e5e7eb;
  font-size: 13px;
  line-height: 1.7;
}

code {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
}

:deep(.el-step__description) {
  max-width: 760px;
}

@media (max-width: 1200px) {
  .guide-hero,
  .syntax-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .template-guide {
    padding: 12px;
  }

  .guide-hero,
  .guide-section {
    padding: 16px;
  }

  .hero-facts {
    grid-template-columns: 1fr;
  }
}
</style>
