<template>
  <div
    v-loading="loading"
    class="home-container"
  >
    <section class="home-hero">
      <div>
        <div class="home-hero__title">工作台</div>
        <div class="home-hero__desc">欢迎{{ displayName }}登录，按你的使用习惯排列首页组件。</div>
      </div>
      <div class="home-hero__actions">
        <template v-if="editing">
          <el-button
            :loading="saving"
            type="primary"
            @click="saveLayout"
          >
            保存布局
          </el-button>
          <el-button @click="cancelEdit">取消</el-button>
          <el-popconfirm
            title="确认恢复默认布局？当前个人布局会被清空。"
            confirm-button-text="恢复默认"
            cancel-button-text="取消"
            @confirm="resetLayout"
          >
            <template #reference>
              <el-button>恢复默认</el-button>
            </template>
          </el-popconfirm>
        </template>
        <el-button
          v-else
          type="primary"
          @click="startEdit"
        >
          编辑布局
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="errorMessage"
      class="home-alert"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <MangoGridDesigner
      v-if="editing"
      v-model="draftItems"
      :widgets="workbenchWidgets"
      :default-width="3"
      :default-height="10"
      :row-height="15"
      :gap="15"
    />
    <MangoGridLayout
      v-else
      :items="layoutItems"
      :widgets="workbenchWidgets"
      :row-height="15"
      :gap="15"
    />
  </div>
</template>

<script setup lang="ts" name="MangoShellHome">
import { computed, onMounted, ref } from 'vue';
import { useUserInfo } from '../../stores/userInfo';
import {
  MangoGridDesigner,
  MangoGridLayout,
  gridLayoutPersonalApi,
  parseGridLayoutValue,
  stringifyGridLayoutValue,
} from '@mango/grid-layout';
import type { GridLayoutItem } from '@mango/grid-layout';
import { workbenchWidgets } from '../../grid-widgets/workbench';

const PAGE_CODE = 'admin-home-workbench';

const userInfo = useUserInfo();
const loading = ref(false);
const saving = ref(false);
const editing = ref(false);
const errorMessage = ref('');
const layoutItems = ref<GridLayoutItem[]>(defaultLayoutItems());
const draftItems = ref<GridLayoutItem[]>([]);

const displayName = computed(() => {
  const nickname = userInfo.userInfos.nickname || userInfo.userInfos.username;
  return nickname ? `，${nickname}` : '';
});

onMounted(() => {
  loadLayout();
});

async function loadLayout(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    const personal = await gridLayoutPersonalApi.getPersonal(PAGE_CODE);
    const parsed = parseGridLayoutValue(PAGE_CODE, personal?.layoutJson);
    layoutItems.value = parsed?.items?.length ? parsed.items : defaultLayoutItems();
  } catch (error) {
    errorMessage.value = '工作台布局加载失败，已使用默认布局。';
    layoutItems.value = defaultLayoutItems();
  } finally {
    loading.value = false;
  }
}

function startEdit(): void {
  draftItems.value = cloneItems(layoutItems.value);
  editing.value = true;
}

function cancelEdit(): void {
  draftItems.value = [];
  editing.value = false;
}

async function saveLayout(): Promise<void> {
  saving.value = true;
  errorMessage.value = '';
  try {
    const value = {
      schemaVersion: 1 as const,
      pageCode: PAGE_CODE,
      items: draftItems.value,
    };
    await gridLayoutPersonalApi.savePersonal({
      pageCode: PAGE_CODE,
      layoutJson: stringifyGridLayoutValue(value),
    });
    layoutItems.value = cloneItems(draftItems.value);
    editing.value = false;
  } catch (error) {
    errorMessage.value = '工作台布局保存失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

async function resetLayout(): Promise<void> {
  saving.value = true;
  errorMessage.value = '';
  try {
    await gridLayoutPersonalApi.resetPersonal(PAGE_CODE);
    layoutItems.value = defaultLayoutItems();
    draftItems.value = defaultLayoutItems();
    editing.value = false;
  } catch (error) {
    errorMessage.value = '恢复默认布局失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

function defaultLayoutItems(): GridLayoutItem[] {
  return [
    gridItem('permission', 'platform-permission', 0, 0, 3, 10, '权限与组织'),
    gridItem('workflow', 'platform-workflow', 3, 0, 3, 10, '流程与协同'),
    gridItem('common', 'platform-common', 6, 0, 3, 10, '平台基础能力'),
    gridItem('quick', 'quick-entry', 9, 0, 3, 10, '常用能力'),
    gridItem('status', 'platform-status', 0, 11, 6, 10, '平台状态'),
    gridItem('todo', 'todo', 6, 11, 6, 10, '待办提醒'),
  ];
}

function gridItem(id: string, widgetType: string, x: number, y: number, w: number, h: number, title: string): GridLayoutItem {
  return {
    id,
    widgetType,
    title,
    layout: { x, y, w, h, minW: 3, minH: 10 },
  };
}

function cloneItems(items: GridLayoutItem[]): GridLayoutItem[] {
  return items.map(item => ({
    ...item,
    layout: { ...item.layout },
    props: item.props ? { ...item.props } : undefined,
  }));
}
</script>

<style scoped lang="scss">
.home-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

.home-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid var(--mango-border-color);
  border-radius: 8px;
  background: var(--mango-bg-color);
}

.home-hero__title {
  color: var(--mango-text-color);
  font-size: 18px;
  font-weight: 600;
}

.home-hero__desc {
  margin-top: 6px;
  color: var(--mango-text-color-regular);
  font-size: 14px;
}

.home-hero__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.home-alert {
  margin-bottom: 0;
}

:deep(.home-widget-metric) {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

:deep(.home-widget-metric__value) {
  color: var(--mango-text-color);
  font-size: 28px;
  font-weight: 700;
}

:deep(.home-widget-metric__label) {
  margin-top: 8px;
  color: var(--mango-text-color-regular);
  font-size: 13px;
}

:deep(.home-widget-metric.is-primary .home-widget-metric__value) {
  color: var(--mango-color-primary);
}

:deep(.home-widget-metric.is-success .home-widget-metric__value) {
  color: var(--mango-color-success);
}

:deep(.home-widget-metric.is-warning .home-widget-metric__value) {
  color: var(--mango-color-warning);
}

:deep(.home-widget-quick) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

:deep(.home-widget-quick__item) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-height: 74px;
  padding: 12px 8px;
  border: 1px solid var(--mango-border-color);
  border-radius: 8px;
  background: var(--mango-bg-color-page);
  color: var(--mango-text-color);
  cursor: pointer;
}

:deep(.home-widget-quick__item:hover) {
  border-color: var(--mango-color-primary);
  color: var(--mango-color-primary);
}

:deep(.home-widget-status) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

:deep(.home-widget-status__label) {
  margin-bottom: 6px;
  color: var(--mango-text-color-regular);
  font-size: 13px;
}

:deep(.home-widget-list) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

:deep(.home-widget-list__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
}

:deep(.home-widget-list__item span:nth-child(2)) {
  flex: 1;
  min-width: 0;
  color: var(--mango-text-color);
}
</style>
