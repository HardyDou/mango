<template>
  <div
    v-loading="loading"
    class="home-container"
    data-page="home.workbench"
    :data-state="pageState"
  >
    <section class="home-page-bar" data-surface="home.page-switcher">
      <div class="home-page-bar__main">
        <div
          class="home-page-tabs"
          data-field="home.current-page"
          role="tablist"
          aria-label="首页切换"
        >
          <button
            v-for="pageTab in pageTabs"
            :key="pageTab.id"
            class="home-page-tabs__item"
            type="button"
            role="tab"
            :aria-selected="pageTab.active"
            :disabled="editing"
            :data-record-key="`home:${pageTab.id}`"
            :data-state="pageTab.active ? 'active' : 'idle'"
            @click="handleHomeSelect(pageTab.id)"
          >
            <el-tooltip v-if="pageTab.defaultPage" content="默认首页" placement="bottom">
              <el-icon class="home-page-tabs__home-icon" data-field="home.default-indicator"><House /></el-icon>
            </el-tooltip>
            <span class="home-page-tabs__name">{{ pageTab.name }}</span>
            <el-tag v-if="pageTab.builtIn" class="home-page-tabs__tag" type="info" effect="light" round>内置</el-tag>
            <el-tag v-else-if="pageTab.readOnly" class="home-page-tabs__tag" type="success" effect="light" round>授权</el-tag>
          </button>
        </div>
        <el-tag v-if="currentPage?.builtIn" type="info" effect="light">内置</el-tag>
        <el-tag v-else-if="currentPage?.readOnly" type="success" effect="light">{{ currentPage.sourceLabel || '授权首页' }}</el-tag>
      </div>

      <div class="home-page-bar__actions">
        <el-tooltip content="新建首页" placement="bottom">
          <el-button circle data-action="home.create" :disabled="editing" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重命名" placement="bottom">
          <el-button circle data-action="home.rename" :disabled="editing || !canManageCurrentPage" @click="openRenameDialog">
            <el-icon><Edit /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="复制" placement="bottom">
          <el-button circle data-action="home.duplicate" :disabled="editing || !canCopyCurrentPage" @click="duplicateCurrentPage">
            <el-icon><CopyDocument /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="前移" placement="bottom">
          <el-button circle data-action="home.sort-up" :disabled="editing || !canMoveCurrentPageUp" @click="moveCurrentPage(-1)">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="后移" placement="bottom">
          <el-button circle data-action="home.sort-down" :disabled="editing || !canMoveCurrentPageDown" @click="moveCurrentPage(1)">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="设为默认首页" placement="bottom">
          <el-button circle data-action="home.set-default" :disabled="editing || !canSetDefault" @click="setCurrentPageDefault">
            <el-icon><Star /></el-icon>
          </el-button>
        </el-tooltip>
        <el-popconfirm
          title="确认删除当前首页？删除默认首页后系统会自动选择一个有效默认首页。"
          confirm-button-text="确认删除"
          cancel-button-text="取消"
          @confirm="deleteCurrentPage"
        >
          <template #reference>
            <el-button circle type="danger" data-action="home.delete" :disabled="editing || !canManageCurrentPage">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-popconfirm>
      </div>
    </section>

    <div class="home-toolbar">
      <template v-if="editing">
        <el-tooltip content="保存布局" placement="left">
          <el-button
            :loading="saving"
            class="home-toolbar__button"
            type="primary"
            circle
            aria-label="保存布局"
            data-action="home.layout.save"
            @click="saveLayout"
          >
            <el-icon><Check /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="取消" placement="left">
          <el-button
            class="home-toolbar__button"
            circle
            aria-label="取消"
            data-action="home.layout.cancel"
            @click="cancelEdit"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </el-tooltip>
        <el-popconfirm
          title="确认恢复默认布局？当前编辑内容会被默认布局替换。"
          confirm-button-text="恢复默认"
          cancel-button-text="取消"
          @confirm="resetLayout"
        >
          <template #reference>
            <el-button
              class="home-toolbar__button"
              circle
              aria-label="恢复默认"
              data-action="home.layout.reset"
            >
              <el-icon><RefreshLeft /></el-icon>
            </el-button>
          </template>
        </el-popconfirm>
      </template>
        <el-tooltip v-else content="编辑布局" placement="left">
        <el-button
          class="home-toolbar__button"
          type="primary"
          circle
          aria-label="编辑布局"
          data-action="home.layout.edit"
          :disabled="!canEditCurrentLayout"
          @click="startEdit"
        >
          <el-icon><EditPen /></el-icon>
        </el-button>
      </el-tooltip>
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

    <el-dialog
      v-model="nameDialog.visible"
      :title="nameDialog.mode === 'create' ? '新建首页' : '重命名首页'"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="nameFormRef"
        :model="nameDialog.form"
        :rules="nameRules"
        label-width="88px"
        data-surface="home.name-form"
      >
        <el-form-item label="首页名称" prop="name">
          <el-input
            v-model="nameDialog.form.name"
            data-field="home.name"
            maxlength="64"
            show-word-limit
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nameDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" data-action="home.name.submit" @click="submitNameDialog">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MangoShellHome">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Close,
  CopyDocument,
  Delete,
  Edit,
  EditPen,
  House,
  Plus,
  RefreshLeft,
  Star,
} from '@element-plus/icons-vue';
import { homePageApi, type HomePageVO } from '@mango/home';
import { useUserInfo } from '../../stores/userInfo';
import { useRoutesList } from '../../stores/routesList';
import { ensureFeatureRegistrars } from '../../runtime/featureRegistrars';
import { useMangoAdminHomeWidgets } from '../../runtime/homeWidgets';
import {
  MangoGridDesigner,
  MangoGridLayout,
  parseGridLayoutValue,
} from '@mango/grid-layout';
import type { GridLayoutItem } from '@mango/grid-layout';
import {
  mergeGridWidgets,
  systemGridWidgets,
} from '@mango/grid-widgets';
import type { MangoWidgetNavigateTarget, MangoWidgetRuntimeContext } from '@mango/grid-widgets';

