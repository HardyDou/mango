<template>
  <div v-loading="loading" class="home-container">
    <div class="home-toolbar">
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
import { ensureFeatureRegistrars } from '../../runtime/featureRegistrars';
import { useMangoAdminHomeWidgets } from '../../runtime/homeWidgets';
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
const businessHomeWidgets = useMangoAdminHomeWidgets();

const widgetRuntime = computed<MangoWidgetRuntimeContext>(() => ({
  pageCode: PAGE_CODE,
  mode: 'host',
  user: {
    userId: userInfo.userInfos.userId,
    username: userInfo.userInfos.username,
    nickname: userInfo.userInfos.nickname,
    avatar: userInfo.userInfos.photo,
    roles: userInfo.userInfos.roles,
    permissions: Array.from(new Set([
      ...(userInfo.userInfos.permissions || []),
      ...(userInfo.userInfos.authBtnList || []),
    ])),
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
  businessWidgets: businessHomeWidgets.value,
}));

onMounted(() => {
  initializeHome();
});

async function initializeHome(): Promise<void> {
  try {
    await ensureFeatureRegistrars();
  } catch (error) {
    console.error('[mango-shell] failed to register shell features', error);
  }
  await loadLayout();
}

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
  // 工作台默认布局由页面直接传入自定义布局组件，无个人配置或恢复默认时使用。
  return [
    gridItem('link-navigation', 'link.link-navigation', 0, 0, 12, 20, '网址导航', {
      minW: 6,
      minH: 18,
      showTitle: false,
      padding: false,
    }),
    gridItem('message-center', 'notice.message-center', 0, 21, 6, 18, '我的消息'),
    gridItem('quick', 'system.quick-entry', 6, 21, 3, 18, '快捷入口'),
    gridItem('calendar', 'calendar.calendar', 9, 21, 3, 14, '日历'),
    gridItem('user-profile', 'system.user-profile', 9, 36, 3, 28, '用户信息', {
      minH: 16,
      showTitle: false,
      padding: false,
    }),
    gridItem('my-process', 'workflow.my-process', 0, 40, 3, 24, '我的申请'),
    gridItem('my-task', 'workflow.my-task', 3, 40, 3, 24, '我的任务'),
    gridItem('my-todo', 'workflow.my-todo', 6, 40, 3, 24, '我的待办', {
      showTitle: false,
      padding: false,
    }),
  ];
}

function gridItem(
  id: string,
  widgetType: string,
  x: number,
  y: number,
  w: number,
  h: number,
  title: string,
  options: {
    minW?: number;
    minH?: number;
    maxW?: number;
    maxH?: number;
    showTitle?: boolean;
    padding?: boolean;
  } = {},
): GridLayoutItem {
  const {
    minW = 3,
    minH = 10,
    maxW = 12,
    maxH = 1000,
    showTitle,
    padding,
  } = options;
  return {
    id,
    widgetType,
    title,
    layout: { x, y, w, h, minW, minH, maxW, maxH },
    props: widgetType === 'link.link-navigation'
      ? { maxGroups: 24, maxItemsPerGroup: 200 }
      : undefined,
    showTitle,
    padding,
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
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

.home-toolbar {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.home-alert {
  margin-bottom: 0;
}
</style>
