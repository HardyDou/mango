<template>
  <MangoSideDrawerShell
    ref="drawerRef"
    v-model="workflowDrawerVisible"
    :title="workflowTitle"
    :show-trigger="showWorkflow && showWorkflowTrigger"
    :drawer-size="workflowDrawerSize"
    data-surface="detail.workflow.drawer"
    data-action="detail.workflow.open"
  >
    <template #main>
      <div ref="pageRootRef" class="mango-collapse-detail-page" :class="{ 'is-with-actions': hasVisibleActions }">
        <MangoPageBackBar
          v-if="showBackBar"
          :title="title"
          :back-label="backLabel"
          :back-to="backTo"
          :navigate-on-back="navigateOnBack"
          :show-refresh="showRefresh"
          :refresh-loading="refreshLoading"
          @back="emit('back')"
          @refresh="emit('refresh')"
        >
          <template v-if="$slots.toolbar" #extra><slot name="toolbar" /></template>
        </MangoPageBackBar>

        <section
          class="mango-collapse-detail-page__content"
          :class="{ 'is-shadowless': !showContentShadow }"
          :data-page="dataPage"
          :data-state="pageState"
        >
          <div v-if="summary || $slots.summary" class="mango-collapse-detail-page__summary">
            <MangoDetailSummary v-if="summary" v-bind="summary" />
            <slot name="summary" />
          </div>

          <section class="mango-collapse-detail-page__body" :aria-busy="loading">
            <div v-if="loading" v-loading="true" class="mango-collapse-detail-page__loading" role="status" />
            <el-result v-else-if="errorText" icon="error" title="详情加载失败" :sub-title="errorText">
              <template #extra>
                <el-button type="primary" data-action="detail.retry" @click="emit('retry')">重试</el-button>
              </template>
            </el-result>
            <el-tabs
              v-else-if="renderTabs.length"
              v-model="activeTabName"
              class="mango-collapse-detail-page__tabs"
              :class="{ 'is-implicit': !hasExplicitTabs }"
              data-surface="detail.collapse.tabs"
            >
              <el-tab-pane
                v-for="(tab, tabIndex) in renderTabs"
                :key="tab.name"
                :name="tab.name"
                :label="tab.label"
                :disabled="tab.disabled"
                :lazy="tab.lazy ?? true"
              >
                <el-collapse
                  v-if="tab.panels?.length"
                  v-model="activePanelNames"
                  class="mango-collapse-detail-page__collapse"
                  :data-surface="hasExplicitTabs ? `detail.tab.${tab.name}.panels` : 'detail.panels'"
                  @change="handleChange"
                >
                  <el-row class="mango-collapse-detail-page__row" :gutter="responsive?.gutter ?? 0">
                    <el-col
                      v-for="(panel, panelIndex) in tab.panels"
                      :key="panel.name"
                      :span="responsive?.span ?? 24"
                      :xs="responsive?.xs"
                      :sm="responsive?.sm"
                      :md="responsive?.md"
                      :lg="responsive?.lg"
                      :xl="responsive?.xl"
                    >
                      <el-collapse-item
                        :name="panel.name"
                        :disabled="panel.disabled"
                        :data-surface="panel.dataSurface || `detail.panel.${panel.name}`"
                      >
                        <template #title>
                          <div class="mango-collapse-detail-page__panel-header">
                            <strong class="mango-collapse-detail-page__panel-heading">{{ panel.title }}</strong>
                            <span
                              v-if="panel.headerActions?.length"
                              class="mango-collapse-detail-page__panel-actions"
                              @click.stop
                              @keydown.stop
                            >
                              <el-tooltip
                                v-for="action in panel.headerActions"
                                :key="action.name"
                                :content="action.label"
                                placement="top"
                              >
                                <el-button
                                  link
                                  type="primary"
                                  size="small"
                                  :icon="action.icon"
                                  :disabled="action.disabled"
                                  :loading="action.loading"
                                  :data-action="action.dataAction || action.name"
                                  :data-stage="action.dataStage"
                                  :aria-label="action.label"
                                  @click.stop="emitPanelAction(action, panel, panelIndex, tab, tabIndex)"
                                >
                                  {{ action.label }}
                                </el-button>
                              </el-tooltip>
                            </span>
                          </div>
                        </template>

                        <div class="mango-collapse-detail-page__panel-body">
                          <component
                            :is="resolvePanelComponent(panel)"
                            v-if="resolvePanelComponent(panel)"
                            v-bind="resolvePanelComponentProps(panel, panelIndex, tab, tabIndex)"
                          >
                            <template
                              v-for="item in panelDescriptionGroupExtraSlotItems(panel)"
                              :key="item.key"
                              #[item.slot]="slotProps"
                            >
                              <slot
                                :name="item.externalSlot"
                                v-bind="slotProps"
                                :panel="panel"
                                :panel-index="panelIndex"
                                :expanded="isExpanded(panel.name)"
                                :tab="publicTab(tab)"
                                :tab-index="publicTabIndex(tabIndex)"
                                >{{ slotProps.displayValue }}</slot
                              >
                            </template>
                            <template
                              v-for="item in panelDescriptionSlotItems(panel)"
                              :key="item.key"
                              #[item.slot]="slotProps"
                            >
                              <slot
                                :name="item.externalSlot"
                                v-bind="slotProps"
                                :panel="panel"
                                :panel-index="panelIndex"
                                :expanded="isExpanded(panel.name)"
                                :tab="publicTab(tab)"
                                :tab-index="publicTabIndex(tabIndex)"
                                >{{ slotProps.displayValue }}</slot
                              >
                            </template>
                            <template
                              v-for="item in panelFileListCellSlotItems(panel)"
                              :key="item.key"
                              #[item.slot]="slotProps"
                            >
                              <slot
                                :name="item.externalSlot"
                                v-bind="slotProps"
                                :panel="panel"
                                :panel-index="panelIndex"
                                :expanded="isExpanded(panel.name)"
                                :tab="publicTab(tab)"
                                :tab-index="publicTabIndex(tabIndex)"
                                >{{ slotProps.displayValue }}</slot
                              >
                            </template>
                            <template
                              v-for="item in panelFileListActionsSlotItems(panel)"
                              :key="item.key"
                              #[item.slot]="slotProps"
                            >
                              <slot
                                :name="item.externalSlot"
                                v-bind="slotProps"
                                :panel="panel"
                                :panel-index="panelIndex"
                                :expanded="isExpanded(panel.name)"
                                :tab="publicTab(tab)"
                                :tab-index="publicTabIndex(tabIndex)"
                              />
                            </template>
                            <template
                              v-for="item in panelListTableCellSlotItems(panel)"
                              :key="item.key"
                              #[item.slot]="slotProps"
                            >
                              <slot
                                :name="item.externalSlot"
                                v-bind="slotProps"
                                :panel="panel"
                                :panel-index="panelIndex"
                                :expanded="isExpanded(panel.name)"
                                :tab="publicTab(tab)"
                                :tab-index="publicTabIndex(tabIndex)"
                                >{{ slotProps.value ?? '-' }}</slot
                              >
                            </template>
                          </component>
                          <template v-else>
                            <slot
                              :name="panelContentSlotName(panel)"
                              :panel="panel"
                              :index="panelIndex"
                              :expanded="isExpanded(panel.name)"
                              :tab="publicTab(tab)"
                              :tab-index="publicTabIndex(tabIndex)"
                            >
                              <slot
                                name="panel"
                                :panel="panel"
                                :index="panelIndex"
                                :expanded="isExpanded(panel.name)"
                                :tab="publicTab(tab)"
                                :tab-index="publicTabIndex(tabIndex)"
                              >
                                <el-empty :description="panel.emptyDescription || '暂无信息'" :image-size="72" />
                              </slot>
                            </slot>
                          </template>
                        </div>
                      </el-collapse-item>
                    </el-col>
                  </el-row>
                </el-collapse>
                <el-empty
                  v-else-if="tab.panels"
                  :description="tab.emptyDescription || '暂无详情信息'"
                  data-surface="detail.tab.empty"
                />
                <slot
                  v-else
                  :name="tabContentSlotName(tab)"
                  :tab="tab"
                  :index="tabIndex"
                  :active="activeTabName === tab.name"
                >
                  <slot name="tab" :tab="tab" :index="tabIndex" :active="activeTabName === tab.name">
                    <el-empty :description="tab.emptyDescription || '暂无详情信息'" data-surface="detail.tab.empty" />
                  </slot>
                </slot>
              </el-tab-pane>
            </el-tabs>
            <el-empty v-else :description="emptyDescription" data-surface="detail.empty" />
          </section>
        </section>

        <div
          v-if="hasVisibleActions"
          class="mango-collapse-detail-page__actions"
          :class="`is-align-${actionsAlign}`"
          data-surface="detail.actions"
          :data-actions-align="actionsAlign"
        >
          <slot name="actions" />
        </div>
      </div>

      <el-button
        v-if="showBackTop && backTopVisible"
        class="mango-collapse-detail-page__back-top"
        :class="{ 'is-with-workflow': showWorkflow && showWorkflowTrigger }"
        type="primary"
        plain
        :icon="ArrowUpBold"
        data-action="detail.back-to-top"
        aria-label="回到顶部"
        title="回到顶部"
        @click="scrollToPageTop"
      />
    </template>
    <slot v-if="showWorkflow" name="workflow" />
  </MangoSideDrawerShell>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useSlots, watch } from 'vue';
