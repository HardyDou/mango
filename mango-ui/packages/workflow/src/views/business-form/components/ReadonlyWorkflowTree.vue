<template>
  <div class="readonly-workflow-tree">
    <WorkflowTreeNode
      v-if="node"
      :node="node"
      :current-node-key="currentNodeKey"
      :visited-node-keys="visitedNodeKeys"
      :completed="completed"
      :rejected="rejected"
      root
    />
    <el-empty v-else description="暂无流程设计图" />
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, type PropType } from 'vue';
import { Bell, Box, Cloudy, Connection, ForkSpoon, Share, User } from '@element-plus/icons-vue';
import type { WorkflowDesignerNode } from '../../../api/workflow';

defineOptions({ name: 'ReadonlyWorkflowTree' });

const props = defineProps<{
  node?: WorkflowDesignerNode | null;
  currentNodeKey?: string;
  visitedNodeKeys?: string[];
  status?: string;
}>();

const completed = computed(() => props.status === '已通过' || props.status === '已结束');
const rejected = computed(() => props.status === '已驳回');

const nodeIconMap: Record<string, any> = {
  ROOT: User,
  APPROVAL: User,
  CC: Bell,
  EXCLUSIVE_GATEWAY: ForkSpoon,
  PARALLEL_GATEWAY: Share,
  SERVICE: Box,
  SERVICE_BEAN: Box,
  SERVICE_HTTP: Cloudy,
  SERVICE_REMOTE: Connection,
  EVENT_PUBLISH: Bell,
  EXCLUSIVE_BRANCH: Share,
};

