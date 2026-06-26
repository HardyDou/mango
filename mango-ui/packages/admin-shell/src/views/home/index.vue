<template>
  <div v-loading="loading" class="home-container">
    <section class="home-hero">
      <div>
        <div class="home-hero__title">工作台</div>
        <div class="home-hero__desc">欢迎{{ displayName }}登录，按你的使用习惯排列首页组件。</div>
      </div>
      <div class="home-hero__actions">
        <template v-if="editing">
          <el-button :loading="saving" type="primary" @click="saveLayout">
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
        <el-button v-else type="primary" @click="startEdit">
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
import { storeToRefs } from 'pinia';
import { useRouter, type LocationQueryRaw } from 'vue-router';
import { useUserInfo } from '../../stores/userInfo';
import { useRoutesList } from '../../stores/routesList';
import {
  MangoGridDesigner,
  MangoGridLayout,
  gridLayoutPersonalApi,
  parseGridLayoutValue,
  stringifyGridLayoutValue,
} from '@mango/grid-layout';
import type { GridLayoutItem } from '@mango/grid-layout';
import {
  mergeGridWidgets,
  systemGridWidgets,
} from '@mango/grid-widgets';
import type { MangoWidgetNavigateTarget, MangoWidgetRuntimeContext } from '@mango/grid-widgets';

const PAGE_CODE = 'admin-home-workbench';

const router = useRouter();
const userInfo = useUserInfo();
const routesListStore = useRoutesList();
const { routesList } = storeToRefs(routesListStore);
const loading = ref(false);
const saving = ref(false);
const editing = ref(false);
const errorMessage = ref('');
const layoutItems = ref<GridLayoutItem[]>(defaultLayoutItems());
const draftItems = ref<GridLayoutItem[]>([]);

const widgetRuntime = computed<MangoWidgetRuntimeContext>(() => ({
  pageCode: PAGE_CODE,
  mode: 'host',
  user: {
    userId: userInfo.userInfos.userId,
    username: userInfo.userInfos.username,
    nickname: userInfo.userInfos.nickname,
    avatar: userInfo.userInfos.photo,
    roles: userInfo.userInfos.roles,
    appCode: userInfo.userInfos.appCode,
  },
  tenant: {
    tenantId: userInfo.userInfos.tenantId,
    tenantCode: userInfo.userInfos.tenantCode,
    tenantName: userInfo.userInfos.tenantName,
  },
  menus: routesList.value,
  navigate: navigateWidget,
}));
const workbenchWidgets = computed(() => mergeGridWidgets({
  runtime: widgetRuntime.value,
  systemWidgets: systemGridWidgets,
  businessWidgets: [],
}));

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
    gridItem('user-profile', 'system.user-profile', 0, 0, 3, 22, '用户信息'),
    gridItem('quick', 'system.quick-entry', 3, 0, 3, 10, '快捷入口'),
    gridItem('my-process', 'system.my-process', 6, 0, 3, 10, '我的申请'),
    gridItem('message-center', 'system.message-center', 9, 0, 3, 10, '消息中心'),
    gridItem('calendar', 'system.calendar', 3, 10, 3, 10, '日历'),
    gridItem('my-task', 'system.my-task', 0, 22, 3, 10, '我的任务'),
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

async function navigateWidget(target: MangoWidgetNavigateTarget): Promise<void> {
  if (target.pageType === 'EXTERNAL_LINK' && target.url) {
    window.open(target.url, '_blank', 'noopener,noreferrer');
    return;
  }
  if (!target.path) {
    return;
  }
  const query = resolveWidgetQuery(target.raw);
  await router.push(query ? { path: target.path, query } : target.path);
}

function resolveWidgetQuery(raw: unknown): LocationQueryRaw | undefined {
  if (!raw || typeof raw !== 'object' || !('query' in raw)) {
    return undefined;
  }
  const query = (raw as { query?: unknown }).query;
  if (!query || typeof query !== 'object' || Array.isArray(query)) {
    return undefined;
  }
  return query as LocationQueryRaw;
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
</style>