import { ArrowUpBold } from '@element-plus/icons-vue';
import {
  MangoDataTable,
  MangoDescriptionList,
  MangoDetailSummary,
  MangoPageBackBar,
  MangoSideDrawerShell,
  type MangoDescriptionItem,
  type MangoSideDrawerShellExpose,
} from '@mango/common';
import { MangoFileList, type MangoFileListRow } from '@mango/file';
import MangoRichTextPreview from './MangoRichTextPreview.vue';
import type {
  MangoCollapseDetailPageEmits,
  MangoCollapseDetailPageExpose,
  MangoCollapseDetailPageProps,
  MangoCollapsePanel,
  MangoCollapsePanelAction,
  MangoCollapsePanelActionEvent,
  MangoCollapseTab,
} from './MangoCollapseDetailPage.types';

const SLOT_SEGMENT_PATTERN = /^[A-Za-z][A-Za-z0-9-]*$/;
const IMPLICIT_TAB_NAME = '__mango-collapse-detail-default';

defineOptions({ name: 'MangoCollapseDetailPage' });

const props = withDefaults(defineProps<MangoCollapseDetailPageProps>(), {
  panels: undefined,
  tabs: undefined,
  responsive: undefined,
  activeTab: undefined,
  defaultActiveTab: undefined,
  autoExpandPanels: true,
  defaultActivePanelNames: undefined,
  backLabel: '返回',
  backTo: undefined,
  navigateOnBack: false,
  showBackBar: true,
  showRefresh: true,
  refreshLoading: false,
  showContentShadow: true,
  summary: undefined,
  showBackTop: true,
  showActions: true,
  actionsAlign: 'right',
  backTopThreshold: 300,
  showWorkflow: false,
  showWorkflowTrigger: true,
  workflowTitle: '节点过程',
  workflowDrawerSize: 'min(420px, 100vw)',
  dataPage: undefined,
  loading: false,
  errorText: '',
  emptyDescription: '暂无详情信息',
});
const emit = defineEmits<MangoCollapseDetailPageEmits>();
const slots = useSlots();
const drawerRef = ref<MangoSideDrawerShellExpose>();
const workflowDrawerVisible = ref(false);
const pageRootRef = ref<HTMLElement | null>(null);
const backTopVisible = ref(false);
let backTopScrollTarget: HTMLElement | Window | null = null;

