<!--
  Workflow approval tree renderer.
  Local implementation for Mango Workflow.
  Branch layout ideas reference:
  - ldhnet/AntFlow-Designer, Apache-2.0
  - willianfu/jw-workflow-engine, Apache-2.0
  - lolicode/scui scWorkflow, MIT
  Copyright notices of referenced projects are kept in repository reference notes;
  this component does not import or depend on those projects.
-->
<template>
  <div v-if="!isGatewayNode" class="workflow-node-wrap">
    <div
      class="workflow-node-card"
      :class="{ root, 'branch-node': node.nodeType === 'EXCLUSIVE_BRANCH' }"
      :style="{ '--node-color': nodeColorOf(node) }"
      @click.stop="$emit('select', node)"
    >
      <div class="node-card-title">
        <el-icon><component :is="nodeIconOf(node)" /></el-icon>
        <span class="node-title-display" :title="node.nodeName || '节点名称'">{{ node.nodeName || '节点名称' }}</span>
        <el-button
          v-if="removable"
          class="node-card-delete"
          :icon="Close"
          text
          title="删除节点"
          @click.stop="$emit('remove-self')"
        />
      </div>
      <div class="node-card-type">{{ nodeSummary }}</div>
    </div>

    <WorkflowAddNode :catalog="catalog" @add="addNode" />
  </div>

  <div v-else class="workflow-branch-wrap">
    <div class="workflow-branch-box-wrap">
      <div class="workflow-branch-box">
        <el-button class="branch-add-plus" :icon="Plus" circle title="添加分支" @click.stop="addBranch" />
        <div v-for="(branch, index) in branchNodes" :key="branch.id" class="workflow-branch-col">
          <div class="workflow-condition-node">
            <div class="workflow-condition-node-box">
              <div
                class="workflow-node-card branch-node"
                :style="{ '--node-color': nodeColorOf(branch) }"
                @click.stop="$emit('select', branch)"
              >
                <div class="node-card-title">
                  <el-icon><component :is="nodeIconOf(branch)" /></el-icon>
                  <span class="node-title-display" :title="branch.nodeName || '分支名称'">{{ branch.nodeName || '分支名称' }}</span>
                  <el-button
                    class="node-card-delete"
                    :icon="Close"
                    text
                    title="删除分支"
                    @click.stop="removeBranch(index)"
                  />
                </div>
                <div class="node-card-type">{{ workflowNodeCardSummary(branch) }}</div>
              </div>
              <WorkflowAddNode :catalog="catalog" @add="item => addNodeToBranch(branch, item)" />
            </div>
          </div>
          <WorkflowNodeTree
            v-if="branch.childNode"
            :node="branch.childNode"
            :catalog="catalog"
            :variable-groups="variableGroups"
            removable
            @select="item => $emit('select', item)"
            @changed="$emit('changed')"
            @remove-self="replacement => removeBranchChild(branch, replacement)"
          />
          <div v-if="index === 0" class="top-left-cover-line" />
          <div v-if="index === 0" class="bottom-left-cover-line" />
          <div v-if="index === branchNodes.length - 1" class="top-right-cover-line" />
          <div v-if="index === branchNodes.length - 1" class="bottom-right-cover-line" />
        </div>
      </div>
      <WorkflowAddNode :catalog="catalog" @add="addNode" />
    </div>
  </div>

  <WorkflowNodeTree
    v-if="node.childNode"
    :node="node.childNode"
    :catalog="catalog"
    :variable-groups="variableGroups"
    removable
    @select="item => $emit('select', item)"
    @changed="$emit('changed')"
    @remove-self="removeCurrentChild"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Bell, Box, Cloudy, Close, Connection, ForkSpoon, Plus, Setting, Share, User } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
import { createNodeId, type WorkflowDesignerNode, type WorkflowNodeCatalog } from '../../../../api/workflow';
import WorkflowAddNode from './WorkflowAddNode.vue';
import type { WorkflowVariableGroup } from './types';

defineOptions({ name: 'WorkflowNodeTree' });

