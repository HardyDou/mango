<template>
  <section class="home-user-page" data-page="home.user" :data-state="pageState">
    <section class="job-toolbar" data-surface="home.user.search">
      <div class="job-toolbar-head">
        <div>
          <h2>用户首页</h2>
          <p>选择用户后查看该用户真实可见首页。</p>
        </div>
      </div>

      <el-form ref="queryFormRef" :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="用户 ID" prop="userId" class="job-search-item">
          <el-input
            v-model="query.userId"
            clearable
            placeholder="输入用户 ID"
            data-field="home.user.user-id"
            @keyup.enter="loadUserHomePages"
          />
        </el-form-item>
        <el-form-item label="选择用户" class="job-search-item job-search-item-wide">
          <el-select
            v-model="selectedUserId"
            filterable
            remote
            default-first-option
            clearable
            :remote-method="searchUsers"
            :loading="userLoading"
            placeholder="输入姓名或账号"
            no-data-text="暂无用户"
            no-match-text="暂无匹配用户"
            data-field="home.user.selector"
            @change="handleUserSelect"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成员 ID" prop="memberId" class="job-search-item">
          <el-input
            v-model="query.memberId"
            clearable
            placeholder="角色匹配"
            data-field="home.user.member-id"
            @keyup.enter="loadUserHomePages"
          />
        </el-form-item>
        <el-form-item label="部门 ID" prop="orgId" class="job-search-item">
          <el-input
            v-model="query.orgId"
            clearable
            placeholder="部门继承"
            data-field="home.user.org-id"
            @keyup.enter="loadUserHomePages"
          />
        </el-form-item>
        <el-form-item class="job-search-actions home-job-search-actions">
          <el-button
            v-auth="'home:user:view'"
            type="primary"
            :icon="View"
            :loading="loading"
            data-action="home.user.view"
            @click="loadUserHomePages"
          >
            查看
          </el-button>
          <el-button :icon="RefreshLeft" data-action="home.user.reset" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section v-loading="loading" class="home-user-panel" data-surface="home.user.preview">
      <el-alert v-if="errorMessage" class="home-user-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" data-action="home.user.retry" @click="loadUserHomePages">重试</el-button>
        </template>
      </el-alert>

      <el-empty v-if="!loaded && !loading" description="请选择用户后查看首页" />
      <el-empty v-else-if="loaded && !pages.length && !loading" description="该用户暂无可见首页" />

      <div v-else class="home-container home-user-workbench">
        <section class="home-page-bar" data-surface="home.user.page-switcher">
          <div class="home-page-bar__main">
            <div
              class="home-page-tabs"
              data-surface="home.user.tabs"
              role="tablist"
              aria-label="用户首页切换"
            >
              <button
                v-for="item in pages"
                :key="pageRouteKey(item)"
                type="button"
                class="home-page-tabs__item"
                role="tab"
                :aria-selected="selectedRouteKey === pageRouteKey(item)"
                :data-record-key="`home-user-page:${pageRouteKey(item)}`"
                :data-state="selectedRouteKey === pageRouteKey(item) ? 'active' : 'idle'"
                @click="selectedRouteKey = pageRouteKey(item)"
              >
                <el-tooltip v-if="item.defaultPage" content="默认首页" placement="bottom">
                  <el-icon class="home-page-tabs__home-icon" data-field="home.user.default-indicator"><House /></el-icon>
                </el-tooltip>
                <span class="home-page-tabs__name">{{ item.name }}</span>
                <el-tag v-if="item.builtIn" class="home-page-tabs__tag" type="info" effect="light" round>内置</el-tag>
                <el-tag v-else-if="item.readOnly" class="home-page-tabs__tag" type="success" effect="light" round>授权</el-tag>
              </button>
            </div>
            <el-tag v-if="currentPage?.builtIn" type="info" effect="light">内置</el-tag>
            <el-tag v-else-if="currentPage?.readOnly" type="success" effect="light">{{ currentPage.sourceLabel || '授权首页' }}</el-tag>
          </div>
        </section>

        <MangoGridLayout
          v-if="currentPage"
          :items="currentLayoutItems"
          :widgets="workbenchWidgets"
          :row-height="15"
          :gap="15"
          data-surface="home.user.layout"
        />
      </div>
    </section>
  </section>
</template>

<script setup lang="ts" name="HomeUserPage">
import { computed, onMounted, reactive, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, type LocationQueryRaw } from 'vue-router';
import { House, RefreshLeft, View } from '@element-plus/icons-vue';
import type { FormInstance } from 'element-plus';
import { get } from '@mango/common/utils/request';
import { homeTemplateApi, type HomePageVO } from '@mango/home';
import { MangoGridLayout, parseGridLayoutValue, type GridLayoutItem } from '@mango/grid-layout';
import { mergeGridWidgets, systemGridWidgets } from '@mango/grid-widgets';
import type { MangoWidgetNavigateTarget, MangoWidgetRuntimeContext } from '@mango/grid-widgets';
import { ensureFeatureRegistrars } from '../../../runtime/featureRegistrars';
import { useMangoAdminHomeWidgets } from '../../../runtime/homeWidgets';
import { useRoutesList } from '../../../stores/routesList';
import { useUserInfo } from '../../../stores/userInfo';
import '@mango/job/style.css';

interface UserOption {
  label: string;
  value: string;
  memberId?: string;
}

interface IdentityUserRecord {
  userId?: string | number;
  id?: string | number;
  memberId?: string | number;
  username?: string;
  nickname?: string;
  memberName?: string;
}

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
}

