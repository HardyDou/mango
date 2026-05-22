<template>
  <DemoDocLayout
    class="workflow-components-view"
    title="工作流组件"
    subtitle="工作流前端能力的统一使用入口，覆盖运行时表单、业务申请注册、业务审批注册和流程轨迹展示。"
    content-box
    :toc-items="tocItems"
  >
    <section id="runtime-form" class="doc-section">
      <h2>运行时表单渲染</h2>
      <p>RuntimeFormRenderer 接收解析后的字段配置、变量对象和字段权限，用于发起流程、审批详情和历史详情里的动态表单展示。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="workflow-demo-toolbar">
            <el-switch v-model="runtimeReadonly" active-text="只读" inactive-text="可编辑" />
            <el-tag type="info" effect="plain">字段权限优先于全局只读</el-tag>
          </div>
          <RuntimeFormRenderer
            :fields="runtimeFields"
            :model="runtimeModel"
            :readonly="runtimeReadonly"
            :permissions="runtimePermissions"
            label-width="108px"
          />
          <div class="result-line">
            <span>当前变量</span>
            <el-tag type="success" effect="plain">{{ runtimeModel.title || '未填写' }}</el-tag>
            <el-tag type="warning" effect="plain">金额 {{ runtimeModel.amount }}</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('runtime')">
          <el-icon><component :is="codeVisible.runtime ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.runtime ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.runtime" :code="runtimeCode" />
      </div>
    </section>

    <section id="business-apply" class="doc-section">
      <h2>业务申请组件注册</h2>
      <p>自定义申请页通过 applyPageKey 注册，流程定义或模板发布后按 renderConfig 进入对应业务申请组件。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="workflow-register-flow">
            <div class="register-step">
              <strong>1. 注册组件</strong>
              <span>在模块初始化时注册 applyPageKey。</span>
            </div>
            <div class="register-step">
              <strong>2. 绑定定义</strong>
              <span>流程定义配置 BUSINESS_FORM / CUSTOM_APPLY 等渲染信息。</span>
            </div>
            <div class="register-step">
              <strong>3. 发起流程</strong>
              <span>业务组件收集 businessKey、variables 后调用发起接口。</span>
            </div>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('apply')">
          <el-icon><component :is="codeVisible.apply ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.apply ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.apply" :code="applyCode" />
      </div>
    </section>

    <section id="business-approval" class="doc-section">
      <h2>业务审批组件注册</h2>
      <p>自定义审批详情通过 businessType 注册，可接管审批意见字段、右侧记录面板和动作前校验，适合合同、费用等业务模板。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-table :data="approvalModeTable" border>
            <el-table-column prop="name" label="配置项" width="180" />
            <el-table-column prop="description" label="含义" min-width="260" />
            <el-table-column prop="scene" label="典型场景" min-width="240" />
          </el-table>
        </div>
        <div class="op-btns" @click="toggleCode('approval')">
          <el-icon><component :is="codeVisible.approval ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.approval ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.approval" :code="approvalCode" />
      </div>
    </section>

    <section id="trace" class="doc-section">
      <h2>流程轨迹组件</h2>
      <p>流程轨迹组件共用流程定义节点、当前节点、已访问节点和审批记录，可分别渲染流程图、节点时间线和审批记录。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-tabs v-model="traceTab">
            <el-tab-pane label="流程图" name="tree">
              <div class="workflow-flow-preview">
                <WorkflowProgressTree
                  :node="workflowNode"
                  current-node-key="finance_approve"
                  :visited-node-keys="['root', 'dept_approve']"
                  status="RUNNING"
                />
              </div>
            </el-tab-pane>
            <el-tab-pane label="节点时间线" name="node">
              <WorkflowNodeTimeline
                :node="workflowNode"
                current-node-key="finance_approve"
                :visited-node-keys="['root', 'dept_approve']"
                status="RUNNING"
                :records="approvalRecords"
              />
            </el-tab-pane>
            <el-tab-pane label="审批记录" name="record">
              <WorkflowApprovalTimeline :records="approvalRecords" />
            </el-tab-pane>
          </el-tabs>
        </div>
        <div class="op-btns" @click="toggleCode('trace')">
          <el-icon><component :is="codeVisible.trace ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.trace ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.trace" :code="traceCode" />
      </div>
    </section>

    <section id="component-list" class="doc-section api-section">
      <h2>组件清单</h2>
      <el-table :data="componentTable" border>
        <el-table-column prop="component" label="组件 / API" width="230" />
        <el-table-column prop="importPath" label="导入方式" min-width="260" />
        <el-table-column prop="scene" label="使用场景" min-width="260" />
        <el-table-column prop="notes" label="关键说明" min-width="260" />
      </el-table>
    </section>

    <section id="field-support" class="doc-section api-section">
      <h2>运行时字段支持</h2>
      <el-table :data="fieldSupportTable" border>
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column prop="fields" label="字段类型" min-width="280" />
        <el-table-column prop="notes" label="说明" min-width="300" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="WorkflowComponentsView">
