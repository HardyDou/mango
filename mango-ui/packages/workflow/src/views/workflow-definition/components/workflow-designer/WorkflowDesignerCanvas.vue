<!--
  Workflow designer canvas.
  Local implementation for Mango Workflow.
  Canvas interactions reference mature approval-flow editors from:
  - willianfu/jw-workflow-engine, Apache-2.0
  - crowncloud/smart-flow-design, Apache-2.0
  - lolicode/scui scWorkflow, MIT
  No runtime dependency is introduced from those projects.
-->
<template>
  <div class="node-canvas-shell">
    <div class="canvas-tools" @mousedown.stop @click.stop>
      <el-button circle size="small" @click="zoomOut">-</el-button>
      <span>{{ Math.round(canvasZoom * 100) }}%</span>
      <el-button circle size="small" @click="zoomIn">+</el-button>
      <el-button :icon="Refresh" circle size="small" title="重置" @click="resetCanvasView" />
    </div>

    <div
      ref="nodeCanvasRef"
      class="node-canvas"
      :class="{ 'canvas-dragging': isCanvasDragging }"
      :style="{ '--canvas-grid-size': `${24 * canvasZoom}px` }"
      @click="handleCanvasClick"
      @mousedown="handleCanvasPointerDown"
      @wheel="handleCanvasWheel"
    >
      <div class="node-canvas-scale" :style="{ transform: `scale(${canvasZoom})` }">
        <WorkflowNodeTree
          :node="root"
          :catalog="catalog"
          :variable-groups="variableGroups"
          root
          @select="node => $emit('select', node)"
          @changed="$emit('changed')"
        />
        <div class="end-node">结束</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import type { WorkflowDesignerNode, WorkflowNodeCatalog } from '../../../../api/workflow';
import type { WorkflowVariableGroup } from './types';
import WorkflowNodeTree from './WorkflowNodeTree.vue';

defineProps<{
  root: WorkflowDesignerNode;
  catalog: WorkflowNodeCatalog[];
  variableGroups: WorkflowVariableGroup[];
}>();

const emit = defineEmits<{
  select: [node: WorkflowDesignerNode];
  changed: [];
  blank: [];
}>();

const nodeCanvasRef = ref<HTMLDivElement>();
const canvasZoom = ref(1);
const isCanvasDragging = ref(false);
const canvasDragMoved = ref(false);
const canvasDragState = reactive({
  startX: 0,
  startY: 0,
  scrollLeft: 0,
  scrollTop: 0,
});

onMounted(() => {
  window.addEventListener('mousemove', handleCanvasPointerMove);
  window.addEventListener('mouseup', stopCanvasDragging);
});

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleCanvasPointerMove);
  window.removeEventListener('mouseup', stopCanvasDragging);
});

function isCanvasInteractiveTarget(target: HTMLElement) {
  return Boolean(
    target.closest('.workflow-node-card')
    || target.closest('.workflow-add-node')
    || target.closest('.branch-add-plus')
    || target.closest('.node-picker')
    || target.closest('.el-popover')
    || target.closest('.el-select-dropdown')
    || target.closest('.el-popper')
    || target.closest('.el-button')
    || target.closest('button')
    || target.closest('input')
    || target.closest('textarea')
  );
}

function handleCanvasClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (canvasDragMoved.value) {
    canvasDragMoved.value = false;
    return;
  }
  if (!target || !isCanvasInteractiveTarget(target)) {
    emit('blank');
  }
}

function handleCanvasPointerDown(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (event.button !== 0 || !nodeCanvasRef.value || !target || isCanvasInteractiveTarget(target)) {
    return;
  }
  isCanvasDragging.value = true;
  canvasDragMoved.value = false;
  canvasDragState.startX = event.clientX;
  canvasDragState.startY = event.clientY;
  canvasDragState.scrollLeft = nodeCanvasRef.value.scrollLeft;
  canvasDragState.scrollTop = nodeCanvasRef.value.scrollTop;
}

function handleCanvasPointerMove(event: MouseEvent) {
  if (!isCanvasDragging.value || !nodeCanvasRef.value) {
    return;
  }
  const deltaX = event.clientX - canvasDragState.startX;
  const deltaY = event.clientY - canvasDragState.startY;
  if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
    canvasDragMoved.value = true;
  }
  nodeCanvasRef.value.scrollLeft = canvasDragState.scrollLeft - deltaX;
  nodeCanvasRef.value.scrollTop = canvasDragState.scrollTop - deltaY;
}

function stopCanvasDragging() {
  isCanvasDragging.value = false;
}

async function updateCanvasZoom(nextZoom: number, event?: WheelEvent) {
  const container = nodeCanvasRef.value;
  const prevZoom = canvasZoom.value;
  const clampedZoom = Math.max(0.5, Math.min(1.8, Number(nextZoom.toFixed(2))));
  if (!container || clampedZoom === prevZoom) {
    canvasZoom.value = clampedZoom;
    return;
  }
  const rect = container.getBoundingClientRect();
  const focusX = event ? event.clientX - rect.left : rect.width / 2;
  const focusY = event ? event.clientY - rect.top : rect.height / 2;
  const worldX = (container.scrollLeft + focusX) / prevZoom;
  const worldY = (container.scrollTop + focusY) / prevZoom;
  canvasZoom.value = clampedZoom;
  await nextTick();
  container.scrollLeft = worldX * clampedZoom - focusX;
  container.scrollTop = worldY * clampedZoom - focusY;
}

async function resetCanvasView() {
  const container = nodeCanvasRef.value;
  canvasZoom.value = 1;
  await nextTick();
  if (!container) {
    return;
  }
  container.scrollLeft = Math.max(0, (container.scrollWidth - container.clientWidth) / 2);
  container.scrollTop = 0;
}

function handleCanvasWheel(event: WheelEvent) {
  if (!event.ctrlKey && !event.metaKey) {
    return;
  }
  event.preventDefault();
  updateCanvasZoom(canvasZoom.value + (event.deltaY < 0 ? 0.1 : -0.1), event);
}

function zoomIn() {
  updateCanvasZoom(canvasZoom.value + 0.1);
}

function zoomOut() {
  updateCanvasZoom(canvasZoom.value - 0.1);
}
</script>

<style scoped>
.node-canvas-shell {
  position: relative;
  height: calc(100vh - 260px);
  min-height: calc(100vh - 260px);
  min-width: 0;
}

.node-canvas {
  position: absolute;
  inset: 0;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 56px 18px 36px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background-color: #fbfcfe;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.16) 1px, transparent 1px);
  background-size: var(--canvas-grid-size, 24px) var(--canvas-grid-size, 24px);
  cursor: grab;
  text-align: center;
}

.node-canvas.canvas-dragging {
  cursor: grabbing;
}

.node-canvas-scale {
  display: inline-block;
  min-width: 960px;
  min-height: calc(100vh - 360px);
  padding: 0 160px 120px;
  --workflow-line-color: #a8b3c7;
  --workflow-line-width: 2px;
  --workflow-branch-bg: #f6f8f9;
  transform-origin: top center;
  transition: transform 0.16s ease-out;
}

.canvas-tools {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 20;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.12);
  color: var(--el-text-color-secondary);
  font-weight: 700;
}

.end-node {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 72px;
  height: 30px;
  margin-top: 4px;
  border-radius: 999px;
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-regular);
  font-size: 13px;
}
</style>
