<template>
  <section class="mango-grid-widget-quick-entry">
    <header class="mango-grid-widget-quick-entry__title">
      <span>快捷入口</span>
      <button
        class="mango-grid-widget-quick-entry__setting"
        type="button"
        title="设置快捷入口"
        @click="openSetting"
      >
        <el-icon><Setting /></el-icon>
      </button>
    </header>

    <div class="mango-grid-widget-quick-entry__body">
      <button
        v-for="item in selectedMenus"
        :key="item.id"
        class="mango-grid-widget-quick-entry__item"
        type="button"
        @click="handleNavigate(item)"
      >
        <el-icon size="18">
          <component
            :is="item.icon || LinkIcon"
          />
        </el-icon>
        <span>{{ item.title }}</span>
      </button>

      <el-empty
        v-if="selectedMenus.length === 0"
        description="暂无快捷入口"
        :image-size="64"
      >
        <el-button
          text
          type="primary"
          @click="openSetting"
        >
          去设置
        </el-button>
      </el-empty>
    </div>

    <MangoDialog
      v-model="settingVisible"
      title="设置快捷入口"
      width="860px"
      footer-align="right"
      destroy-on-close
      append-to-body
      class="mango-grid-widget-quick-entry-dialog"
      @open="syncDraftSelection"
    >
      <div class="mango-grid-widget-quick-entry-config">
        <section class="mango-grid-widget-quick-entry-config__panel">
          <div class="mango-grid-widget-quick-entry-config__panel-title">可选菜单</div>
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索菜单名称或路径"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div class="mango-grid-widget-quick-entry-config__list">
            <label
              v-for="item in filteredMenus"
              :key="item.id"
              class="mango-grid-widget-quick-entry-config__option"
              @click.prevent="toggleDraftSelection(item.id)"
            >
              <el-checkbox
                :model-value="draftSelectedIds.includes(item.id)"
                @click.stop
                @change="toggleDraftSelection(item.id)"
              />
              <span class="mango-grid-widget-quick-entry-config__option-icon">
                <el-icon>
                  <component :is="item.icon || LinkIcon" />
                </el-icon>
              </span>
              <span class="mango-grid-widget-quick-entry-config__option-info">
                <span>{{ item.title }}</span>
                <small>{{ formatMenuLocation(item) }}</small>
              </span>
            </label>

            <el-empty
              v-if="filteredMenus.length === 0"
              description="暂无可选菜单"
              :image-size="72"
            />
          </div>
        </section>

        <section class="mango-grid-widget-quick-entry-config__panel">
          <div class="mango-grid-widget-quick-entry-config__panel-title">
            已选快捷入口
            <span>{{ draftSelectedMenus.length }}</span>
          </div>
          <div class="mango-grid-widget-quick-entry-config__selected">
            <div
              v-for="item in draftSelectedMenus"
              :key="item.id"
              class="mango-grid-widget-quick-entry-config__selected-item"
            >
              <span class="mango-grid-widget-quick-entry-config__option-icon">
                <el-icon>
                  <component :is="item.icon || LinkIcon" />
                </el-icon>
              </span>
              <span class="mango-grid-widget-quick-entry-config__option-info">
                <span>{{ item.title }}</span>
                <small>{{ formatMenuLocation(item) }}</small>
              </span>
              <button
                type="button"
                title="移除"
                @click="removeDraftSelection(item.id)"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>

            <el-empty
              v-if="draftSelectedMenus.length === 0"
              description="还没有选择快捷入口"
              :image-size="72"
            />
          </div>
        </section>
      </div>

      <template #footer>
        <el-button @click="clearSelection">清空</el-button>
        <el-button @click="settingVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="saveSelection"
        >
          保存
        </el-button>
      </template>
    </MangoDialog>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Component } from 'vue';
import { Close, Link as LinkIcon, Search, Setting } from '@element-plus/icons-vue';
import { iconMap, MangoDialog } from '@mango/common';
import type { QuickEntryMenuItem, QuickEntryWidgetProps } from '../../types';

