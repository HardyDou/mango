<template>
  <div class="mango-grid-designer">
    <aside class="mango-grid-designer__library">
      <div class="mango-grid-designer__library-header">
        <div>
          <div class="mango-grid-designer__library-title">组件库</div>
          <div class="mango-grid-designer__library-subtitle">搜索后拖入或点击添加</div>
        </div>
      </div>
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索组件"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-tabs
        v-model="activeCategory"
        class="mango-grid-designer__library-tabs"
      >
        <el-tab-pane
          v-for="tab in categoryTabs"
          :key="tab.value"
          :label="tab.label"
          :name="tab.value"
        />
      </el-tabs>
      <div class="mango-grid-designer__library-list">
        <button
          v-for="widget in filteredWidgets"
          :key="widget.type"
          class="mango-grid-designer__library-item"
          :disabled="widget.disabled"
          draggable="true"
          type="button"
          @click="addWidget(widget)"
          @dragstart="handleWidgetDragStart($event, widget)"
          @dragend="handleWidgetDragEnd"
        >
          <span class="mango-grid-designer__library-icon">
            <el-icon v-if="widget.icon">
              <component :is="widget.icon" />
            </el-icon>
          </span>
          <span class="mango-grid-designer__library-info">
            <span class="mango-grid-designer__library-name">{{ widget.title }}</span>
            <span class="mango-grid-designer__library-desc">{{ widget.description || '可添加到当前布局' }}</span>
          </span>
        </button>
      </div>
    </aside>

    <section class="mango-grid-designer__canvas">
      <div
        class="mango-grid-designer__grid"
        :class="{ 'is-dragging-widget': Boolean(draggingWidget) }"
        :style="gridStyle"
        @dragover.prevent="handleCanvasDragOver"
        @dragleave="handleCanvasDragLeave"
        @drop.prevent="handleCanvasDrop"
      >
        <div
          v-for="item in localItems"
          :key="item.id"
          class="mango-grid-designer__item"
          :class="{ 'is-active': activeId === item.id, 'is-moving': draggingItemId === item.id }"
          :style="itemStyle(item)"
          @mousedown.left="startMove($event, item)"
        >
          <div :class="cardClass(item)">
            <div class="mango-grid-designer__drag-mask" />
            <div
              v-if="shouldShowTitle(item)"
              class="mango-grid-designer__card-title"
            >
              <span class="mango-grid-designer__drag-dots" />
              <span>{{ item.title || widgetMap[item.widgetType]?.title || item.widgetType }}</span>
              <button
                class="mango-grid-designer__delete"
                type="button"
                title="删除组件"
                @mousedown.stop
                @click.stop="removeItem(item.id)"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>
            <button
              v-else
              class="mango-grid-designer__delete mango-grid-designer__delete--floating"
              type="button"
              title="删除组件"
              @mousedown.stop
              @click.stop="removeItem(item.id)"
            >
              <el-icon><Close /></el-icon>
            </button>
            <div class="mango-grid-designer__card-body">
              <slot
                name="widget"
                :item="item"
                :widget="widgetMap[item.widgetType]"
              >
                <component
                  :is="widgetMap[item.widgetType]?.component"
                  v-if="widgetMap[item.widgetType]?.component"
                  v-bind="widgetProps(item)"
                  :item="item"
                />
                <el-empty
                  v-else
                  description="组件不存在"
                  :image-size="72"
                />
              </slot>
            </div>
            <button
              class="mango-grid-designer__resize mango-grid-designer__resize--left"
              type="button"
              title="拖拽调整宽度"
              @mousedown.stop.prevent="startResize($event, item, 'width-left')"
            >
              <span />
            </button>
            <button
              class="mango-grid-designer__resize mango-grid-designer__resize--right"
              type="button"
              title="拖拽调整宽度"
              @mousedown.stop.prevent="startResize($event, item, 'width-right')"
            >
              <span />
            </button>
            <button
              class="mango-grid-designer__resize mango-grid-designer__resize--bottom"
              type="button"
              title="拖拽调整高度"
              @mousedown.stop.prevent="startResize($event, item, 'height')"
            >
              <span />
            </button>
            <span class="mango-grid-designer__size mango-grid-designer__size--corner">{{ sizeText(item) }}</span>
          </div>
        </div>

        <div
          v-if="placeholder"
          class="mango-grid-designer__placeholder"
          :style="placeholderStyle"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts" name="MangoGridDesigner">