const WorkflowTreeNode = defineComponent({
  name: 'WorkflowTreeNode',
  props: {
    node: {
      type: Object as PropType<WorkflowDesignerNode>,
      required: true,
    },
    currentNodeKey: {
      type: String,
      default: '',
    },
    visitedNodeKeys: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
    completed: {
      type: Boolean,
      default: false,
    },
    rejected: {
      type: Boolean,
      default: false,
    },
    root: {
      type: Boolean,
      default: false,
    },
  },
  setup(nodeProps) {
    const visitedSet = () => new Set(nodeProps.visitedNodeKeys || []);
    const currentKeySet = () => new Set(String(nodeProps.currentNodeKey || '')
      .split(',')
      .map(item => item.trim())
      .filter(Boolean));
    const rejectedNodeKey = () => {
      const currentKeys = currentKeySet();
      if (currentKeys.size) {
        return '';
      }
      const visitedKeys = nodeProps.visitedNodeKeys || [];
      return visitedKeys[visitedKeys.length - 1] || '';
    };
    const isCurrent = (node: WorkflowDesignerNode) => Boolean(
      !nodeProps.completed
      && !nodeProps.rejected
      && currentKeySet().has(node.id),
    );
    const isRejectedNode = (node: WorkflowDesignerNode) => Boolean(
      nodeProps.rejected
      && (currentKeySet().has(node.id) || rejectedNodeKey() === node.id),
    );
    const hasWorkflowStarted = () => Boolean(
      nodeProps.currentNodeKey
      || nodeProps.completed
      || nodeProps.rejected
      || visitedSet().size,
    );
    const hasVisitedDescendant = (node: WorkflowDesignerNode): boolean => {
      const keys = visitedSet();
      if (node.childNode && (keys.has(node.childNode.id) || isCurrent(node.childNode) || hasVisitedDescendant(node.childNode))) {
        return true;
      }
      return (node.conditionNodes || []).some(branch =>
        keys.has(branch.id)
        || isCurrent(branch)
        || hasVisitedDescendant(branch),
      );
    };
    const nodeState = (node: WorkflowDesignerNode) => {
      if (isRejectedNode(node)) return 'rejected';
      if (isCurrent(node)) return 'active';
      if (node.nodeType === 'ROOT' && hasWorkflowStarted()) return 'done';
      if (visitedSet().has(node.id)) return 'done';
      if (isGatewayNode(node) || node.nodeType === 'EXCLUSIVE_BRANCH') {
        return hasVisitedDescendant(node) ? 'done' : 'pending';
      }
      return 'pending';
    };
    const nodeTitle = (node: WorkflowDesignerNode) => node.nodeName || nodeTypeLabel(node);
    const nodeTypeLabel = (node: WorkflowDesignerNode) => {
      if (node.nodeType === 'ROOT') return '发起人';
      if (node.nodeType === 'APPROVAL' || node.executionType === 'USER_TASK' || node.bpmnType === 'userTask') return '人工审批';
      if (node.nodeType === 'CC') return '抄送';
      if (node.nodeType === 'EXCLUSIVE_GATEWAY') return '条件分支';
      if (node.nodeType === 'PARALLEL_GATEWAY') return '并行分支';
      if (node.nodeType === 'EXCLUSIVE_BRANCH') return '分支';
      if (node.executionType === 'EVENT_PUBLISH') return '事件发布';
      if (node.bpmnType === 'serviceTask' || node.nodeType?.startsWith('SERVICE')) return '服务任务';
      return node.description || '流程节点';
    };
    const nodeSummary = (node: WorkflowDesignerNode) => {
      if (node.nodeType === 'EXCLUSIVE_BRANCH') {
        return node.conditionExpression ? `条件：${formatConditionSummary(node.conditionExpression)}` : '默认分支';
      }
      if (node.nodeType === 'EXCLUSIVE_GATEWAY') return '满足条件的一个分支继续流转';
      if (node.nodeType === 'PARALLEL_GATEWAY') return '多个分支并行执行后汇聚';
      if (node.nodeType === 'ROOT') return '业务提交申请后进入流程';
      return node.description || nodeTypeLabel(node);
    };
    const nodeStateLabel = (node: WorkflowDesignerNode) => {
      const state = nodeState(node);
      if (state === 'active') return '当前';
      if (state === 'done') return '经过';
      if (state === 'rejected') return '驳回';
      return '未经过';
    };
    const iconOf = (node: WorkflowDesignerNode) => nodeIconMap[node.nodeType] || nodeIconMap[node.executionType || ''] || Box;

    const renderCard = (node: WorkflowDesignerNode) => h('div', {
      class: ['readonly-node-card', nodeState(node), { root: nodeProps.root, branch: node.nodeType === 'EXCLUSIVE_BRANCH' }],
    }, [
      h('div', { class: 'readonly-node-title' }, [
        h('span', { class: 'readonly-node-icon' }, [h(iconOf(node))]),
        h('span', { class: 'readonly-node-name', title: nodeTitle(node) }, nodeTitle(node)),
        h('span', { class: 'readonly-node-state-badge' }, nodeStateLabel(node)),
      ]),
      h('div', { class: 'readonly-node-type' }, nodeTypeLabel(node)),
      h('div', { class: 'readonly-node-summary' }, nodeSummary(node)),
    ]);

    const renderNode = (node: WorkflowDesignerNode, root = false): any => {
      const child = node.childNode ? renderNode(node.childNode) : null;
      if (!isGatewayNode(node)) {
        return h('div', { class: 'readonly-node-stack' }, [
          h('div', { class: ['readonly-node-wrap', { root }] }, [renderCard(node)]),
          child,
        ]);
      }

      const branches = node.conditionNodes || [];
      return h('div', { class: 'readonly-node-stack gateway-stack' }, [
        h('div', { class: ['readonly-node-wrap', { root }] }, [renderCard(node)]),
        h('div', { class: 'readonly-branch-box-wrap' }, [
          h('div', { class: 'readonly-branch-label' }, node.nodeType === 'PARALLEL_GATEWAY' ? '并行' : '分支'),
          h('div', { class: 'readonly-branch-box' }, branches.map((branch, index) =>
            h('div', { class: 'readonly-branch-col', key: branch.id || `${branch.nodeName}-${index}` }, [
              h('div', { class: 'readonly-condition-node' }, [renderCard(branch)]),
              branch.childNode
                ? renderNode(branch.childNode)
                : h('div', { class: 'readonly-empty-branch' }, '无后续节点'),
              index === 0 ? h('div', { class: 'top-left-cover-line' }) : null,
              index === 0 ? h('div', { class: 'bottom-left-cover-line' }) : null,
              index === branches.length - 1 ? h('div', { class: 'top-right-cover-line' }) : null,
              index === branches.length - 1 ? h('div', { class: 'bottom-right-cover-line' }) : null,
            ]),
          )),
        ]),
        child,
      ]);
    };

    return () => renderNode(nodeProps.node, nodeProps.root);
  },
});

function isGatewayNode(node: WorkflowDesignerNode) {
  return node.nodeType === 'EXCLUSIVE_GATEWAY' || node.nodeType === 'PARALLEL_GATEWAY';
}