const panelComponentRegistry = {
  'description-list': MangoDescriptionList,
  'file-list': MangoFileList,
  'list-table': MangoDataTable,
  'rich-text-preview': MangoRichTextPreview,
} as const;

const hasVisibleActions = computed(
  () => Boolean(slots.actions) && props.showActions && !props.loading && !props.errorText,
);
const pageState = computed(() => {
  if (props.loading) return 'loading';
  if (props.errorText) return 'error';
  return renderTabs.value.length ? 'ready' : 'empty';
});
const hasExplicitTabs = computed(() => props.tabs !== undefined);
const renderTabs = computed<MangoCollapseTab[]>(() => {
  if (props.tabs !== undefined && props.panels !== undefined) {
    throw new Error('MangoCollapseDetailPage: tabs 与 panels 不能同时配置');
  }
  if (props.tabs !== undefined) {
    validateTabStructure(props.tabs);
    return props.tabs;
  }
  if (props.panels !== undefined) {
    validatePanelNames(props.panels);
    return [{ name: IMPLICIT_TAB_NAME, label: '', lazy: false, panels: props.panels }];
  }
  return [];
});
const allPanels = computed(() => renderTabs.value.flatMap((tab) => tab.panels || []));
const internalActiveTabName = ref(props.defaultActiveTab || '');
const activeTabName = computed({
  get: () => normalizeActiveTabName(props.activeTab === undefined ? internalActiveTabName.value : props.activeTab),
  set: (value: string) => {
    const normalized = normalizeActiveTabName(value);
    if (props.activeTab === undefined) internalActiveTabName.value = normalized;
    if (hasExplicitTabs.value) {
      emit('update:activeTab', normalized);
      emit('tab-change', normalized);
    }
  },
});
const internalActiveNames = ref<string[]>([]);
const activePanelNames = computed({
  get: () => internalActiveNames.value,
  set: (value: string[]) => {
    internalActiveNames.value = normalizeActiveNames(value);
  },
});
let defaultsInitialized = false;

