<template>
  <div class="workflow-node-timeline">
    <el-timeline v-if="timelineItems.length" class="node-timeline-list">
      <el-timeline-item
        v-for="item in timelineItems"
        :key="item.key"
        :type="stateTimelineType(item.state)"
        :hollow="item.state === 'pending'"
        :timestamp="nodeTimeText(item)"
        placement="bottom"
      >
        <div class="node-card" :class="item.state">
          <div class="node-head">
            <div class="node-title-group">
              <div class="node-title" :title="item.name">{{ item.name }}</div>
            </div>
          </div>
          <div v-if="item.key !== '__start'" class="node-content">
            <template v-if="item.records.length">
              <div
                v-for="record in item.records"
                :key="record.id || `${item.key}-${record.action}-${record.createdTime || ''}`"
                class="node-record"
              >
                <div class="node-record-line">
                  <strong>{{ record.actionName || record.action || '-' }}：</strong>
                  <span class="node-record-comment">{{ record.comment || '无审批意见' }}</span>
                </div>
                <div class="node-record-operator">处理人：{{ record.operatorName || '-' }}</div>
              </div>
            </template>
            <div v-else class="node-empty">{{ emptyTextOf(item.state) }}</div>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无流程节点" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { WorkflowDesignerNode, WorkflowTaskRecord } from '../../api/workflow';

defineOptions({ name: 'WorkflowNodeTimeline' });

const props = withDefaults(defineProps<{
  node?: WorkflowDesignerNode | null;
  currentNodeKey?: string;
  visitedNodeKeys?: string[];
  status?: string;
  records?: WorkflowTaskRecord[];
}>(), {
  currentNodeKey: '',
  visitedNodeKeys: () => [],
  status: '',
  records: () => [],
});

type NodeState = 'done' | 'active' | 'pending' | 'rejected';

interface TimelineNodeItem {
  key: string;
  id?: string;
  name: string;
  typeLabel: string;
  state: NodeState;
  records: WorkflowTaskRecord[];
}

const completed = computed(() => ['已通过', '已结束', 'APPROVED', 'COMPLETED'].includes(props.status));
const rejected = computed(() => ['已驳回', '已拒绝', 'REJECTED'].includes(props.status));
const currentKeySet = computed(() => new Set(String(props.currentNodeKey || '')
  .split(',')
  .map(item => item.trim())
  .filter(Boolean)));
const visitedKeySet = computed(() => new Set((props.visitedNodeKeys || [])
  .flatMap(key => String(key).split(','))
  .map(key => key.trim())
  .filter(Boolean)));
const recordMap = computed(() => {
  const map = new Map<string, WorkflowTaskRecord[]>();
  (props.records || []).forEach((record) => {
    const key = record.taskDefinitionKey;
    if (!key) return;
    map.set(key, [...(map.get(key) || []), record]);
  });
  return map;
});
const firstRecordTime = computed(() => (props.records || [])
  .map(record => record.createdTime)
  .filter(Boolean)
  .sort()[0] || '');

const timelineItems = computed<TimelineNodeItem[]>(() => {
  const started = Boolean(props.node && (currentKeySet.value.size || visitedKeySet.value.size || completed.value || rejected.value));
  const items: TimelineNodeItem[] = [
    {
      key: '__start',
      name: '开始',
      typeLabel: '流程开始',
      state: started ? 'done' : 'pending',
      records: [],
    },
  ];
  if (props.node) {
    flattenNode(props.node).forEach((node) => {
      items.push({
        key: node.id,
        id: node.id,
        name: node.nodeName || nodeTypeLabel(node),
        typeLabel: nodeTypeLabel(node),
        state: nodeState(node),
        records: recordMap.value.get(node.id) || [],
      });
    });
  }
  items.push({
    key: '__end',
    name: '结束',
    typeLabel: '流程结束',
    state: rejected.value ? 'rejected' : completed.value ? 'done' : 'pending',
    records: [],
  });
  return items;
});

function flattenNode(node: WorkflowDesignerNode): WorkflowDesignerNode[] {
  const items: WorkflowDesignerNode[] = [node];
  (node.conditionNodes || []).forEach((branch) => {
    items.push(branch);
    if (branch.childNode) {
      items.push(...flattenNode(branch.childNode));
    }
  });
  if (node.childNode) {
    items.push(...flattenNode(node.childNode));
  }
  return items;
}