import { Close, Search } from '@element-plus/icons-vue';
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CSSProperties } from 'vue';
import {
  createGridItemFromWidget,
  getGridRows,
  moveGridItem,
  normalizeRect,
  normalizeLayoutItems,
  resizeGridItem,
  removeGridItem,
} from '../composables/useGridEngine';
import type { GridLayoutItem, GridLayoutRect, GridWidgetDefinition } from '../types';

// 宽度左右两侧的拖拽语义不同：右侧只改宽度，左侧需要同时改 x 和宽度。
type ResizeMode = 'width-left' | 'width-right' | 'height';

interface MoveState {
  id: string;
  startX: number;
  startY: number;
  origin: GridLayoutRect;
  moved: boolean;
  previewX: number;
  previewY: number;
  target: GridLayoutRect;
}

const props = withDefaults(defineProps<{
  modelValue: GridLayoutItem[];
  widgets: GridWidgetDefinition[];
  columns?: number;
  rowHeight?: number;
  gap?: number;
  defaultWidth?: number;
  defaultHeight?: number;
  minRows?: number;
}>(), {
  columns: 12,
  rowHeight: 15,
  gap: 15,
  defaultWidth: 3,
  defaultHeight: 10,
  minRows: 30,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: GridLayoutItem[]): void;
  (event: 'change', value: GridLayoutItem[]): void;
}>();

const keyword = ref('');
const activeCategory = ref('全部');
const localItems = ref<GridLayoutItem[]>([]);
const activeId = ref('');
const draggingItemId = ref('');
const draggingWidget = ref<GridWidgetDefinition | null>(null);
const placeholder = ref<GridLayoutRect | null>(null);
const canvasRect = ref<DOMRect | null>(null);
const verticalGapRows = computed(() => Math.max(0, Math.round(props.gap / props.rowHeight)));
// 当前正在拖拽的卡片只做视觉预览，最终布局在松手时一次性提交。
const moveState = ref<MoveState | null>(null);

const options = computed(() => ({
  columns: props.columns,
  rowHeight: props.rowHeight,
  gap: props.gap,
  defaultWidth: props.defaultWidth,
  defaultHeight: props.defaultHeight,
  compact: false,
  verticalGapRows: verticalGapRows.value,
}));

let resizeState: {
  id: string;
  mode: ResizeMode;
  startX: number;
  startY: number;
  origin: GridLayoutRect;
  originItems: GridLayoutItem[];
} | null = null;
let pendingMovePoint: { clientX: number; clientY: number } | null = null;
let moveFrame = 0;

watch(
  () => props.modelValue,
  (items) => {
    localItems.value = normalizeLayoutItems(items || [], options.value);
  },
  { immediate: true, deep: true },
);

const widgetMap = computed<Record<string, GridWidgetDefinition>>(() => {
  return props.widgets.reduce<Record<string, GridWidgetDefinition>>((result, widget) => {
    result[widget.type] = widget;
    return result;
  }, {});
});

const categoryTabs = computed(() => {
  const categories = Array.from(new Set(props.widgets.map(widget => widget.category || '业务')));
  return ['全部', ...categories].map(value => ({ value, label: value }));
});