function formatConditionSummary(expression: string) {
  const inner = expression.match(/^\$\{\s*(.+?)\s*}$/)?.[1] || expression;
  return inner
    .replace(/\(([^()]+)\)/g, '$1')
    .replace(/([A-Za-z_][\w.]*)\s*(==|!=|>=|<=|>|<)\s*('(?:\\'|[^'])*'|true|false|null|-?\d+(?:\.\d+)?|[^\s&|]+)/g, (_, field, operator, value) => `${field} ${operatorLabel(operator)} ${formatConditionSummaryValue(value)}`)
    .replace(/\s*&&\s*/g, ' 且 ')
    .replace(/\s*\|\|\s*/g, ' 或 ')
    .trim();
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

<style>
.readonly-workflow-tree {
  --workflow-line-color: var(--el-border-color);
  --workflow-line-width: 2px;
  min-width: max-content;
  padding: 12px 24px 24px;
  background: var(--el-bg-color);
}

.readonly-node-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.readonly-node-stack > .readonly-node-stack::before,
.readonly-branch-box-wrap + .readonly-node-stack::before {
  content: '';
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
}

.readonly-node-wrap {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
}

.readonly-node-card {
  width: 220px;
  min-height: 86px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: 0 4px 14px rgba(31, 41, 55, 0.06);
}

.readonly-node-card.done {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.readonly-node-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.readonly-node-card.pending {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-blank);
}

.readonly-node-card.rejected {
  border-color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  box-shadow: 0 0 0 2px var(--el-color-danger-light-8);
}

.readonly-node-card.branch {
  border-style: dashed;
}

.readonly-node-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  background: var(--el-fill-color-light);
}

.readonly-node-card.active .readonly-node-title {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.readonly-node-card.done .readonly-node-title {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.readonly-node-card.rejected .readonly-node-title {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.readonly-node-card.pending .readonly-node-title {
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.readonly-node-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  overflow: hidden;
}

.readonly-node-icon svg {
  display: block;
  width: 16px;
  height: 16px;
}

.readonly-node-title span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.readonly-node-name {
  flex: 1;
}

.readonly-node-state-badge {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 500;
  background: var(--el-fill-color);
}

.readonly-node-card.done .readonly-node-state-badge {
  color: var(--el-color-success);
  background: var(--el-color-success-light-8);
}

.readonly-node-card.active .readonly-node-state-badge {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-8);
}

.readonly-node-card.rejected .readonly-node-state-badge {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-8);
}

.readonly-node-type,
.readonly-node-summary {
  padding: 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.readonly-node-type {
  margin-top: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.readonly-node-summary {
  margin: 4px 0 10px;
  min-height: 18px;
  word-break: break-word;
}

.readonly-branch-box-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100%;
}

.readonly-branch-box-wrap::before {
  content: '';
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
}

.readonly-branch-label {
  position: absolute;
  top: 9px;
  z-index: 3;
  padding: 2px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  background: var(--el-bg-color);
}

.readonly-branch-box {
  position: relative;
  display: flex;
  min-height: 220px;
  border-top: var(--workflow-line-width) solid var(--workflow-line-color);
  border-bottom: var(--workflow-line-width) solid var(--workflow-line-color);
}

.readonly-branch-col {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  min-width: 284px;
  padding: 28px 32px;
}

.readonly-branch-col::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  z-index: 0;
  width: var(--workflow-line-width);
  background: var(--workflow-line-color);
  transform: translateX(-50%);
}

.readonly-condition-node,
.readonly-branch-col > .readonly-node-stack,
.readonly-empty-branch {
  position: relative;
  z-index: 1;
}

.readonly-condition-node + .readonly-node-stack::before,
.readonly-condition-node + .readonly-empty-branch::before {
  content: '';
  display: block;
  width: var(--workflow-line-width);
  height: 22px;
  margin: 0 auto;
  background: var(--workflow-line-color);
}

.readonly-empty-branch {
  width: 160px;
  padding: 10px 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  text-align: center;
  font-size: 12px;
  background: var(--el-bg-color);
}

.top-left-cover-line,
.top-right-cover-line,
.bottom-left-cover-line,
.bottom-right-cover-line {
  position: absolute;
  z-index: 2;
  width: 50%;
  height: 8px;
  background: var(--el-bg-color);
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