const props = withDefaults(defineProps<{
  node: WorkflowDesignerNode;
  catalog: WorkflowNodeCatalog[];
  variableGroups?: WorkflowVariableGroup[];
  root?: boolean;
  removable?: boolean;
}>(), {
  variableGroups: () => [],
  root: false,
  removable: false,
});

const emit = defineEmits<{
  select: [node: WorkflowDesignerNode];
  changed: [];
  'remove-self': [replacement?: WorkflowDesignerNode | null];
}>();

const nodeIconMap: Record<string, any> = {
  ROOT: User,
  APPROVAL: User,
  CC: Bell,
  EXCLUSIVE_GATEWAY: ForkSpoon,
  PARALLEL_GATEWAY: Share,
  SERVICE: Setting,
  SERVICE_BEAN: Box,
  SERVICE_HTTP: Cloudy,
  SERVICE_REMOTE: Connection,
  EVENT_PUBLISH: Bell,
  EXCLUSIVE_BRANCH: Share,
};

const isGatewayNode = computed(() => props.node.nodeType === 'EXCLUSIVE_GATEWAY' || props.node.nodeType === 'PARALLEL_GATEWAY');
const branchNodes = computed(() => props.node.conditionNodes || []);

const catalogItem = computed(() => props.catalog.find(item => item.nodeDefinitionCode === props.node.nodeDefinitionCode || item.nodeType === props.node.nodeType));
const nodeSummary = computed(() => workflowNodeCardSummary(props.node));

function addNode(item: WorkflowNodeCatalog) {
  const next = createDesignerNode(item, props.node.childNode || null);
  props.node.childNode = next;
  emit('select', next);
  emit('changed');
}

function addNodeToBranch(branch: WorkflowDesignerNode, item: WorkflowNodeCatalog) {
  const next = createDesignerNode(item, branch.childNode || null);
  branch.childNode = next;
  emit('select', next);
  emit('changed');
}

function createDesignerNode(item: WorkflowNodeCatalog, childNode: WorkflowDesignerNode | null): WorkflowDesignerNode {
  const next: WorkflowDesignerNode = {
    id: createNodeId(item.nodeType.toLowerCase()),
    nodeDefinitionCode: item.nodeDefinitionCode,
    nodeName: item.nodeName,
    nodeType: item.nodeType,
    bpmnType: item.bpmnType,
    executionType: item.executionType,
    description: item.description,
    childNode,
    conditionNodes: [],
    properties: parseDefaultProperties(item.defaultProperties),
  };
  if (item.nodeType === 'EXCLUSIVE_GATEWAY' || item.nodeType === 'PARALLEL_GATEWAY') {
    next.conditionNodes = [
      createBranchNode('分支1', item.nodeType === 'EXCLUSIVE_GATEWAY' ? '${true}' : ''),
      createBranchNode('分支2', ''),
    ];
  }
  return next;
}

function addBranch() {
  props.node.conditionNodes ||= [];
  props.node.conditionNodes.push(createBranchNode(`分支${props.node.conditionNodes.length + 1}`, ''));
  emit('changed');
}

async function removeBranch(index: number) {
  const branches = props.node.conditionNodes || [];
  if (branches.length <= 2) {
    try {
      await ElMessageBox.confirm(
        '当前条件/并行节点至少需要 2 个分支，删除后将移除整个分支节点，是否继续？',
        '删除分支节点',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
        },
      );
    } catch {
      return;
    }
    emit('remove-self', promoteRemainingBranch(index));
    emit('changed');
    return;
  }
  branches.splice(index, 1);
  emit('changed');
}

function removeCurrentChild(replacement?: WorkflowDesignerNode | null) {
  props.node.childNode = replacement !== undefined ? replacement : props.node.childNode?.childNode || null;
  emit('changed');
}

function removeBranchChild(branch: WorkflowDesignerNode, replacement?: WorkflowDesignerNode | null) {
  branch.childNode = replacement !== undefined ? replacement : branch.childNode?.childNode || null;
  emit('changed');
}