const filteredWidgets = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return props.widgets.filter((widget) => {
    const category = widget.category || '业务';
    if (activeCategory.value !== '全部' && category !== activeCategory.value) {
      return false;
    }
    if (!value) {
      return true;
    }
    const fields = [widget.title, widget.description, widget.category, widget.type, ...(widget.tags || [])];
    return fields.some(field => String(field || '').toLowerCase().includes(value));
  });
});

const totalRows = computed(() => {
  const rows = placeholder.value ? [...localItems.value, placeholderItem()] : localItems.value;
  return Math.max(getGridRows(rows, props.minRows), props.minRows);
});

const gridStyle = computed<CSSProperties>(() => ({
  '--mango-grid-columns': String(props.columns),
  '--mango-grid-row-height': `${props.rowHeight}px`,
  '--mango-grid-gap': `${props.gap}px`,
  '--mango-grid-vertical-gap-rows': String(verticalGapRows.value),
  gridTemplateRows: `repeat(${totalRows.value}, var(--mango-grid-row-height))`,
}));

const placeholderStyle = computed<CSSProperties>(() => placeholder.value ? rectStyle(placeholder.value) : {});

function addWidget(widget: GridWidgetDefinition, rect?: Partial<GridLayoutRect>): void {
  if (widget.disabled) {
    return;
  }
  const nextItem = createGridItemFromWidget(widget, localItems.value, rect, options.value);
  updateItems([...localItems.value, nextItem]);
  activeId.value = nextItem.id;
  placeholder.value = null;
}

function handleWidgetDragStart(event: DragEvent, widget: GridWidgetDefinition): void {
  if (widget.disabled) {
    event.preventDefault();
    return;
  }
  draggingWidget.value = widget;
  event.dataTransfer?.setData('text/plain', widget.type);
  event.dataTransfer?.setDragImage(createDragPreview(widget.title), 24, 24);
}

function handleCanvasDragOver(event: DragEvent): void {
  if (!draggingWidget.value) {
    return;
  }
  placeholder.value = pointerToRect(event.clientX, event.clientY, draggingWidget.value);
}

function handleCanvasDrop(): void {
  if (!draggingWidget.value) {
    return;
  }
  addWidget(draggingWidget.value, placeholder.value || undefined);
  clearWidgetDragState();
}

function handleWidgetDragEnd(): void {
  clearWidgetDragState();
}

function handleCanvasDragLeave(event: DragEvent): void {
  const current = event.currentTarget as HTMLElement;
  const related = event.relatedTarget;
  if (related instanceof Node && current.contains(related)) {
    return;
  }
  placeholder.value = null;
}

function clearWidgetDragState(): void {
  draggingWidget.value = null;
  placeholder.value = null;
}

function startMove(event: MouseEvent, item: GridLayoutItem): void {
  if (item.locked || isInteractiveTarget(event.target)) {
    return;
  }
  activeId.value = item.id;
  canvasRect.value = (event.currentTarget as HTMLElement).closest('.mango-grid-designer__grid')?.getBoundingClientRect() || null;
  moveState.value = {
    id: item.id,
    startX: event.clientX,
    startY: event.clientY,
    origin: { ...item.layout },
    moved: false,
    previewX: 0,
    previewY: 0,
    target: { ...item.layout },
  };
  window.addEventListener('mousemove', handleMove);
  window.addEventListener('mouseup', stopMove);
}

function handleMove(event: MouseEvent): void {
  if (!moveState.value) {
    return;
  }
  pendingMovePoint = {
    clientX: event.clientX,
    clientY: event.clientY,
  };
  if (!moveFrame) {
    moveFrame = window.requestAnimationFrame(applyMovePreview);
  }
}