watch(
  () => allPanels.value.map((panel) => panel.name),
  (panelNames, previousPanelNames = []) => {
    validateDefaultPanelConfiguration(panelNames);
    const validNames = new Set(panelNames);
    if (props.autoExpandPanels) {
      const previousNames = new Set(previousPanelNames);
      const retained = internalActiveNames.value.filter((name) => validNames.has(name));
      const added = panelNames.filter((name) => !previousNames.has(name));
      internalActiveNames.value = [...new Set([...retained, ...added])];
      return;
    }
    if (!defaultsInitialized && (panelNames.length > 0 || !props.defaultActivePanelNames?.length)) {
      internalActiveNames.value = [...(props.defaultActivePanelNames || [])];
      defaultsInitialized = true;
      return;
    }
    internalActiveNames.value = internalActiveNames.value.filter((name) => validNames.has(name));
  },
  { immediate: true },
);

watch(
  () => props.showWorkflow,
  (visible) => {
    if (!visible) workflowDrawerVisible.value = false;
  },
);

function validateDefaultPanelConfiguration(panelNames: string[]) {
  const defaults = props.defaultActivePanelNames;
  if (props.autoExpandPanels && defaults?.length) {
    throw new Error('MangoCollapseDetailPage: autoExpandPanels 开启时不能配置 defaultActivePanelNames');
  }
  if (props.autoExpandPanels || !defaults) return;
  const normalized = defaults.map((name) => String(name ?? '').trim());
  if (normalized.some((name) => !name))
    throw new Error('MangoCollapseDetailPage: defaultActivePanelNames 不能包含空 Key');
  if (new Set(normalized).size !== normalized.length)
    throw new Error('MangoCollapseDetailPage: defaultActivePanelNames 不能包含重复 Key');
  if (panelNames.length && normalized.some((name) => !panelNames.includes(name))) {
    throw new Error('MangoCollapseDetailPage: defaultActivePanelNames 包含不存在的面板 Key');
  }
}