defineOptions({
  name: 'MangoQuickEntryWidget',
});

const props = withDefaults(defineProps<QuickEntryWidgetProps>(), {
  menus: () => [],
  maxDefaultItems: 6,
  navigate: undefined,
});

const settingVisible = ref(false);
const keyword = ref('');
const selectedIds = ref<string[]>([]);
const draftSelectedIds = ref<string[]>([]);

const rawMenus = computed(() => props.menus.length ? props.menus : props.runtime?.menus || []);
const availableMenus = computed(() => {
  return props.resolveMenus
    ? props.resolveMenus(rawMenus.value, props.runtime)
    : resolveQuickEntryMenus(rawMenus.value);
});
const effectiveStorageKey = computed(() => props.storageKey || createStorageKey());

const menuMap = computed(() => {
  return availableMenus.value.reduce<Record<string, QuickEntryMenuItem>>((result, item) => {
    result[item.id] = item;
    return result;
  }, {});
});

const effectiveSelectedIds = computed(() => {
  const storedIds = selectedIds.value.filter(id => menuMap.value[id]);
  if (storedIds.length > 0 || hasStoredSelection()) {
    return storedIds;
  }
  return availableMenus.value.slice(0, props.maxDefaultItems).map(item => item.id);
});

const selectedMenus = computed(() => {
  return effectiveSelectedIds.value
    .map(id => menuMap.value[id])
    .filter(Boolean);
});

const filteredMenus = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return availableMenus.value;
  }
  return availableMenus.value.filter((item) => {
    return [item.title, item.path, item.moduleCode, item.appCode]
      .some(field => String(field || '').toLowerCase().includes(value));
  });
});

const draftSelectedMenus = computed(() => {
  return draftSelectedIds.value
    .map(id => menuMap.value[id])
    .filter(Boolean);
});

watch(
  () => [effectiveStorageKey.value, availableMenus.value],
  () => {
    selectedIds.value = loadSelection();
  },
  { immediate: true, deep: true },
);

function openSetting(): void {
  syncDraftSelection();
  settingVisible.value = true;
}

function syncDraftSelection(): void {
  draftSelectedIds.value = [...effectiveSelectedIds.value];
}

function toggleDraftSelection(id: string): void {
  if (draftSelectedIds.value.includes(id)) {
    removeDraftSelection(id);
    return;
  }
  draftSelectedIds.value = [...draftSelectedIds.value, id];
}

function removeDraftSelection(id: string): void {
  draftSelectedIds.value = draftSelectedIds.value.filter(item => item !== id);
}

function clearSelection(): void {
  draftSelectedIds.value = [];
}

function saveSelection(): void {
  selectedIds.value = draftSelectedIds.value.filter(id => menuMap.value[id]);
  saveStoredSelection(selectedIds.value);
  settingVisible.value = false;
}

async function handleNavigate(item: QuickEntryMenuItem): Promise<void> {
  if (props.navigate) {
    await props.navigate(item);
    return;
  }
  await props.runtime?.navigate?.({
    path: item.path,
    url: item.url,
    pageType: item.pageType,
    raw: item,
  });
}

function hasStoredSelection(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  return window.localStorage.getItem(effectiveStorageKey.value) !== null;
}