function applyMovePreview(): void {
  moveFrame = 0;
  const state = moveState.value;
  const point = pendingMovePoint;
  if (!state || !point) {
    return;
  }
  const deltaX = point.clientX - state.startX;
  const deltaY = point.clientY - state.startY;
  if (!state.moved && Math.abs(deltaX) + Math.abs(deltaY) < 6) {
    return;
  }
  // 鼠标移动过程中只更新占位和当前卡片 transform，避免频繁重排导致闪烁。
  const desiredRect = normalizeRect({
    ...state.origin,
    x: state.origin.x + Math.round(deltaX / getUnitWidth()),
    y: state.origin.y + Math.round(deltaY / rowUnitHeight()),
  }, props.columns);
  draggingItemId.value = state.id;
  moveState.value = {
    ...state,
    moved: true,
    previewX: deltaX,
    previewY: deltaY,
    target: desiredRect,
  };
  placeholder.value = { ...desiredRect };
}

function stopMove(): void {
  if (moveFrame) {
    window.cancelAnimationFrame(moveFrame);
    moveFrame = 0;
    applyMovePreview();
  }
  const state = moveState.value;
  // 先算最终布局，再清空拖拽 transform，避免新网格位置叠加旧位移造成瞬间错位。
  const nextItems = state?.moved
    ? moveGridItem(localItems.value, state.id, state.target, props.columns, verticalGapRows.value)
    : null;
  if (state?.moved) {
    activeId.value = state.id;
  }
  moveState.value = null;
  pendingMovePoint = null;
  draggingItemId.value = '';
  placeholder.value = null;
  window.removeEventListener('mousemove', handleMove);
  window.removeEventListener('mouseup', stopMove);
  if (nextItems) {
    updateItems(nextItems);
  }
}

function startResize(event: MouseEvent, item: GridLayoutItem, mode: ResizeMode): void {
  activeId.value = item.id;
  canvasRect.value = (event.currentTarget as HTMLElement).closest('.mango-grid-designer__grid')?.getBoundingClientRect() || null;
  resizeState = {
    id: item.id,
    mode,
    startX: event.clientX,
    startY: event.clientY,
    origin: { ...item.layout },
    originItems: localItems.value.map(cloneGridItem),
  };
  window.addEventListener('mousemove', handleResize);
  window.addEventListener('mouseup', stopResize);
}

function handleResize(event: MouseEvent): void {
  if (!resizeState) {
    return;
  }
  const deltaX = event.clientX - resizeState.startX;
  const deltaY = event.clientY - resizeState.startY;
  // 缩放始终基于开始缩放时的布局计算，避免连续 mousemove 基于已推开的布局反复漂移。
  const nextRect = getResizeRect(resizeState.origin, resizeState.mode, deltaX, deltaY);
  updateItems(resizeGridItem(resizeState.originItems, resizeState.id, nextRect, props.columns, verticalGapRows.value));
}

function stopResize(): void {
  resizeState = null;
  window.removeEventListener('mousemove', handleResize);
  window.removeEventListener('mouseup', stopResize);
}

function removeItem(itemId: string): void {
  updateItems(removeGridItem(localItems.value, itemId, props.columns, verticalGapRows.value));
  if (activeId.value === itemId) {
    activeId.value = '';
  }
}

function updateItems(items: GridLayoutItem[]): void {
  const nextItems = normalizeLayoutItems(items, options.value);
  localItems.value = nextItems;
  emit('update:modelValue', nextItems);
  emit('change', nextItems);
}

function itemStyle(item: GridLayoutItem): CSSProperties {
  const style = rectStyle(item.layout);
  const state = moveState.value;
  if (state?.id === item.id && state.moved) {
    style.transform = `translate3d(${state.previewX}px, ${state.previewY}px, 0)`;
    style.zIndex = 5;
  }
  return style;
}

function rectStyle(rect: GridLayoutRect): CSSProperties {
  return {
    gridColumn: `${rect.x + 1} / span ${rect.w}`,
    gridRow: `${rect.y + 1} / span ${rect.h}`,
  };
}