const BUILT_IN_HOME_ID = '__built_in__';
const PAGE_CODE = 'admin-home-workbench';

const route = useRoute();
const router = useRouter();
const userInfo = useUserInfo();
const routesListStore = useRoutesList();
const { routesList } = storeToRefs(routesListStore);
const loading = ref(false);
const saving = ref(false);
const editing = ref(false);
const errorMessage = ref('');
const pages = ref<HomePageVO[]>([]);
const currentPage = ref<HomePageVO | null>(null);
const selectedHomeId = ref(BUILT_IN_HOME_ID);
const layoutItems = ref<GridLayoutItem[]>(defaultLayoutItems());
const draftItems = ref<GridLayoutItem[]>([]);
const businessHomeWidgets = useMangoAdminHomeWidgets();
const nameFormRef = ref<FormInstance>();
const nameDialog = reactive({
  visible: false,
  mode: 'create' as 'create' | 'rename',
  form: {
    name: '',
  },
});

const nameRules: FormRules = {
  name: [
    { required: true, message: '请输入首页名称', trigger: 'blur' },
    { max: 64, message: '首页名称长度不能超过64', trigger: 'blur' },
  ],
};

const pageState = computed(() => {
  if (errorMessage.value) return 'error';
  if (editing.value) return 'editing';
  if (loading.value) return 'loading';
  return 'ready';
});
const currentRouteKey = computed(() => pageRouteKey(currentPage.value));
const canManageCurrentPage = computed(() => Boolean(currentPage.value?.id && !currentPage.value.builtIn && !currentPage.value.readOnly));
const canCopyCurrentPage = computed(() => Boolean(currentPage.value && !currentPage.value.builtIn && currentPage.value.canCopy !== false));
const canSetDefault = computed(() => Boolean(currentRouteKey.value && currentRouteKey.value !== BUILT_IN_HOME_ID && !currentPage.value?.defaultPage));
const canEditCurrentLayout = computed(() => Boolean(!currentPage.value?.readOnly || currentPage.value?.builtIn));
const currentPageIndex = computed(() => {
  const id = currentPage.value?.id;
  return id ? pages.value.findIndex(pageItem => String(pageItem.id) === String(id)) : -1;
});
const canMoveCurrentPageUp = computed(() => currentPageIndex.value > 0);
const canMoveCurrentPageDown = computed(() => currentPageIndex.value >= 0 && currentPageIndex.value < pages.value.length - 1);
const pageTabs = computed(() => {
  const tabMap = new Map<string, {
    id: string;
    name: string;
    defaultPage?: boolean;
    builtIn: boolean;
    readOnly?: boolean;
    active: boolean;
  }>();
  pages.value.forEach(pageItem => {
    const id = pageRouteKey(pageItem);
    if (tabMap.has(id)) {
      return;
    }
    tabMap.set(id, {
      id,
      name: pageItem.name,
      defaultPage: pageItem.defaultPage,
      builtIn: Boolean(pageItem.builtIn),
      readOnly: Boolean(pageItem.readOnly),
      active: selectedHomeId.value === id,
    });
  });
  if (currentPage.value?.builtIn && !tabMap.has(BUILT_IN_HOME_ID)) {
    tabMap.set(BUILT_IN_HOME_ID, {
        id: BUILT_IN_HOME_ID,
        name: '系统工作台',
        defaultPage: currentPage.value.defaultPage,
        builtIn: true,
        active: selectedHomeId.value === BUILT_IN_HOME_ID,
    });
  }
  return Array.from(tabMap.values());
});

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