function promoteRemainingBranch(removeIndex: number) {
  const branches = props.node.conditionNodes || [];
  const remainingBranch = branches.find((_, index) => index !== removeIndex);
  if (!remainingBranch?.childNode) {
    return props.node.childNode || null;
  }
  appendTailNode(remainingBranch.childNode, props.node.childNode || null);
  return remainingBranch.childNode;
}

function appendTailNode(node: WorkflowDesignerNode, appendNode: WorkflowDesignerNode | null) {
  if (!appendNode) return;
  let current = node;
  while (current.childNode) {
    current = current.childNode;
  }
  current.childNode = appendNode;
}

function createBranchNode(name: string, expression: string): WorkflowDesignerNode {
  return {
    id: createNodeId('branch'),
    nodeName: name,
    nodeType: 'EXCLUSIVE_BRANCH',
    conditionExpression: expression,
    childNode: null,
    conditionNodes: [],
    properties: {},
  };
}

function parseDefaultProperties(value?: string) {
  if (!value) return {};
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

function resolveNodeIcon(item: Partial<WorkflowNodeCatalog | WorkflowDesignerNode>) {
  const iconName = 'icon' in item ? item.icon : undefined;
  if (iconName && iconName in nodeIconMap) {
    return nodeIconMap[iconName];
  }
  return nodeIconMap[item.nodeType || ''] || nodeIconMap[item.executionType || ''] || Setting;
}

function catalogOf(node: WorkflowDesignerNode) {
  return props.catalog.find(item => item.nodeDefinitionCode === node.nodeDefinitionCode || item.nodeType === node.nodeType);
}

function nodeColorOf(node: WorkflowDesignerNode) {
  return catalogOf(node)?.color || (props.root && node.id === props.node.id ? '#64748b' : node.nodeType === 'EXCLUSIVE_BRANCH' ? '#15bc83' : '#2563eb');
}

function nodeIconOf(node: WorkflowDesignerNode) {
  return resolveNodeIcon(catalogOf(node) || node);
}

function workflowNodeCardSummary(node: WorkflowDesignerNode) {
  if (node.nodeType === 'EXCLUSIVE_BRANCH') {
    return node.conditionExpression ? `条件：${formatConditionSummary(node.conditionExpression)}` : '请设置条件';
  }
  if (node.nodeType === 'ROOT') return '发起人发起流程';
  if (node.nodeType === 'APPROVAL' || node.executionType === 'USER_TASK' || node.bpmnType === 'userTask') return '人工处理节点';
  if (node.nodeType === 'CC') return '流程流转后通知相关人员';
  if (node.nodeType === 'EXCLUSIVE_GATEWAY') return '按条件选择一个分支继续流转';
  if (node.nodeType === 'PARALLEL_GATEWAY') return '多个分支同时流转并在结束后合并';
  if (node.nodeType === 'SERVICE' || node.bpmnType === 'serviceTask' || node.executionType === 'SPRING_BEAN') return '调用白名单 Spring Bean 执行业务动作';
  if (node.executionType === 'HTTP_URL') return '调用受控 HTTP URL 执行业务动作';
  if (node.executionType === 'REMOTE_SERVICE') return '调用受控远程服务执行业务动作';
  if (node.executionType === 'EVENT_PUBLISH') return '发布流程事件';
  return node.description || '节点配置';
}

function formatConditionSummary(expression: string) {
  const inner = expression.match(/^\$\{\s*(.+?)\s*}$/)?.[1] || expression;
  const normalized = inner
    .replace(/\(([^()]+)\)/g, '$1')
    .replace(/!\s*([A-Za-z_][\w.]*)\.contains\(([^)]+)\)/g, (_, field, value) => `${variableLabel(field)} 不属于/不包含 ${formatConditionSummaryValue(value)}`)
    .replace(/([A-Za-z_][\w.]*)\.contains\(([^)]+)\)/g, (_, field, value) => `${variableLabel(field)} 属于/包含 ${formatConditionSummaryValue(value)}`)
    .replace(/([A-Za-z_][\w.]*)\s*(==|!=|>=|<=|>|<)\s*('(?:\\'|[^'])*'|true|false|null|-?\d+(?:\.\d+)?|[^\s&|]+)/g, (_, field, operator, value) => `${variableLabel(field)} ${operatorLabel(operator)} ${formatConditionSummaryValue(value)}`)
    .replace(/\s*&&\s*/g, ' 且 ')
    .replace(/\s*\|\|\s*/g, ' 或 ')
    .trim();
  return normalized || expression;
}