function normalizeActiveNames(value: unknown) {
  const validNames = new Set(allPanels.value.map((panel) => panel.name));
  return [
    ...new Set(
      (Array.isArray(value) ? value : [value])
        .map((item) => String(item ?? '').trim())
        .filter((name) => name && validNames.has(name)),
    ),
  ];
}
function normalizeActiveTabName(value: unknown) {
  const requested = renderTabs.value.find((tab) => tab.name === String(value ?? '').trim() && !tab.disabled);
  return requested?.name || renderTabs.value.find((tab) => !tab.disabled)?.name || renderTabs.value[0]?.name || '';
}
function validateTabStructure(tabs: MangoCollapseTab[]) {
  const names = new Set<string>();
  const panels = tabs.flatMap((tab) => {
    const name = String(tab.name || '').trim();
    if (!name) throw new Error('MangoCollapseDetailPage: Tab name 不能为空');
    if (names.has(name)) throw new Error(`MangoCollapseDetailPage: Tab name ${name} 重复`);
    names.add(name);
    return tab.panels || [];
  });
  validatePanelNames(panels);
}
function validatePanelNames(panels: MangoCollapsePanel[]) {
  const names = new Set<string>();
  for (const panel of panels) {
    const name = String(panel.name || '').trim();
    if (!name) throw new Error('MangoCollapseDetailPage: 面板 name 不能为空');
    if (names.has(name)) throw new Error(`MangoCollapseDetailPage: 面板 name ${name} 重复`);
    names.add(name);
  }
}

function handleChange(value: unknown) {
  emit('change', normalizeActiveNames(value));
}
function emitPanelAction(
  action: MangoCollapsePanelAction,
  panel: MangoCollapsePanel,
  panelIndex: number,
  tab: MangoCollapseTab,
  tabIndex: number,
) {
  const payload: MangoCollapsePanelActionEvent = {
    action,
    panel,
    panelIndex,
    tab: publicTab(tab),
    tabIndex: publicTabIndex(tabIndex),
  };
  emit('panel-action', payload);
}
function isExpanded(name: string) {
  return activePanelNames.value.includes(name);
}
function publicTab(tab: MangoCollapseTab) {
  return hasExplicitTabs.value ? tab : undefined;
}
function publicTabIndex(index: number) {
  return hasExplicitTabs.value ? index : undefined;
}
function panelContentSlotName(panel: MangoCollapsePanel): `panel-${string}` {
  return `panel-${validateSlotSegment(panel.name, '面板 name')}`;
}
function tabContentSlotName(tab: MangoCollapseTab): `tab-${string}` {
  return `tab-${validateSlotSegment(tab.name, 'Tab name')}`;
}

