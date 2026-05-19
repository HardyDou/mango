<template>
  <div class="workflow-progress-tree">
    <div v-if="node" class="workflow-flow-sequence">
      <div class="workflow-terminal-node start" :class="terminalStartState">开始</div>
      <div class="workflow-tree-body">
        <WorkflowTreeNode
          :node="node"
          :current-node-key="currentNodeKey"
          :visited-node-keys="visitedNodeKeys"
          :completed="completed"
          :rejected="rejected"
          root
        />
      </div>
      <div class="workflow-terminal-node end" :class="terminalEndState">结束</div>
    </div>
    <el-empty v-else description="暂无流程设计图" />
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, type PropType } from 'vue';
import type { WorkflowDesignerNode } from '../../api/workflow';

defineOptions({ name: 'WorkflowProgressTree' });

const props = defineProps<{
  node?: WorkflowDesignerNode | null;
  currentNodeKey?: string;
  visitedNodeKeys?: string[];
  status?: string;
}>();

const completed = computed(() => props.status === '已通过' || props.status === '已结束' || props.status === 'APPROVED' || props.status === 'COMPLETED');
const rejected = computed(() => props.status === '已驳回' || props.status === '已拒绝' || props.status === 'REJECTED');
const hasWorkflowStarted = computed(() => Boolean(
  props.currentNodeKey
  || completed.value
  || rejected.value
  || props.visitedNodeKeys?.length,
));
const terminalStartState = computed(() => hasWorkflowStarted.value ? 'done' : 'pending');
const terminalEndState = computed(() => {
  if (rejected.value) return 'rejected';
  if (completed.value) return 'done';
  return 'pending';
});

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

    const renderCard = (node: WorkflowDesignerNode) => h('div', {
      class: ['workflow-node-card', nodeState(node), { root: nodeProps.root, branch: node.nodeType === 'EXCLUSIVE_BRANCH' }],
    }, [
      h('div', { class: 'workflow-node-title' }, [
        h('span', { class: 'workflow-node-dot' }),
        h('span', { class: 'workflow-node-name', title: nodeTitle(node) }, nodeTitle(node)),
      ]),
    ]);

    const renderNode = (node: WorkflowDesignerNode, root = false): any => {
      const child = node.childNode ? renderNode(node.childNode) : null;
      if (!isGatewayNode(node)) {
        return h('div', { class: 'workflow-node-stack' }, [
          h('div', { class: ['workflow-node-wrap', { root }] }, [renderCard(node)]),
          child,
        ]);
      }

      const branches = node.conditionNodes || [];
      return h('div', { class: 'workflow-node-stack gateway-stack' }, [
        h('div', { class: ['workflow-node-wrap', { root }] }, [renderCard(node)]),
        h('div', { class: 'workflow-branch-box-wrap' }, [
          h('div', { class: 'workflow-branch-label' }, node.nodeType === 'PARALLEL_GATEWAY' ? '并行' : '分支'),
          h('div', { class: 'workflow-branch-box' }, branches.map((branch, index) =>
            h('div', { class: 'workflow-branch-col', key: branch.id || `${branch.nodeName}-${index}` }, [
              h('div', { class: 'workflow-condition-node' }, [renderCard(branch)]),
              branch.childNode
                ? renderNode(branch.childNode)
                : h('div', { class: 'workflow-empty-branch' }, '无后续节点'),
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
</script>

<style>
.workflow-progress-tree {
  --workflow-line-color: var(--el-border-color);
  --workflow-line-width: 2px;
  min-width: max-content;
  padding: 12px 24px 24px;
  background: var(--el-bg-color);
}

.workflow-flow-sequence,
.workflow-tree-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.workflow-tree-body::before {
  content: '';
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
}

.workflow-terminal-node {
  position: relative;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 92px;
  height: 34px;
  padding: 0 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 600;
  background: var(--el-bg-color);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.workflow-terminal-node.end {
  margin-top: 24px;
}

.workflow-terminal-node.end::before {
  content: '';
  position: absolute;
  top: -25px;
  left: 50%;
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
  transform: translateX(-50%);
}

.workflow-terminal-node.done {
  border-color: var(--el-color-success-light-5);
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.workflow-terminal-node.rejected {
  border-color: var(--el-color-danger-light-5);
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.workflow-terminal-node.pending {
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-blank);
}

.workflow-node-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.workflow-node-stack > .workflow-node-stack::before,
.workflow-branch-box-wrap + .workflow-node-stack::before {
  content: '';
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
}

.workflow-node-wrap {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
}

.workflow-node-card {
  width: 176px;
  min-height: 40px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  background: var(--el-bg-color);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.workflow-node-card.done {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.workflow-node-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.workflow-node-card.pending {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-blank);
}

.workflow-node-card.rejected {
  border-color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  box-shadow: 0 0 0 2px var(--el-color-danger-light-8);
}

.workflow-node-card.branch {
  border-style: dashed;
}

.workflow-node-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 9px 14px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.workflow-node-card.active .workflow-node-title {
  color: var(--el-color-primary);
}

.workflow-node-card.done .workflow-node-title {
  color: var(--el-color-success);
}

.workflow-node-card.rejected .workflow-node-title {
  color: var(--el-color-danger);
}

.workflow-node-card.pending .workflow-node-title {
  color: var(--el-text-color-secondary);
}

.workflow-node-dot {
  display: inline-flex;
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 999px;
  background: var(--el-border-color);
}

.workflow-node-card.done .workflow-node-dot {
  background: var(--el-color-success);
}

.workflow-node-card.active .workflow-node-dot {
  background: var(--el-color-primary);
}

.workflow-node-card.rejected .workflow-node-dot {
  background: var(--el-color-danger);
}

.workflow-node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-branch-box-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100%;
}

.workflow-branch-box-wrap::before {
  content: '';
  width: var(--workflow-line-width);
  height: 24px;
  background: var(--workflow-line-color);
}

.workflow-branch-label {
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

.workflow-branch-box {
  position: relative;
  display: flex;
  min-height: 220px;
  border-top: var(--workflow-line-width) solid var(--workflow-line-color);
  border-bottom: var(--workflow-line-width) solid var(--workflow-line-color);
}

.workflow-branch-col {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  min-width: 284px;
  padding: 28px 32px;
}

.workflow-branch-col::before {
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

.workflow-condition-node,
.workflow-branch-col > .workflow-node-stack,
.workflow-empty-branch {
  position: relative;
  z-index: 1;
}

.workflow-condition-node + .workflow-node-stack::before,
.workflow-condition-node + .workflow-empty-branch::before {
  content: '';
  display: block;
  width: var(--workflow-line-width);
  height: 22px;
  margin: 0 auto;
  background: var(--workflow-line-color);
}

.workflow-empty-branch {
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
