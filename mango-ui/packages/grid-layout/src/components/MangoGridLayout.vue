<template>
  <div
    class="mango-grid-layout"
    :style="gridStyle"
  >
    <div
      v-for="item in normalizedItems"
      :key="item.id"
      class="mango-grid-layout__item"
      :style="itemStyle(item)"
    >
      <div :class="cardClass(item)">
        <div
          v-if="shouldShowTitle(item)"
          class="mango-grid-layout__card-title"
        >
          <span>{{ item.title || widgetMap[item.widgetType]?.title || item.widgetType }}</span>
        </div>
        <div class="mango-grid-layout__card-body">
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="MangoGridLayout">
import { computed } from 'vue';
import type { CSSProperties } from 'vue';
import { normalizeLayoutItems } from '../composables/useGridEngine';
import type { GridLayoutItem, GridWidgetDefinition } from '../types';

const props = withDefaults(defineProps<{
  items: GridLayoutItem[];
  widgets: GridWidgetDefinition[];
  columns?: number;
  rowHeight?: number;
  gap?: number;
  minRows?: number;
}>(), {
  columns: 12,
  rowHeight: 15,
  gap: 15,
  minRows: 30,
});

const verticalGapRows = computed(() => Math.max(0, Math.round(props.gap / props.rowHeight)));

const normalizedItems = computed(() => normalizeLayoutItems(props.items, {
  columns: props.columns,
  rowHeight: props.rowHeight,
  gap: props.gap,
  defaultWidth: 3,
  defaultHeight: 10,
  compact: false,
  verticalGapRows: verticalGapRows.value,
}));

const widgetMap = computed<Record<string, GridWidgetDefinition>>(() => {
  return props.widgets.reduce<Record<string, GridWidgetDefinition>>((result, widget) => {
    result[widget.type] = widget;
    return result;
  }, {});
});

const totalRows = computed(() => {
  const maxRows = normalizedItems.value.reduce((rows, item) => Math.max(rows, item.layout.y + item.layout.h), props.minRows);
  return Math.max(maxRows, props.minRows);
});

const gridStyle = computed<CSSProperties>(() => ({
  '--mango-grid-columns': String(props.columns),
  '--mango-grid-row-height': `${props.rowHeight}px`,
  '--mango-grid-gap': `${props.gap}px`,
  '--mango-grid-vertical-gap-rows': String(verticalGapRows.value),
  gridTemplateRows: `repeat(${totalRows.value}, var(--mango-grid-row-height))`,
}));

function itemStyle(item: GridLayoutItem): CSSProperties {
  return {
    gridColumn: `${item.layout.x + 1} / span ${item.layout.w}`,
    gridRow: `${item.layout.y + 1} / span ${item.layout.h}`,
  };
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
    'mango-grid-layout__card',
    {
      'is-no-padding': (item.padding ?? widget?.padding ?? true) === false,
      'is-no-title': !shouldShowTitle(item),
    },
  ];
}
</script>