function panelDescriptionSlotItems(panel: MangoCollapsePanel) {
  if (panel.content?.componentType !== 'description-list') return [];
  const items = panel.content.data.groups
    ? panel.content.data.groups.flatMap((group) => group.items)
    : panel.content.data.items;
  const names = new Set<string>();
  return items.flatMap((item: MangoDescriptionItem, index: number) => {
    if (!item.slot) return [];
    const local = validateSlotSegment(item.slot, '描述项 slot');
    const externalSlot = descriptionItemSlotName(panel, local);
    if (names.has(externalSlot)) return [];
    names.add(externalSlot);
    return [{ key: `${item.key}-${index}`, slot: local, externalSlot }];
  });
}
function panelDescriptionGroupExtraSlotItems(panel: MangoCollapsePanel) {
  if (panel.content?.componentType !== 'description-list' || !panel.content.data.groups) return [];
  const names = new Set<string>();
  return panel.content.data.groups.flatMap((group, index) => {
    if (!group.header?.extraSlot) return [];
    const local = validateSlotSegment(group.header.extraSlot, '描述列表分组头 extraSlot');
    const externalSlot = descriptionGroupExtraSlotName(panel, local);
    if (names.has(externalSlot)) return [];
    names.add(externalSlot);
    return [{ key: `${group.key}-${index}`, slot: `group-extra-${local}`, externalSlot }];
  });
}
function descriptionItemSlotName(panel: MangoCollapsePanel, slot: string): `description-item-${string}__${string}` {
  return `description-item-${validateSlotSegment(panel.name, '面板 name')}__${slot}`;
}
function descriptionGroupExtraSlotName(
  panel: MangoCollapsePanel,
  slot: string,
): `description-group-extra-${string}__${string}` {
  return `description-group-extra-${validateSlotSegment(panel.name, '面板 name')}__${slot}`;
}
function panelFileListCellSlotItems(panel: MangoCollapsePanel) {
  if (panel.content?.componentType !== 'file-list') return [];
  return panel.content.data.columns
    .filter((column) => column.slot)
    .map((column, index) => {
      const slot = validateSlotSegment(column.slot || '', '文件列表单元格 slot');
      return {
        key: `${column.key}-${index}`,
        slot: `cell-${slot}`,
        externalSlot: `file-list-cell-${validateSlotSegment(panel.name, '面板 name')}__${slot}` as const,
      };
    });
}
function panelFileListActionsSlotItems(panel: MangoCollapsePanel) {
  if (panel.content?.componentType !== 'file-list') return [];
  return panel.content.data.columns
    .filter((column) => column.fileActionsSlot)
    .map((column, index) => {
      const slot = validateSlotSegment(column.fileActionsSlot || '', '文件列表文件操作 slot');
      return {
        key: `${column.key}-${index}`,
        slot: `file-actions-${slot}`,
        externalSlot: `file-list-file-actions-${validateSlotSegment(panel.name, '面板 name')}__${slot}` as const,
      };
    });
}
function panelListTableCellSlotItems(panel: MangoCollapsePanel) {
  if (panel.content?.componentType !== 'list-table') return [];
  const names = new Set<string>();
  return panel.content.data.columns.flatMap((column, index) => {
    if (!column.slot) return [];
    const slot = validateSlotSegment(column.slot, '列表表格单元格 slot');
    const externalSlot = `list-table-cell-${validateSlotSegment(panel.name, '面板 name')}__${slot}` as const;
    if (names.has(externalSlot))
      throw new Error(`MangoCollapseDetailPage: 面板 ${panel.name} 的列表表格 Slot ${slot} 重复`);
    names.add(externalSlot);
    return [{ key: `${column.field}-${index}`, slot, externalSlot }];
  });
}
function validateSlotSegment(value: string, label: string) {
  if (!SLOT_SEGMENT_PATTERN.test(value))
    throw new Error(`MangoCollapseDetailPage: ${label} 必须以英文字母开头，且只能包含英文字母、数字和短横线`);
  return value;
}

function resolvePanelComponent(panel: MangoCollapsePanel) {
  if (!panel.content || panel.content.componentType === 'custom') return undefined;
  return panelComponentRegistry[panel.content.componentType];
}
function resolvePanelComponentProps(
  panel: MangoCollapsePanel,
  panelIndex: number,
  tab: MangoCollapseTab,
  tabIndex: number,
) {
  if (panel.content?.componentType === 'description-list') {
    return { display: 'key-value', ...panel.content.data, richTextPreviewComponent: MangoRichTextPreview };
  }
  if (panel.content?.componentType === 'file-list') {
    return {
      ...panel.content.data,
      tableStyle: {
        borderRadius: 0,
        contentMinHeight: 52,
        contentPadding: '16px 0',
        contentLineHeight: 20,
        headerCellStyle: {
          height: 'auto',
          backgroundColor: 'var(--mango-table-header-bg)',
          borderRight: '1px solid var(--el-table-border-color)',
          color: 'var(--mango-table-header-text)',
          fontWeight: 600,
          padding: '11px 0',
        },
        bodyCellStyle: {
          borderRight: '1px solid var(--el-table-border-color)',
          fontSize: 14,
          lineHeight: '20px',
          padding: '0',
        },
        ...panel.content.data.tableStyle,
      },
      onSelectionChange: (rows: MangoFileListRow[]) =>
        emit('file-list-selection-change', {
          panel,
          panelIndex,
          rows,
          tab: publicTab(tab),
          tabIndex: publicTabIndex(tabIndex),
        }),
    };
  }
  if (panel.content?.componentType === 'rich-text-preview') return panel.content.data;
  if (panel.content?.componentType === 'list-table') {
    return {
      ...panel.content.data,
      card: false,
      mode: 'flat',
      showModeSwitch: false,
      headerCellStyle: {
        backgroundColor: 'var(--mango-table-header-bg)',
        color: 'var(--mango-table-header-text)',
        height: '46px',
        padding: '0',
      },
    };
  }
  throw new Error(`MangoCollapseDetailPage: 面板 ${panel.name} 未配置可用的内置子组件数据`);
}