function pointerToRect(clientX: number, clientY: number, widget: GridWidgetDefinition): GridLayoutRect {
  const rect = getCanvasRect();
  const x = rect ? Math.floor((clientX - rect.left) / getUnitWidth()) : 0;
  const y = rect ? Math.floor((clientY - rect.top) / rowUnitHeight()) : 0;
  return {
    x,
    y,
    w: widget.defaultLayout?.w || props.defaultWidth,
    h: widget.defaultLayout?.h || props.defaultHeight,
    minW: widget.defaultLayout?.minW,
    minH: widget.defaultLayout?.minH,
    maxW: widget.defaultLayout?.maxW,
    maxH: widget.defaultLayout?.maxH,
  };
}

function getCanvasRect(): DOMRect | null {
  return canvasRect.value || document.querySelector('.mango-grid-designer__grid')?.getBoundingClientRect() || null;
}

function getUnitWidth(): number {
  const rect = getCanvasRect();
  if (!rect) {
    return 1;
  }
  return (rect.width - props.gap * (props.columns - 1)) / props.columns + props.gap;
}

function rowUnitHeight(): number {
  return props.rowHeight;
}

function getResizeRect(origin: GridLayoutRect, mode: ResizeMode, deltaX: number, deltaY: number): Partial<GridLayoutRect> {
  if (mode === 'height') {
    return {
      h: origin.h + Math.round(deltaY / rowUnitHeight()),
    };
  }
  if (mode === 'width-right') {
    return {
      w: origin.w + Math.round(deltaX / getUnitWidth()),
    };
  }
  // 左侧拖拽时保持右边界不动，只移动左边界，并受最小/最大宽度与栅格边界约束。
  const deltaColumns = Math.round(deltaX / getUnitWidth());
  const rightEdge = origin.x + origin.w;
  const minW = Math.max(1, origin.minW || 1);
  const maxW = Math.min(props.columns, origin.maxW || props.columns);
  const minX = Math.max(0, rightEdge - maxW);
  const maxX = Math.max(minX, rightEdge - minW);
  const nextX = clampNumber(origin.x + deltaColumns, minX, maxX);
  return {
    x: nextX,
    w: rightEdge - nextX,
  };
}

function sizeText(item: GridLayoutItem): string {
  return `${item.layout.w}*${item.layout.h}`;
}

function shouldShowTitle(item: GridLayoutItem): boolean {
  const widget = widgetMap.value[item.widgetType];
  return item.showTitle ?? widget?.showTitle ?? true;
}

function widgetProps(item: GridLayoutItem): Record<string, unknown> {
  const widget = widgetMap.value[item.widgetType];
  return {
    ...(widget?.defaultProps || {}),
    ...(item.props || {}),
  };
}

function cardClass(item: GridLayoutItem) {
  const widget = widgetMap.value[item.widgetType];
  return [
    'mango-grid-designer__card',
    {
      'is-no-padding': (item.padding ?? widget?.padding ?? true) === false,
      'is-no-title': !shouldShowTitle(item),
    },
  ];
}

function placeholderItem(): GridLayoutItem {
  return {
    id: '__placeholder__',
    widgetType: '__placeholder__',
    layout: placeholder.value || { x: 0, y: 0, w: 1, h: 1 },
  };
}

function isInteractiveTarget(target: EventTarget | null): boolean {
  return target instanceof HTMLElement && Boolean(target.closest('button,a,input,textarea,select,.el-button,.el-input'));
}

function createDragPreview(title: string): HTMLElement {
  const element = document.createElement('div');
  element.className = 'mango-grid-designer__drag-preview';
  element.textContent = title;
  document.body.appendChild(element);
  window.setTimeout(() => element.remove(), 0);
  return element;
}

function cloneGridItem(item: GridLayoutItem): GridLayoutItem {
  return {
    ...item,
    layout: { ...item.layout },
    props: item.props ? { ...item.props } : undefined,
  };
}

function clampNumber(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

onBeforeUnmount(() => {
  stopMove();
  stopResize();
  clearWidgetDragState();
});
</script>
