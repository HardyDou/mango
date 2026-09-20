<template>
  <div
    v-if="normalizedGroups.length"
    class="mango-description-list"
    :class="`is-${display}`"
    :style="{
      '--mango-description-columns': normalizedColumn,
      '--mango-description-label-width': cssLabelWidth,
    }"
    data-surface="detail.description-list"
  >
    <section
      v-for="(group, groupIndex) in normalizedGroups"
      :key="group.key"
      class="mango-description-list__group"
      :class="{ 'has-header': group.header }"
    >
      <div v-if="group.header" class="mango-description-list__group-header">
        <div class="mango-description-list__group-header-copy">
          <strong class="mango-description-list__group-title">{{ group.header.title }}</strong>
          <span v-if="group.header.description" class="mango-description-list__group-description">
            {{ group.header.description }}
          </span>
          <slot
            v-if="group.header.extraSlot && group.header.extraPlacement === 'after-description'"
            :name="`group-extra-${group.header.extraSlot}`"
            :group="group"
            :group-index="groupIndex"
            :header="group.header"
            :value="group.header.extra"
            :display-value="displayGroupHeaderExtra(group.header.extra)"
          >
            {{ displayGroupHeaderExtra(group.header.extra) }}
          </slot>
        </div>
        <div
          v-if="
            (group.header.extraSlot && group.header.extraPlacement !== 'after-description') ||
            (!group.header.extraSlot && hasValue(group.header.extra))
          "
          class="mango-description-list__group-extra"
        >
          <slot
            v-if="group.header.extraSlot"
            :name="`group-extra-${group.header.extraSlot}`"
            :group="group"
            :group-index="groupIndex"
            :header="group.header"
            :value="group.header.extra"
            :display-value="displayGroupHeaderExtra(group.header.extra)"
          >
            {{ displayGroupHeaderExtra(group.header.extra) }}
          </slot>
          <template v-else>{{ displayGroupHeaderExtra(group.header.extra) }}</template>
        </div>
      </div>
      <el-descriptions
        class="mango-description-list__descriptions"
        :column="normalizedColumn"
        :border="display === 'table'"
        :label-width="labelWidth"
      >
        <el-descriptions-item
          v-for="(item, index) in group.items"
          :key="item.key"
          :label="descriptionLabel(item)"
          :span="normalizeSpan(item.span)"
        >
          <slot
            v-if="item.slot"
            :name="item.slot"
            :item="item"
            :index="index"
            :group="group"
            :group-index="groupIndex"
            :value="item.value"
            :display-value="displayValue(item)"
          >
            {{ displayValue(item) }}
          </slot>
          <component
            :is="richTextPreviewComponent"
            v-else-if="item.componentType === 'rich-text-preview' && hasValue(item.value)"
            :content="String(item.value)"
          />
          <template v-else>{{ displayValue(item) }}</template>
        </el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
  <el-empty v-else :description="emptyDescription" :image-size="64" data-surface="detail.description-list.empty" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import RichTextViewer from '../RichTextViewer/index.vue';
import type {
  MangoDescriptionGroup,
  MangoDescriptionGroupExtraSlotProps,
  MangoDescriptionItem,
  MangoDescriptionItemSlotProps,
  MangoDescriptionListProps,
  MangoDescriptionValue,
} from './types';

defineOptions({ name: 'MangoDescriptionList' });
defineSlots<{
  [name: string]: (props: MangoDescriptionItemSlotProps | MangoDescriptionGroupExtraSlotProps) => unknown;
}>();

const props = withDefaults(defineProps<MangoDescriptionListProps>(), {
  display: 'key-value',
  column: 2,
  labelWidth: 150,
  emptyText: '-',
  emptyDescription: '暂无信息',
  richTextPreviewComponent: () => RichTextViewer,
});

const normalizedGroups = computed<MangoDescriptionGroup[]>(() => {
  if (props.groups && props.items) throw new Error('MangoDescriptionList: items 与 groups 不能同时配置');
  const groups = props.groups
    ? props.groups.filter((group) => group.items.length)
    : props.items?.length
      ? [{ key: 'default', items: props.items }]
      : [];
  for (const group of groups) {
    for (const item of group.items) {
      if (item.slot && item.componentType) {
        throw new Error(`MangoDescriptionList: 描述项 ${item.key} 的 slot 与 componentType 不能同时配置`);
      }
    }
  }
  return groups;
});
const normalizedColumn = computed(() => normalizePositiveInteger(props.column, 2));
const cssLabelWidth = computed(() =>
  typeof props.labelWidth === 'number' ? `${props.labelWidth}px` : props.labelWidth,
);