const PAGE_CODE = 'admin-home-workbench';

const router = useRouter();
const userInfo = useUserInfo();
const routesListStore = useRoutesList();
const { routesList } = storeToRefs(routesListStore);
const businessHomeWidgets = useMangoAdminHomeWidgets();
const queryFormRef = ref<FormInstance>();
const loading = ref(false);
const loaded = ref(false);
const userLoading = ref(false);
const errorMessage = ref('');
const pages = ref<HomePageVO[]>([]);
const selectedRouteKey = ref('');
const selectedUserId = ref('');
const userOptions = ref<UserOption[]>([]);
const query = reactive({
  userId: '',
  memberId: '',
  orgId: '',
});

const pageState = computed(() => {
  if (errorMessage.value) return 'error';
  if (loading.value) return 'loading';
  if (!loaded.value) return 'idle';
  return pages.value.length ? 'ready' : 'empty';
});
const currentPage = computed(() => pages.value.find(item => pageRouteKey(item) === selectedRouteKey.value) || pages.value[0]);
const currentLayoutItems = computed(() => resolveLayoutItems(currentPage.value?.layoutJson));
const widgetRuntime = computed<MangoWidgetRuntimeContext>(() => ({
  pageCode: PAGE_CODE,
  mode: 'host',
  user: {
    userId: query.userId || userInfo.userInfos.userId,
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

onMounted(async () => {
  await ensureFeatureRegistrars().catch(error => {
    console.error('[mango-shell] failed to register shell features', error);
  });
  await searchUsers('');
});

async function loadUserHomePages(): Promise<void> {
  const userId = normalizeText(query.userId);
  if (!userId) {
    errorMessage.value = '请选择或输入用户 ID。';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    pages.value = await homeTemplateApi.resolveUserPages({
      userId,
      memberId: normalizeText(query.memberId),
      orgId: normalizeText(query.orgId),
    });
    selectedRouteKey.value = pageRouteKey(pages.value.find(item => item.defaultPage) || pages.value[0]);
    loaded.value = true;
  } catch (error) {
    pages.value = [];
    selectedRouteKey.value = '';
    errorMessage.value = '用户首页加载失败，请稍后重试。';
  } finally {
    loading.value = false;
  }
}

function resetQuery(): void {
  query.userId = '';
  query.memberId = '';
  query.orgId = '';
  selectedUserId.value = '';
  pages.value = [];
  selectedRouteKey.value = '';
  loaded.value = false;
  errorMessage.value = '';
  queryFormRef.value?.clearValidate();
}

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true;
  try {
    const response = await get<BackendPageResult<IdentityUserRecord>>('/identity/users/page', {
      params: { page: 1, size: 50, keyword: normalizeText(keyword) },
    });
    userOptions.value = (response.records || response.list || [])
      .map(toUserOption)
      .filter((item): item is UserOption => Boolean(item));
  } catch (error) {
    userOptions.value = [];
  } finally {
    userLoading.value = false;
  }
}

function handleUserSelect(value: string): void {
  if (!value) {
    return;
  }
  query.userId = value;
  const selected = userOptions.value.find(item => item.value === value);
  if (selected?.memberId && !query.memberId) {
    query.memberId = selected.memberId;
  }
}

function toUserOption(item: IdentityUserRecord): UserOption | undefined {
  const id = item.userId ?? item.id;
  if (id === undefined || id === null) {
    return undefined;
  }
  const name = item.nickname || item.memberName || item.username || id;
  const username = item.username && item.username !== name ? ` / ${item.username}` : '';
  return {
    value: String(id),
    label: `${name}${username}`,
    memberId: item.memberId === undefined || item.memberId === null ? undefined : String(item.memberId),
  };
}

function pageRouteKey(page?: HomePageVO): string {
  return page?.routeKey || (page?.id ? String(page.id) : '');
}

function resolveLayoutItems(layoutJson?: string | null): GridLayoutItem[] {
  const parsed = parseGridLayoutValue(PAGE_CODE, layoutJson);
  return parsed?.items || [];
}

function normalizeText(value: string): string | undefined {
  const text = String(value || '').trim();
  return text || undefined;
}

async function navigateWidget(target: MangoWidgetNavigateTarget): Promise<void> {
  if (target.pageType === 'EXTERNAL_LINK' && target.url) {
    window.open(target.url, '_blank', 'noopener,noreferrer');
    return;
  }
  if (!target.path) {
    return;
  }
  const queryValue = resolveWidgetQuery(target.raw);
  await router.push(queryValue ? { path: target.path, query: queryValue } : target.path);
}

function resolveWidgetQuery(raw: unknown): LocationQueryRaw | undefined {
  if (!raw || typeof raw !== 'object' || !('query' in raw)) {
    return undefined;
  }
  const queryValue = (raw as { query?: unknown }).query;
  if (!queryValue || typeof queryValue !== 'object' || Array.isArray(queryValue)) {
    return undefined;
  }
  return queryValue as LocationQueryRaw;
}
</script>

<style scoped>
.home-user-page {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 14px;
}

.home-user-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.home-job-search-actions {
  margin-left: auto;
}

.home-user-panel {
  padding: 16px 18px 14px;
  overflow: hidden;
}

.home-user-error {
  margin-bottom: 12px;
}

.home-container {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

.home-user-workbench {
  min-height: 360px;
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

.home-page-bar__main {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 8px;
  min-width: 0;
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

.home-page-tabs__item:hover {
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

@media (max-width: 560px) {
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

  .home-user-panel {
    padding: 12px;
  }
}
</style>