import { reactive, ref } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import {
  RuntimeFormRenderer,
  WorkflowApprovalTimeline,
  WorkflowNodeTimeline,
  WorkflowProgressTree,
  type RuntimeFormField,
  type WorkflowDesignerNode,
  type WorkflowTaskRecord,
} from '@mango/workflow';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'runtime-form', label: '运行时表单渲染' },
  { id: 'business-apply', label: '业务申请组件注册' },
  { id: 'business-approval', label: '业务审批组件注册' },
  { id: 'trace', label: '流程轨迹组件' },
  { id: 'component-list', label: '组件清单' },
  { id: 'field-support', label: '运行时字段支持' },
];

const codeVisible = ref<Record<string, boolean>>({
  runtime: false,
  apply: false,
  approval: false,
  trace: false,
});
const runtimeReadonly = ref(false);
const traceTab = ref('tree');

const runtimeFields: RuntimeFormField[] = [
  {
    key: 'title',
    label: '申请标题',
    type: 'input',
    placeholder: '请输入申请标题',
    rules: [{ required: true, message: '申请标题不能为空', trigger: 'blur' }],
  },
  {
    key: 'amount',
    label: '申请金额',
    type: 'number',
    min: 0,
    step: 100,
    rules: [{ required: true, message: '申请金额不能为空', trigger: 'change' }],
  },
  {
    key: 'applicant',
    label: '申请人',
    type: 'systemUser',
    options: [
      { label: '张三', value: '1001' },
      { label: '李四', value: '1002' },
    ],
  },
  {
    key: 'department',
    label: '所属部门',
    type: 'systemDept',
    treeOptions: [
      {
        label: '产品研发部',
        value: 'dept-rd',
        children: [
          { label: '平台组', value: 'dept-platform' },
          { label: '业务组', value: 'dept-business' },
        ],
      },
    ],
  },
  {
    key: 'expenseType',
    label: '费用类型',
    type: 'select',
    options: [
      { label: '差旅', value: 'travel' },
      { label: '采购', value: 'purchase' },
      { label: '合同', value: 'contract' },
    ],
  },
  {
    key: 'description',
    label: '申请说明',
    type: 'textarea',
    placeholder: '请输入申请说明',
  },
];

const runtimeModel = reactive<Record<string, any>>({
  title: '合同用印审批',
  amount: 12000,
  applicant: '1001',
  department: 'dept-platform',
  expenseType: 'contract',
  description: '合同归档前需要完成用印审批。',
});
const runtimePermissions = {
  applicant: 'READONLY',
};

const workflowNode: WorkflowDesignerNode = {
  id: 'root',
  nodeName: '发起人',
  nodeType: 'ROOT',
  childNode: {
    id: 'dept_approve',
    nodeName: '部门负责人审批',
    nodeType: 'APPROVAL',
    executionType: 'USER_TASK',
    childNode: {
      id: 'finance_approve',
      nodeName: '财务会签',
      nodeType: 'APPROVAL',
      executionType: 'USER_TASK',
      properties: {
        approvalMode: 'COUNTERSIGN',
        passRatio: 0.66,
      },
      childNode: {
        id: 'seal_cc',
        nodeName: '用印抄送',
        nodeType: 'CC',
      },
    },
  },
};

const approvalRecords: WorkflowTaskRecord[] = [
  {
    id: 'record-1',
    processInstanceId: 'process-demo-1',
    taskId: 'task-1',
    taskName: '部门负责人审批',
    taskDefinitionKey: 'dept_approve',
    action: 'complete',
    actionName: '通过',
    operatorId: '1002',
    operatorName: '李四',
    comment: '合同内容和金额已确认。',
    variables: { deptOpinion: '同意' },
    createdTime: '2026-05-22 09:30:00',
  },
];

const approvalModeTable = [
  { name: 'commentMode', description: '审批意见来源。ACTION_BAR 使用右侧动作栏意见；BUSINESS_FORM 由业务表单自己收集；NONE 不提交意见。', scene: '合同用印表单内维护意见字段' },
  { name: 'recordPanelMode', description: '审批记录面板模式。DEFAULT 使用通用右侧记录；HIDDEN 隐藏；CUSTOM 使用业务自定义记录面板。', scene: '特殊业务在表单中渲染审批历史' },
  { name: 'collectVariables', description: '动作提交前从业务组件收集流程变量，随通过、驳回、暂存等动作一起提交。', scene: '提交合同编号、用印次数、业务审批字段' },
  { name: 'validateBeforeAction', description: '动作提交前执行业务校验，失败时阻断动作。', scene: 'BUSINESS_FORM 模式下校验审批意见必填' },
  { name: 'getActionOverrides', description: '按业务上下文覆盖按钮可见性、禁用状态、文案和提示。', scene: '部分节点隐藏暂存或限制转办' },
];