watch(
  () => route.path,
  async (path) => {
    if (!path.startsWith('/home')) {
      return;
    }
    await loadPagesAndResolve();
  },
);

async function initializeHome(): Promise<void> {
  try {
    await ensureFeatureRegistrars();
  } catch (error) {
    console.error('[mango-shell] failed to register shell features', error);
  }
  await loadPagesAndResolve();
}

async function loadPagesAndResolve(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    pages.value = await homePageApi.listMyPages();
    currentPage.value = await homePageApi.resolve({ homeId: routeHomeId() });
    selectedHomeId.value = pageRouteKey(currentPage.value);
    layoutItems.value = resolveLayoutItems(currentPage.value.layoutJson);
  } catch (error) {
    errorMessage.value = '首页工作台加载失败，已使用系统默认布局。';
    currentPage.value = builtInFallbackPage();
    selectedHomeId.value = BUILT_IN_HOME_ID;
    layoutItems.value = defaultLayoutItems();
  } finally {
    loading.value = false;
  }
}

function routeHomeId(): string | undefined {
  const match = route.path.match(/^\/home\/([^/]+)$/);
  return match?.[1];
}

async function handleHomeSelect(value: string): Promise<void> {
  if (value === BUILT_IN_HOME_ID) {
    await router.push('/home');
    return;
  }
  await router.push(`/home/${value}`);
}