function resolveBackTopScrollTarget() {
  let current = pageRootRef.value?.parentElement || null;
  while (current) {
    if (/(auto|scroll|overlay)/.test(window.getComputedStyle(current).overflowY)) return current;
    current = current.parentElement;
  }
  return window;
}
function currentScrollTop() {
  if (backTopScrollTarget instanceof HTMLElement) return backTopScrollTarget.scrollTop;
  return backTopScrollTarget ? window.scrollY || document.documentElement.scrollTop : 0;
}
function normalizedBackTopThreshold() {
  const threshold = Number(props.backTopThreshold);
  return Number.isFinite(threshold) && threshold >= 0 ? threshold : 300;
}
function updateBackTopVisible() {
  backTopVisible.value = props.showBackTop && currentScrollTop() >= normalizedBackTopThreshold();
}
function unbindBackTopScrollTarget() {
  backTopScrollTarget?.removeEventListener('scroll', updateBackTopVisible);
  backTopScrollTarget = null;
}
function bindBackTopScrollTarget() {
  unbindBackTopScrollTarget();
  if (!props.showBackTop || !pageRootRef.value) {
    backTopVisible.value = false;
    return;
  }
  backTopScrollTarget = resolveBackTopScrollTarget();
  backTopScrollTarget.addEventListener('scroll', updateBackTopVisible, { passive: true });
  updateBackTopVisible();
}
function scrollToPageTop() {
  (backTopScrollTarget || resolveBackTopScrollTarget()).scrollTo({ top: 0, behavior: 'smooth' });
}
function openWorkflowDrawer() {
  if (props.showWorkflow) drawerRef.value?.open();
}
function closeWorkflowDrawer() {
  drawerRef.value?.close();
}
function toggleWorkflowDrawer() {
  if (props.showWorkflow) drawerRef.value?.toggle();
}

onMounted(async () => {
  await nextTick();
  bindBackTopScrollTarget();
});
onBeforeUnmount(unbindBackTopScrollTarget);
watch(
  () => [props.showBackTop, props.backTopThreshold] as const,
  async () => {
    await nextTick();
    bindBackTopScrollTarget();
  },
);

defineExpose<MangoCollapseDetailPageExpose>({
  openWorkflowDrawer,
  closeWorkflowDrawer,
  toggleWorkflowDrawer,
  scrollToPageTop,
});
</script>

<style scoped>
.mango-collapse-detail-page {
  --mango-detail-title-size: 18px;
  --mango-detail-body-size: 14px;
  --mango-detail-meta-size: 12px;
  --mango-collapse-page-gap: 15px;

  display: flex;
  box-sizing: border-box;
  width: 100%;
  max-width: 1280px;
  min-width: 0;
  margin-inline: auto;
  flex-direction: column;
  gap: var(--mango-collapse-page-gap);
  font-family: var(--el-font-family);
  font-size: var(--mango-detail-body-size);
  line-height: 22px;
  letter-spacing: 0;
}

.mango-collapse-detail-page__content {
  display: flex;
  width: 100%;
  min-width: 0;
  padding: 15px;
  box-sizing: border-box;
  flex-direction: column;
  gap: 12px;
  background: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: var(--mango-shadow-light);
}