const componentTable = [
  { component: 'RuntimeFormRenderer', importPath: '@mango/workflow', scene: '动态表单、自定义表单兜底渲染、审批详情只读/可编辑字段', notes: '字段权限支持 HIDDEN、READONLY、EDITABLE' },
  { component: 'registerBusinessApplyComponents', importPath: '@mango/workflow', scene: '注册业务申请页', notes: 'key 需要与流程 renderConfig.applyPageKey 保持一致' },
  { component: 'registerBusinessApprovalComponents', importPath: '@mango/workflow', scene: '注册业务审批详情页', notes: '支持业务意见、业务变量、动作校验和记录面板模式' },
  { component: 'WorkflowProgressTree', importPath: '@mango/workflow', scene: '流程图形轨迹', notes: '传入设计节点、当前节点、已访问节点和流程状态' },
  { component: 'WorkflowNodeTimeline', importPath: '@mango/workflow', scene: '节点维度流转时间线', notes: '会按 taskDefinitionKey 聚合审批记录' },
  { component: 'WorkflowApprovalTimeline', importPath: '@mango/workflow', scene: '审批记录列表', notes: '支持 record-extra 插槽扩展业务记录内容' },
];

const fieldSupportTable = [
  { category: '基础录入', fields: 'input、textarea、password、number、switch、date、datetime、time、slider、rate、color', notes: '用于普通流程变量采集，支持只读和必填规则。' },
  { category: '选项数据', fields: 'select、radio、checkbox、cascader、treeSelect、transfer、systemDict、businessType', notes: '字典字段通过 dictType / dictCode 关联系统字典。' },
  { category: '系统数据', fields: 'systemUser、systemOrg、systemDept、systemPost、systemRole', notes: '可通过 options 或 treeOptions 渲染；涉及系统主数据的业务场景应优先接入对应选择器。' },
  { category: '附件签名', fields: 'upload、imageUpload、signature、serialNo', notes: '上传组件提交文件 ID；签名字段只读时展示签名图片。' },
  { category: '展示容器', fields: 'alert、text、html、divider、tag、image、button、container', notes: '用于详情页展示和布局容器，不作为业务变量提交。' },
];

const runtimeCode = [
  '<script setup lang="ts">',
  `import { RuntimeFormRenderer, type RuntimeFormField } from '@mango/workflow';

const fields: RuntimeFormField[] = [
  { key: 'title', label: '申请标题', type: 'input', rules: [{ required: true, message: '申请标题不能为空' }] },
  { key: 'department', label: '所属部门', type: 'systemDept', treeOptions: deptTree },
];
const model = reactive({ title: '', department: undefined });
const permissions = { applicant: 'READONLY' };`,
  '<' + '/script>',
  `
<RuntimeFormRenderer
  :fields="fields"
  :model="model"
  :permissions="permissions"
  label-width="108px"
/>`,
].join('\n');

const applyCode = `import { registerBusinessApplyComponents } from '@mango/workflow';
import ContractApplyView from './ContractApplyView.vue';

registerBusinessApplyComponents({
  contractSealApply: {
    title: '合同用印申请',
    component: ContractApplyView,
  },
});

// 流程定义 renderConfig.applyPageKey = 'contractSealApply' 时进入该申请组件。`;

const approvalCode = `import { registerBusinessApprovalComponents } from '@mango/workflow';
import ContractApprovalDetail from './ContractApprovalDetail.vue';
import ContractRecordPanel from './ContractRecordPanel.vue';

registerBusinessApprovalComponents({
  CONTRACT_SEAL: {
    component: ContractApprovalDetail,
    commentMode: 'BUSINESS_FORM',
    recordPanelMode: 'CUSTOM',
    recordPanelComponent: ContractRecordPanel,
    collectComment: (context) => context.variables.approvalComment,
    collectVariables: (context) => ({
      approvalComment: context.variables.approvalComment,
      sealCount: context.variables.sealCount,
    }),
    async validateBeforeAction(context, action) {
      if (action === 'complete' && !String(context.variables.approvalComment || '').trim()) {
        throw new Error('审批意见不能为空');
      }
    },
  },
});`;

const traceCode = `<WorkflowProgressTree
  :node="workflowNode"
  current-node-key="finance_approve"
  :visited-node-keys="['root', 'dept_approve']"
  status="RUNNING"
/>

<WorkflowNodeTimeline
  :node="workflowNode"
  current-node-key="finance_approve"
  :visited-node-keys="['root', 'dept_approve']"
  :records="records"
/>

<WorkflowApprovalTimeline :records="records">
  <template #record-extra="{ record }">
    <BusinessRecordExtra :record="record" />
  </template>
</WorkflowApprovalTimeline>`;

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.workflow-demo-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.workflow-register-flow {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.register-step {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);

  strong {
    display: block;
    margin-bottom: 8px;
    color: var(--el-text-color-primary);
    font-size: 15px;
    font-weight: 600;
  }

  span {
    color: var(--el-text-color-regular);
    font-size: 14px;
    line-height: 1.7;
  }
}

.workflow-flow-preview {
  overflow: auto;
  width: 100%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

@media (max-width: 768px) {
  .workflow-register-flow {
    grid-template-columns: 1fr;
  }
}
</style>