function loadSelection(): string[] {
  if (typeof window === 'undefined') {
    return [];
  }
  const raw = window.localStorage.getItem(effectiveStorageKey.value);
  if (!raw) {
    return [];
  }
  try {
    const value = JSON.parse(raw);
    return Array.isArray(value) ? value.filter(item => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

function saveStoredSelection(ids: string[]): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(effectiveStorageKey.value, JSON.stringify(ids));
}

function createStorageKey(): string {
  const pageCode = props.runtime?.pageCode || 'default-page';
  const tenantId = props.runtime?.tenant?.tenantId || 'default-tenant';
  const username = props.runtime?.user?.username || props.runtime?.user?.userId || 'anonymous';
  return `mango:${pageCode}:quick-entry:${tenantId}:${username}`;
}

function resolveQuickEntryMenus(menus: unknown[]): QuickEntryMenuItem[] {
  const result: QuickEntryMenuItem[] = [];
  const visit = (items: unknown[]) => {
    items.forEach((item) => {
      if (isQuickEntryMenuItem(item)) {
        result.push(item);
        return;
      }
      const menu = toRecord(item);
      if (!menu) {
        return;
      }
      const sourceMenu = toRecord(menu.sourceMenu);
      if (isQuickEntryRoute(menu, sourceMenu)) {
        result.push(toQuickEntryMenu(menu, sourceMenu));
      }
      const children = Array.isArray(menu.children) ? menu.children : [];
      if (children.length > 0) {
        visit(children);
      }
    });
  };
  visit(menus);
  return dedupeMenus(result);
}

function isQuickEntryRoute(route: Record<string, unknown>, sourceMenu?: Record<string, unknown>): boolean {
  const meta = toRecord(route.meta);
  const path = toString(route.path);
  const externalUrl = toString(sourceMenu?.externalUrl);
  if ((!path && !externalUrl) || toBoolean(meta?.isHide)) {
    return false;
  }
  if (!sourceMenu) {
    return Boolean(route.component || path === '/home');
  }
  if (toNumber(sourceMenu.menuType) !== 2 || toNumber(sourceMenu.visible) === 0) {
    return false;
  }
  const pageType = toString(sourceMenu.pageType);
  if (pageType === 'IFRAME' || pageType === 'EXTERNAL_LINK') {
    return Boolean(externalUrl || path);
  }
  return Boolean(toString(sourceMenu.component) || path === '/home');
}

function toQuickEntryMenu(route: Record<string, unknown>, sourceMenu?: Record<string, unknown>): QuickEntryMenuItem {
  const meta = toRecord(route.meta);
  const iconName = toString(meta?.icon) || toString(sourceMenu?.icon);
  const path = toString(route.path);
  const externalUrl = toString(sourceMenu?.externalUrl);
  const pageType = toString(sourceMenu?.pageType);
  return {
    id: toString(sourceMenu?.menuId) || toString(route.name) || path || externalUrl,
    title: toString(meta?.title) || toString(sourceMenu?.menuName) || toString(route.name) || path || externalUrl,
    path,
    url: externalUrl || undefined,
    icon: resolveMenuIcon(iconName),
    iconName,
    moduleCode: toString(sourceMenu?.moduleCode) || undefined,
    appCode: toString(sourceMenu?.appCode) || undefined,
    pageType: pageType || undefined,
    raw: sourceMenu || route,
  };
}

function formatMenuLocation(item: QuickEntryMenuItem): string {
  return item.url || item.path;
}

function dedupeMenus(menus: QuickEntryMenuItem[]): QuickEntryMenuItem[] {
  const menuMap = new Map<string, QuickEntryMenuItem>();
  menus.forEach((item) => {
    if (!menuMap.has(item.id)) {
      menuMap.set(item.id, item);
    }
  });
  return Array.from(menuMap.values());
}

function isQuickEntryMenuItem(value: unknown): value is QuickEntryMenuItem {
  const item = toRecord(value);
  return Boolean(item && typeof item.id === 'string' && typeof item.title === 'string' && typeof item.path === 'string');
}

function resolveMenuIcon(icon?: string): Component | undefined {
  return icon ? iconMap[icon] : undefined;
}

function toRecord(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object' ? value as Record<string, unknown> : undefined;
}

function toString(value: unknown): string {
  return typeof value === 'string' || typeof value === 'number' ? String(value) : '';
}

function toNumber(value: unknown): number | undefined {
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
  }
  return undefined;
}

function toBoolean(value: unknown): boolean {
  return value === true || value === 'true' || value === 1 || value === '1';
}
</script>