function variableLabel(field: string) {
  for (const group of props.variableGroups) {
    const matched = group.options.find(item => item.value === field);
    if (matched) {
      return matched.label || field;
    }
  }
  return field;
}

function operatorLabel(operator: string) {
  const map: Record<string, string> = {
    '==': '是',
    '!=': '不是',
    '>': '大于',
    '>=': '大于等于',
    '<': '小于',
    '<=': '小于等于',
  };
  return map[operator] || operator;
}

function formatConditionSummaryValue(value: string) {
  const trimmed = String(value || '').trim();
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed.slice(1, -1).replace(/\\'/g, "'");
  }
  return trimmed;
}
</script>

<style scoped>
.workflow-node-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.workflow-node-card {
  position: relative;
  z-index: 2;
  width: 220px;
  min-height: 72px;
  border: 1px solid color-mix(in srgb, var(--node-color) 36%, var(--el-border-color));
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: 0 4px 14px rgba(31, 41, 55, 0.08);
  cursor: pointer;
  overflow: hidden;
}

.workflow-node-card.branch-node {
  border-style: dashed;
}

.workflow-node-card.root {
  border-color: var(--el-color-primary-light-5);
}

.node-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  font-weight: 600;
  color: #fff;
  background: var(--node-color);
}

.node-title-display {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-card-delete {
  margin-left: auto;
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.88);
}

.node-card-delete:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

.node-card-type {
  padding: 10px 12px;
  text-align: left;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  word-break: break-word;
}

.workflow-branch-wrap {
  position: relative;
  display: inline-flex;
  width: 100%;
}

.workflow-branch-box-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 270px;
  width: 100%;
  flex-shrink: 0;
}

.workflow-branch-box {
  position: relative;
  display: flex;
  overflow: visible;
  min-height: 180px;
  height: auto;
  margin-top: 15px;
  border-top: var(--workflow-line-width) solid var(--workflow-line-color);
  border-bottom: var(--workflow-line-width) solid var(--workflow-line-color);
  background: var(--workflow-branch-bg);
}

.branch-add-plus {
  position: absolute;
  top: -17px;
  left: 50%;
  z-index: 4;
  width: 34px;
  height: 34px;
  border: 0;
  color: var(--el-color-primary);
  background: var(--el-bg-color);
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.12);
  transform: translateX(-50%);
}

.workflow-branch-col {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  background: var(--workflow-branch-bg);
}

.workflow-branch-col::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: var(--workflow-line-width);
  background: var(--workflow-line-color);
  transform: translateX(-50%);
  z-index: 0;
}

.workflow-condition-node {
  display: inline-flex;
  min-height: 220px;
  flex-direction: column;
  flex-grow: 1;
}

.workflow-condition-node-box {
  position: relative;
  z-index: 1;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
  padding: 30px 46px 0;
}

.workflow-condition-node-box::before {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: var(--workflow-line-width);
  height: 100%;
  background: var(--workflow-line-color);
  z-index: -1;
}

.workflow-branch-col > .workflow-node-wrap,
.workflow-branch-col > .workflow-branch-wrap {
  z-index: 1;
}

.top-left-cover-line,
.top-right-cover-line,
.bottom-left-cover-line,
.bottom-right-cover-line {
  position: absolute;
  z-index: 2;
  width: 50%;
  height: 8px;
  background: var(--workflow-branch-bg);
}

.top-left-cover-line,
.top-right-cover-line {
  top: -4px;
}

.bottom-left-cover-line,
.bottom-right-cover-line {
  bottom: -4px;
}

.top-left-cover-line,
.bottom-left-cover-line {
  left: -1px;
}

.top-right-cover-line,
.bottom-right-cover-line {
  right: -1px;
}
</style>