.mango-collapse-detail-page__content.is-shadowless {
  box-shadow: none;
}

.mango-collapse-detail-page.is-with-actions .mango-collapse-detail-page__content {
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}

.mango-collapse-detail-page__summary,
.mango-collapse-detail-page__body,
.mango-collapse-detail-page__tabs {
  width: 100%;
  min-width: 0;
}

.mango-collapse-detail-page__summary {
  margin-bottom: 6px;
}

.mango-collapse-detail-page__body,
.mango-collapse-detail-page__loading {
  min-height: 160px;
}

.mango-collapse-detail-page__tabs > :deep(.el-tabs__header) {
  margin: 0 0 14px;
}

.mango-collapse-detail-page__tabs > :deep(.el-tabs__content) {
  overflow: visible;
}

.mango-collapse-detail-page__tabs.is-implicit > :deep(.el-tabs__header) {
  display: none;
}

.mango-collapse-detail-page__collapse {
  display: block;
  overflow: visible;
  background: transparent;
  border: 0;
}

.mango-collapse-detail-page__row {
  row-gap: 10px;
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item) {
  min-width: 0;
  background: transparent;
  border: 0;
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item__header) {
  min-height: 44px;
  height: auto;
  padding: 0 16px;
  box-sizing: border-box;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  border: 0;
  border-radius: 6px;
  font-size: 14px;
  line-height: 20px;
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item__header:hover) {
  background: var(--el-fill-color);
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item__header.is-active) {
  color: var(--el-text-color-primary);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 50%, var(--el-bg-color));
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item__arrow) {
  display: inline-flex;
  width: 28px;
  height: 28px;
  margin-left: 16px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item__wrap) {
  background: var(--el-bg-color);
  border: 0;
}

.mango-collapse-detail-page__collapse :deep(.el-collapse-item__content) {
  padding: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 22px;
}

.mango-collapse-detail-page__panel-header {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
}

.mango-collapse-detail-page__panel-heading {
  min-width: 0;
  flex: 1;
  color: inherit;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  overflow-wrap: anywhere;
}

.mango-collapse-detail-page__panel-actions {
  display: inline-flex;
  margin-left: 16px;
  flex: none;
  align-items: center;
  gap: 8px;
}

.mango-collapse-detail-page__panel-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.mango-collapse-detail-page__panel-body {
  min-width: 0;
  padding: 14px 15px 20px;
}

.mango-collapse-detail-page__actions {
  position: sticky;
  bottom: 0;
  z-index: 30;
  display: flex;
  box-sizing: border-box;
  width: 100%;
  min-height: 60px;
  margin-top: calc(var(--mango-collapse-page-gap) * -1);
  padding: 0 15px;
  align-items: center;
  gap: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-radius: 0 0 8px 8px;
  background: var(--el-bg-color);
  box-shadow: var(--mango-shadow-light);
}

.mango-collapse-detail-page__actions.is-align-left {
  justify-content: flex-start;
}

.mango-collapse-detail-page__actions.is-align-center {
  justify-content: center;
}

.mango-collapse-detail-page__actions.is-align-right {
  justify-content: flex-end;
}

.mango-collapse-detail-page__back-top {
  position: fixed;
  right: 32px;
  bottom: 44px;
  z-index: 100;
  width: 36px;
  min-width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgb(49 92 246 / 18%);
}

.mango-collapse-detail-page__back-top.is-with-workflow {
  bottom: 168px;
}

@media (width <= 760px) {
  .mango-collapse-detail-page {
    max-width: none;

    --mango-collapse-page-gap: 12px;
  }

  .mango-collapse-detail-page__back-top {
    right: 16px;
    bottom: 32px;
  }

  .mango-collapse-detail-page__back-top.is-with-workflow {
    bottom: 188px;
  }

  .mango-collapse-detail-page__collapse :deep(.el-collapse-item__header) {
    min-height: 42px;
    padding-inline: 12px;
  }

  .mango-collapse-detail-page__panel-body {
    padding: 12px 15px 16px;
  }
}
</style>