function normalizePositiveInteger(value: number | undefined, fallback: number) {
  return Number.isFinite(value) && Number(value) >= 1 ? Math.floor(Number(value)) : fallback;
}

function normalizeSpan(span?: number) {
  return Math.min(normalizePositiveInteger(span, 1), normalizedColumn.value);
}

function descriptionLabel(item: MangoDescriptionItem) {
  return props.display === 'key-value' ? `${item.label}：` : item.label;
}

function displayValue(item: MangoDescriptionItem) {
  return hasValue(item.value) ? String(item.value) : (item.emptyText ?? props.emptyText);
}

function displayGroupHeaderExtra(value: MangoDescriptionValue) {
  return hasValue(value) ? String(value) : props.emptyText;
}

function hasValue(value: MangoDescriptionValue) {
  return value !== null && value !== undefined && value !== '';
}
</script>

<style scoped>
.mango-description-list {
  width: 100%;
}

.mango-description-list__group.has-header + .mango-description-list__group.has-header {
  margin-top: 14px;
}

.mango-description-list__group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding-bottom: 8px;
}

.mango-description-list__group-header-copy {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 10px;
}

.mango-description-list__group-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.mango-description-list__group-description {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 20px;
  overflow-wrap: anywhere;
}

.mango-description-list__group-extra {
  flex: none;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.mango-description-list :deep(.el-descriptions__label) {
  width: var(--mango-description-label-width);
  min-width: var(--mango-description-label-width);
  color: var(--el-text-color-secondary);
  font-weight: 500;
  text-align: right;
}

.mango-description-list :deep(.el-descriptions__content) {
  min-width: 0;
  color: var(--el-text-color-primary);
  overflow-wrap: anywhere;
}

.mango-description-list.is-key-value :deep(.el-descriptions__cell) {
  padding-bottom: 12px;
  vertical-align: top;
}

.mango-description-list.is-key-value :deep(.el-descriptions__table) {
  width: 100%;
  table-layout: fixed;
}

.mango-description-list.is-key-value :deep(.el-descriptions__cell:not([colspan])) {
  width: calc(100% / var(--mango-description-columns));
}

.mango-description-list.is-key-value :deep(.el-descriptions__label) {
  display: inline-block;
  margin-right: 10px;
  vertical-align: top;
}

.mango-description-list.is-key-value :deep(.el-descriptions__content) {
  display: inline-block;
  width: calc(100% - var(--mango-description-label-width) - 10px);
  box-sizing: border-box;
  vertical-align: top;
  white-space: normal;
  overflow-wrap: anywhere;
}

.mango-description-list.is-table :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  background: var(--el-fill-color-extra-light);
}

@media (width <= 760px) {
  .mango-description-list__group-header {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .mango-description-list :deep(.el-descriptions__body),
  .mango-description-list :deep(.el-descriptions__table),
  .mango-description-list :deep(.el-descriptions__table tbody) {
    display: block;
    width: 100%;
  }

  .mango-description-list.is-key-value :deep(.el-descriptions__table tr) {
    display: block;
  }

  .mango-description-list.is-key-value :deep(.el-descriptions__cell) {
    display: grid;
    width: 100% !important;
    grid-template-columns: minmax(96px, min(var(--mango-description-label-width), 42%)) minmax(0, 1fr);
  }

  .mango-description-list.is-table :deep(.el-descriptions__table tr) {
    display: grid;
    grid-template-columns: minmax(96px, min(var(--mango-description-label-width), 42%)) minmax(0, 1fr);
  }

  .mango-description-list.is-table :deep(.el-descriptions__cell) {
    width: auto !important;
  }

  .mango-description-list.is-key-value :deep(.el-descriptions__label),
  .mango-description-list.is-key-value :deep(.el-descriptions__content) {
    display: block !important;
    width: auto !important;
    min-width: 0 !important;
  }

  .mango-description-list.is-key-value :deep(.el-descriptions__label) {
    margin-right: 10px;
  }
}
</style>