function nodeState(node: WorkflowDesignerNode): NodeState {
  if (rejected.value && isRejectedNode(node)) return 'rejected';
  if (!completed.value && !rejected.value && currentKeySet.value.has(node.id)) return 'active';
  if (node.nodeType === 'ROOT' && (visitedKeySet.value.size || currentKeySet.value.size || completed.value || rejected.value)) return 'done';
  if (visitedKeySet.value.has(node.id)) return 'done';
  if (hasVisitedDescendant(node)) return 'done';
  return 'pending';
}

function isRejectedNode(node: WorkflowDesignerNode) {
  if (currentKeySet.value.has(node.id)) return true;
  const visitedKeys = Array.from(visitedKeySet.value);
  return visitedKeys[visitedKeys.length - 1] === node.id;
}

function hasVisitedDescendant(node: WorkflowDesignerNode): boolean {
  if (node.childNode && (visitedKeySet.value.has(node.childNode.id) || currentKeySet.value.has(node.childNode.id) || hasVisitedDescendant(node.childNode))) {
    return true;
  }
  return (node.conditionNodes || []).some(branch =>
    visitedKeySet.value.has(branch.id)
    || currentKeySet.value.has(branch.id)
    || hasVisitedDescendant(branch),
  );
}

function nodeTypeLabel(node: WorkflowDesignerNode) {
  if (node.nodeType === 'ROOT') return '发起人';
  if (node.nodeType === 'APPROVAL' || node.executionType === 'USER_TASK' || node.bpmnType === 'userTask') return '人工审批';
  if (node.nodeType === 'CC') return '抄送';
  if (node.nodeType === 'EXCLUSIVE_GATEWAY') return '条件分支';
  if (node.nodeType === 'PARALLEL_GATEWAY') return '并行分支';
  if (node.nodeType === 'EXCLUSIVE_BRANCH') return '分支条件';
  if (node.executionType === 'EVENT_PUBLISH') return '事件发布';
  if (node.bpmnType === 'serviceTask' || node.nodeType?.startsWith('SERVICE')) return '服务任务';
  return node.description || '流程节点';
}

function stateLabel(state: NodeState) {
  if (state === 'done') return '已经过';
  if (state === 'active') return '处理中';
  if (state === 'rejected') return '驳回';
  return '未到达';
}

function stateTagType(state: NodeState) {
  if (state === 'done') return 'success';
  if (state === 'active') return 'primary';
  if (state === 'rejected') return 'danger';
  return 'info';
}

function stateTimelineType(state: NodeState) {
  if (state === 'done') return 'success';
  if (state === 'active') return 'primary';
  if (state === 'rejected') return 'danger';
  return 'info';
}

function nodeTimeText(item: TimelineNodeItem) {
  const firstTime = item.records.find(record => record.createdTime)?.createdTime;
  if (firstTime) return firstTime;
  if (item.key === '__start') return firstRecordTime.value || '-';
  return '-';
}

function emptyTextOf(state: NodeState) {
  if (state === 'active') return '当前节点待处理';
  if (state === 'done') return '该节点已流转，暂无审批意见';
  if (state === 'rejected') return '流程在该节点驳回或结束';
  return '流程尚未到达该节点';
}

</script>

<style scoped>
.workflow-node-timeline {
  min-width: 0;
}

.node-timeline-list {
  padding-left: 8px;
}

.node-timeline-list :deep(.el-timeline-item) {
  padding-bottom: 18px;
}

.node-timeline-list :deep(.el-timeline-item__tail) {
  border-left-color: var(--el-border-color);
}

.node-timeline-list :deep(.el-timeline-item__node) {
  box-shadow: 0 0 0 4px var(--el-bg-color), 0 0 0 5px var(--el-border-color-lighter);
}

.node-timeline-list :deep(.el-timeline-item__wrapper) {
  padding-left: 18px;
}

.node-timeline-list :deep(.el-timeline-item__timestamp.is-bottom) {
  margin-top: 5px;
  margin-bottom: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.node-card {
  position: relative;
  min-width: 0;
  padding-bottom: 2px;
}

.node-card.done:first-child .node-title {
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.node-title {
  width: 100%;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-title-group {
  min-width: 0;
}

.node-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.node-content {
  min-width: 0;
  margin-top: 8px;
  padding-bottom: 10px;
}

.node-record + .node-record {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color);
}

.node-empty {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.node-record-line {
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.node-record-line strong {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.node-record-comment {
  color: var(--el-text-color-regular);
}

.node-record-operator {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-align: right;
}

@media (max-width: 520px) {
  .node-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