function startEdit(): void {
  if (!canEditCurrentLayout.value) {
    return;
  }
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
    const layoutJson = stringifyHomeLayout(draftItems.value);
    let savedPage: HomePageVO;
    if (currentPage.value?.id && !currentPage.value.readOnly) {
      savedPage = await homePageApi.saveLayout(String(currentPage.value.id), { layoutJson });
    } else {
      savedPage = await homePageApi.create({
        name: '我的工作台',
        layoutJson,
        setDefault: true,
      });
    }
    currentPage.value = savedPage;
    layoutItems.value = cloneItems(draftItems.value);
    editing.value = false;
    await refreshPagesAndNavigate(savedPage);
  } catch (error) {
    errorMessage.value = '首页布局保存失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

function resetLayout(): void {
  draftItems.value = defaultLayoutItems();
}

function openCreateDialog(): void {
  nameDialog.mode = 'create';
  nameDialog.form.name = nextHomeName();
  nameDialog.visible = true;
  focusNameInput();
}

function openRenameDialog(): void {
  if (!currentPage.value?.id) {
    return;
  }
  nameDialog.mode = 'rename';
  nameDialog.form.name = currentPage.value.name;
  nameDialog.visible = true;
  focusNameInput();
}

async function submitNameDialog(): Promise<void> {
  const valid = await nameFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  errorMessage.value = '';
  try {
    let page: HomePageVO;
    if (nameDialog.mode === 'create') {
      page = await homePageApi.create({
        name: nameDialog.form.name.trim(),
        layoutJson: stringifyHomeLayout(layoutItems.value.length ? layoutItems.value : defaultLayoutItems()),
        setDefault: pages.value.length === 0,
      });
    } else {
      page = await homePageApi.rename(String(currentPage.value?.id), { name: nameDialog.form.name.trim() });
    }
    nameDialog.visible = false;
    await refreshPagesAndNavigate(page);
  } catch (error) {
    errorMessage.value = nameDialog.mode === 'create' ? '新建首页失败，请稍后重试。' : '重命名首页失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

async function duplicateCurrentPage(): Promise<void> {
  if (!currentPage.value || !canCopyCurrentPage.value) {
    return;
  }
  saving.value = true;
  errorMessage.value = '';
  try {
    const page = currentPage.value.id && !currentPage.value.readOnly
      ? await homePageApi.duplicate(String(currentPage.value.id))
      : await homePageApi.create({
        name: `${currentPage.value.name} 副本`,
        layoutJson: stringifyHomeLayout(layoutItems.value),
        setDefault: false,
      });
    await refreshPagesAndNavigate(page);
  } catch (error) {
    errorMessage.value = '复制首页失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

async function moveCurrentPage(offset: -1 | 1): Promise<void> {
  const index = currentPageIndex.value;
  if (index < 0) {
    return;
  }
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= pages.value.length) {
    return;
  }
  const sortedPages = [...pages.value];
  const [moving] = sortedPages.splice(index, 1);
  sortedPages.splice(targetIndex, 0, moving);
  saving.value = true;
  errorMessage.value = '';
  try {
    pages.value = await homePageApi.sort({ ids: sortedPages.map(pageItem => String(pageItem.id)) });
  } catch (error) {
    errorMessage.value = '首页排序保存失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

async function setCurrentPageDefault(): Promise<void> {
  const routeKey = currentRouteKey.value;
  if (!routeKey || routeKey === BUILT_IN_HOME_ID) {
    return;
  }
  saving.value = true;
  errorMessage.value = '';
  try {
    const page = await homePageApi.setDefault(routeKey);
    await refreshPagesAndNavigate(page);
  } catch (error) {
    errorMessage.value = '设置默认首页失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

async function deleteCurrentPage(): Promise<void> {
  if (!currentPage.value?.id) {
    return;
  }
  saving.value = true;
  errorMessage.value = '';
  try {
    const page = await homePageApi.delete(String(currentPage.value.id));
    await refreshPagesAndNavigate(page);
  } catch (error) {
    errorMessage.value = '删除首页失败，请稍后重试。';
  } finally {
    saving.value = false;
  }
}

async function refreshPagesAndNavigate(page: HomePageVO): Promise<void> {
  pages.value = await homePageApi.listMyPages();
  currentPage.value = page;
  selectedHomeId.value = pageRouteKey(page);
  layoutItems.value = resolveLayoutItems(page.layoutJson);
  await router.replace(pageRouteKey(page) === BUILT_IN_HOME_ID ? '/home' : `/home/${pageRouteKey(page)}`);
}

function pageRouteKey(page?: HomePageVO | null): string {
  if (!page) {
    return BUILT_IN_HOME_ID;
  }
  return page.routeKey || (page.id ? String(page.id) : BUILT_IN_HOME_ID);
}

function resolveLayoutItems(layoutJson?: string | null): GridLayoutItem[] {
  const parsed = parseGridLayoutValue(PAGE_CODE, layoutJson);
  return parsed?.items?.length ? parsed.items : defaultLayoutItems();
}

function stringifyHomeLayout(items: GridLayoutItem[]): string {
  return JSON.stringify({
    schemaVersion: 1,
    items,
  });
}

function builtInFallbackPage(): HomePageVO {
  return {
    name: '系统工作台',
    layoutJson: stringifyHomeLayout(defaultLayoutItems()),
    sort: 0,
    enabled: true,
    defaultPage: true,
    builtIn: true,
  };
}

function nextHomeName(): string {
  return `我的工作台 ${pages.value.length + 1}`;
}

function focusNameInput(): void {
  nextTick(() => {
    const input = document.querySelector<HTMLElement>('[data-field="home.name"] input');
    input?.focus();
  });
}

function defaultLayoutItems(): GridLayoutItem[] {
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

.home-page-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.home-page-bar__main,
.home-page-bar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.home-page-bar__main {
  min-width: 0;
  flex: 1;
}

.home-page-tabs {
  display: flex;
  min-width: 0;
  max-width: min(720px, 58vw);
  padding: 2px;
  overflow-x: auto;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  scrollbar-width: thin;
}

.home-page-tabs__item {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  max-width: 180px;
  height: 30px;
  padding: 0 10px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
  transition: color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

.home-page-tabs__item:hover:not(:disabled) {
  color: var(--el-color-primary);
  background: var(--el-bg-color);
}

.home-page-tabs__item[data-state='active'] {
  color: var(--el-color-primary);
  cursor: default;
  background: var(--el-bg-color);
  box-shadow: 0 1px 4px rgb(31 45 61 / 8%);
}

.home-page-tabs__name {
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-page-tabs__home-icon {
  flex: 0 0 auto;
  color: var(--el-color-success);
  font-size: 15px;
}

.home-page-tabs__tag {
  flex: 0 0 auto;
}

.home-page-bar__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.home-toolbar {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.home-toolbar__button {
  width: 44px;
  height: 44px;
  box-shadow: 0 8px 18px rgb(31 45 61 / 16%);
}

.home-toolbar :deep(.el-button + .el-button) {
  margin-left: 0;
}

.home-alert {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .home-page-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .home-page-bar__main {
    flex-wrap: wrap;
  }

  .home-page-tabs {
    width: 100%;
    max-width: 100%;
  }

  .home-page-bar__actions {
    justify-content: flex-start;
  }
}
</style>
