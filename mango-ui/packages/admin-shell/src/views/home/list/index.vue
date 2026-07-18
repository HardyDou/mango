<template>
  <section class="home-list-page" data-page="home.list" :data-state="pageState">
    <section class="job-toolbar" data-surface="home.list.search">
      <div class="job-toolbar-head">
        <div>
          <h2>首页列表</h2>
          <p>查看所有用户自定义首页，默认展示全部用户数据。</p>
        </div>
      </div>

      <el-form ref="queryFormRef" :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="关键词" prop="keyword" class="job-search-item job-search-item-wide">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="首页名称 / 路由标识"
            data-field="home.list.keyword"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户" prop="userId" class="job-search-item home-list-user-filter">
          <el-select
            v-model="query.userId"
            clearable
            filterable
            :loading="userLoading"
            placeholder="请选择用户"
            data-field="home.list.user-selector"
            @visible-change="handleUserSelectVisible"
            @change="handleSearch"
          >
            <el-option
              v-for="user in filteredUserOptions"
              :key="String(user.userId)"
              :label="formatUserOption(user)"
              :value="String(user.userId)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="enabled" class="job-search-item job-search-item-small">
          <el-select v-model="query.enabled" clearable placeholder="全部" data-field="home.list.enabled">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item class="job-search-actions home-job-search-actions">
          <el-button
            v-auth="'home:list:view'"
            type="primary"
            :icon="Search"
            :loading="loading"
            data-action="home.list.search"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button :icon="RefreshLeft" data-action="home.list.reset" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="job-panel" data-surface="home.list.table-panel">
      <el-alert v-if="errorMessage" class="home-list-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" data-action="home.list.retry" @click="loadPages">重试</el-button>
        </template>
      </el-alert>

      <div class="home-list-toolbar" data-surface="home.list.batch-toolbar">
        <el-button
          v-auth="'home:list:delete'"
          type="danger"
          plain
          :disabled="selectedPages.length === 0"
          data-action="home.list.batch-delete"
          @click="deleteSelectedPages"
        >
          批量删除
        </el-button>
        <span>已选 {{ selectedPages.length }} 项</span>
      </div>

      <el-table
        v-loading="loading"
        :data="pages"
        row-key="id"
        stripe
        data-surface="home.list.table"
      >
        <template #empty>
          <el-empty :description="emptyDescription" />
        </template>
        <el-table-column width="48" fixed="left">
          <template #header>
            <el-checkbox
              :model-value="allSelectableSelected"
              :indeterminate="someSelectableSelected"
              :disabled="selectablePages.length === 0"
              aria-label="选择所有首页"
              data-action="home.list.select-all"
              @change="toggleAllPages"
            />
          </template>
          <template #default="{ row }">
            <el-checkbox
              :model-value="isPageSelected(row)"
              :disabled="!canDeletePage(row)"
              aria-label="选择首页"
              data-action="home.list.select-row"
              :data-record-key="`home-page:${row.id || row.routeKey}`"
              @change="togglePage(row, Boolean($event))"
            />
          </template>
        </el-table-column>
        <el-table-column label="首页名称" min-width="240" fixed="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="home-list-name" :data-record-key="`home-page:${row.id || row.routeKey}`">
              <strong>{{ row.name }}</strong>
              <span>{{ row.id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="routeKey" label="路由标识" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.routeKey || '-' }}</template>
        </el-table-column>
        <el-table-column label="用户" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ formatUserDisplay(row.userId) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="108">
          <template #default>
            <el-tag type="info" effect="light">用户定义</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="86">
          <template #default="{ row }">
            <el-tag v-if="row.defaultPage" type="success" effect="light">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="184" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              data-action="home.list.preview"
              :data-record-key="`home-page:${row.id || row.routeKey}`"
              @click="openPreview(row)"
            >
              预览
            </el-button>
            <el-button
              v-auth="'home:list:edit'"
              link
              type="primary"
              :disabled="!canEditPage(row)"
              data-action="home.list.edit"
              :data-record-key="`home-page:${row.id || row.routeKey}`"
              @click="openEditor(row)"
            >
              编辑
            </el-button>
            <el-button
              v-auth="'home:list:delete'"
              link
              type="danger"
              :disabled="!canDeletePage(row)"
              data-action="home.list.delete"
              :data-record-key="`home-page:${row.id || row.routeKey}`"
              @click="deletePage(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="job-pagination">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          data-surface="home.list.pagination"
          @current-change="loadPages"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="preview.visible" title="预览首页" width="1180px" destroy-on-close append-to-body>
      <div class="home-list-preview" data-surface="home.list.preview">
        <MangoGridLayout
          :items="previewItems"
          :widgets="templateWidgets"
          :row-height="15"
          :gap="15"
          :auto-fit="false"
        />
      </div>
    </el-dialog>

    <el-dialog
      v-model="editor.visible"
      title="编辑首页"
      width="1180px"
      destroy-on-close
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="editorFormRef" :model="editor.form" :rules="editorRules" label-width="86px" data-surface="home.list.editor">
        <div class="home-admin-form-section">基本信息</div>
        <el-row :gutter="14">
          <el-col :xs="24" :md="12">
            <el-form-item label="首页名称" prop="name">
              <el-input
                v-model="editor.form.name"
                maxlength="64"
                show-word-limit
                clearable
                placeholder="请输入首页名称"
                data-field="home.list.editor.name"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="home-admin-form-section">布局配置</div>
        <div class="home-list-designer" data-surface="home.list.designer">
          <MangoGridDesigner
            v-model="editorItems"
            :widgets="templateWidgets"
            :default-width="3"
            :default-height="10"
            :row-height="15"
            :gap="15"
          />
        </div>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="closeEditor">取消</el-button>
        <el-button type="primary" :loading="saving" data-action="home.list.save" @click="savePage">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts" name="HomeListPage">
import { computed, onMounted, reactive, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, type LocationQueryRaw } from 'vue-router';
import { RefreshLeft, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { homePageApi, type HomePageVO, type UserHomePageResult } from '@mango/home';
import { MangoGridDesigner, MangoGridLayout, parseGridLayoutValue, type GridLayoutItem } from '@mango/grid-layout';
import { mergeGridWidgets, systemGridWidgets } from '@mango/grid-widgets';
import type { MangoWidgetNavigateTarget, MangoWidgetRuntimeContext } from '@mango/grid-widgets';
import { userApi, type IdentityUserVO } from '@mango/rbac';
import { ensureFeatureRegistrars } from '../../../runtime/featureRegistrars';
import { useMangoAdminHomeWidgets } from '../../../runtime/homeWidgets';
import { useRoutesList } from '../../../stores/routesList';
import { useUserInfo } from '../../../stores/userInfo';
import '@mango/job/style.css';

type UserHomePageResponse = UserHomePageResult | { data?: UserHomePageResult | { data?: UserHomePageResult } };

const PAGE_CODE = 'admin-home-list';

const router = useRouter();
const userInfo = useUserInfo();
const routesListStore = useRoutesList();
const { routesList } = storeToRefs(routesListStore);
const businessHomeWidgets = useMangoAdminHomeWidgets();
const queryFormRef = ref<FormInstance>();
const editorFormRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const loaded = ref(false);
const userLoading = ref(false);
const errorMessage = ref('');
const pages = ref<HomePageVO[]>([]);
const selectedPageIds = ref<string[]>([]);
const userOptions = ref<IdentityUserVO[]>([]);
const editorItems = ref<GridLayoutItem[]>([]);
const query = reactive<{
  keyword: string;
  userId: string;
  enabled?: boolean;
}>({
  keyword: '',
  userId: '',
  enabled: undefined,
});
const pager = reactive({
  page: 1,
  size: 20,
  total: 0,
});
const preview = reactive({
  visible: false,
  userId: '',
  layoutJson: '',
});
const editor = reactive({
  visible: false,
  form: {
    id: '',
    userId: '',
    name: '',
  },
});

const editorRules: FormRules = {
  name: [
    { required: true, message: '请输入首页名称', trigger: 'blur' },
    { max: 64, message: '首页名称不能超过 64 个字符', trigger: 'blur' },
  ],
};
const pageState = computed(() => {
  if (errorMessage.value) return 'error';
  if (loading.value) return 'loading';
  if (!loaded.value) return 'idle';
  return pages.value.length > 0 ? 'ready' : 'empty';
});
const hasQuery = computed(() => Boolean(query.keyword.trim() || query.userId.trim() || query.enabled !== undefined));
const emptyDescription = computed(() => hasQuery.value ? '暂无符合条件的用户定义首页' : '暂无用户定义首页');
const filteredUserOptions = computed(() => userOptions.value.filter(user => user.userId));
const selectablePages = computed(() => pages.value.filter(canDeletePage));
const selectedPages = computed(() => {
  const selected = new Set(selectedPageIds.value);
  return selectablePages.value.filter(row => row.id !== undefined && selected.has(String(row.id)));
});
const allSelectableSelected = computed(() => selectablePages.value.length > 0 && selectedPages.value.length === selectablePages.value.length);
const someSelectableSelected = computed(() => selectedPages.value.length > 0 && !allSelectableSelected.value);
const activeRuntimeUserId = computed(() => editor.form.userId || preview.userId || query.userId || userInfo.userInfos.userId);
const widgetRuntime = computed<MangoWidgetRuntimeContext>(() => ({
  pageCode: PAGE_CODE,
  mode: 'host',
  user: {
    userId: activeRuntimeUserId.value,
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
const templateWidgets = computed(() => mergeGridWidgets({
  runtime: widgetRuntime.value,
  systemWidgets: systemGridWidgets,
  businessWidgets: businessHomeWidgets.value,
}));
const previewItems = computed(() => resolveLayoutItems(preview.layoutJson));

onMounted(initializePage);

async function initializePage(): Promise<void> {
  try {
    await ensureFeatureRegistrars();
  } catch (error) {
    console.error('[mango-shell] failed to register shell features', error);
  }
  await Promise.all([loadUserOptions(), loadPages()]);
}

async function loadPages(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await homePageApi.pageUserPages({
      page: pager.page,
      size: pager.size,
      keyword: normalizeText(query.keyword),
      userId: normalizeText(query.userId),
      enabled: query.enabled,
    }) as unknown as UserHomePageResponse;
    const result = normalizePageResult(response);
    pages.value = (result.list || []).filter(page => !page.builtIn && !page.readOnly && !page.templateId);
    pager.total = Number(result.total || pages.value.length || 0);
    selectedPageIds.value = [];
    loaded.value = true;
  } catch (error) {
    pages.value = [];
    pager.total = 0;
    selectedPageIds.value = [];
    errorMessage.value = '首页列表加载失败，请稍后重试。';
  } finally {
    loading.value = false;
  }
}

async function loadUserOptions(): Promise<void> {
  userLoading.value = true;
  try {
    const result = await userApi.page({ pageNum: 1, pageSize: 200, status: 1 });
    userOptions.value = result.list.filter(user => user.userId);
  } catch (error) {
    userOptions.value = [];
  } finally {
    userLoading.value = false;
  }
}

function handleUserSelectVisible(visible: boolean): void {
  if (visible && userOptions.value.length === 0) {
    void loadUserOptions();
  }
}

function handleSearch(): void {
  pager.page = 1;
  void loadPages();
}

function handleReset(): void {
  query.keyword = '';
  query.userId = '';
  query.enabled = undefined;
  pager.page = 1;
  queryFormRef.value?.clearValidate();
  void loadPages();
}

function handlePageSizeChange(): void {
  pager.page = 1;
  void loadPages();
}

function canDeletePage(row: HomePageVO): boolean {
  return Boolean(row.id && !row.builtIn && !row.readOnly && !row.templateId);
}

function canEditPage(row: HomePageVO): boolean {
  return canDeletePage(row) && row.enabled;
}

function isPageSelected(row: HomePageVO): boolean {
  return row.id !== undefined && row.id !== null && selectedPageIds.value.includes(String(row.id));
}

function togglePage(row: HomePageVO, checked: boolean): void {
  if (!row.id || !canDeletePage(row)) {
    return;
  }
  const id = String(row.id);
  if (checked && !selectedPageIds.value.includes(id)) {
    selectedPageIds.value = [...selectedPageIds.value, id];
    return;
  }
  if (!checked) {
    selectedPageIds.value = selectedPageIds.value.filter(item => item !== id);
  }
}

function toggleAllPages(checked: boolean): void {
  selectedPageIds.value = checked
    ? selectablePages.value.map(row => String(row.id)).filter(Boolean)
    : [];
}

function openPreview(row: HomePageVO): void {
  preview.userId = row.userId ? String(row.userId) : '';
  preview.layoutJson = row.layoutJson || '';
  preview.visible = true;
}

function openEditor(row: HomePageVO): void {
  if (!canEditPage(row)) {
    ElMessage.warning('当前首页不可编辑');
    return;
  }
  editor.form.id = String(row.id);
  editor.form.userId = row.userId ? String(row.userId) : '';
  editor.form.name = row.name || '';
  editorItems.value = resolveLayoutItems(row.layoutJson);
  editor.visible = true;
}

function closeEditor(): void {
  if (saving.value) {
    return;
  }
  editor.visible = false;
  resetEditor();
}

function resetEditor(): void {
  editor.form.id = '';
  editor.form.userId = '';
  editor.form.name = '';
  editorItems.value = [];
  editorFormRef.value?.clearValidate();
}

async function savePage(): Promise<void> {
  const valid = await editorFormRef.value?.validate().catch(() => false);
  if (!valid || !editor.form.id) {
    return;
  }
  saving.value = true;
  try {
    const layoutJson = stringifyLayout(editorItems.value);
    await homePageApi.adminRename(editor.form.id, { name: editor.form.name.trim() });
    await homePageApi.adminSaveLayout(editor.form.id, { layoutJson });
    ElMessage.success('首页已保存');
    editor.visible = false;
    resetEditor();
    await loadPages();
  } finally {
    saving.value = false;
  }
}

async function deletePage(row: HomePageVO): Promise<void> {
  if (!row.id || !canDeletePage(row)) {
    return;
  }
  await ElMessageBox.confirm(`确认删除首页「${row.name}」？`, '删除首页', {
    type: 'warning',
    confirmButtonText: '确认删除',
    confirmButtonClass: 'el-button--danger',
  });
  await homePageApi.adminDelete(String(row.id));
  ElMessage.success('首页已删除');
  await loadPages();
}

async function deleteSelectedPages(): Promise<void> {
  const ids = selectedPages.value.map(row => row.id).filter((id): id is string | number => id !== undefined && id !== null);
  if (ids.length === 0) {
    return;
  }
  await ElMessageBox.confirm(`确认删除已选 ${ids.length} 个首页？`, '批量删除首页', {
    type: 'warning',
    confirmButtonText: '确认删除',
    confirmButtonClass: 'el-button--danger',
  });
  await homePageApi.adminBatchDelete(ids);
  ElMessage.success('已批量删除');
  await loadPages();
}

function normalizeText(value: string): string | undefined {
  const text = value.trim();
  return text || undefined;
}

function normalizePageResult(response: UserHomePageResponse | undefined | null): UserHomePageResult {
  if (!response || typeof response !== 'object') {
    return emptyPageResult();
  }
  if ('list' in response && Array.isArray(response.list)) {
    return response;
  }
  const firstData = response.data;
  if (firstData && typeof firstData === 'object' && 'list' in firstData && Array.isArray(firstData.list)) {
    return firstData;
  }
  const nestedData = firstData && typeof firstData === 'object' && 'data' in firstData ? firstData.data : undefined;
  if (nestedData && typeof nestedData === 'object' && Array.isArray(nestedData.list)) {
    return nestedData;
  }
  return emptyPageResult();
}

function emptyPageResult(): UserHomePageResult {
  return { list: [], total: 0, page: pager.page, size: pager.size };
}

function resolveLayoutItems(layoutJson?: string | null): GridLayoutItem[] {
  const parsed = parseGridLayoutValue(PAGE_CODE, layoutJson);
  return parsed?.items || [];
}

function stringifyLayout(items: GridLayoutItem[]): string {
  return JSON.stringify({ schemaVersion: 1, items });
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function formatUserDisplay(userId?: string | number): string {
  if (userId === undefined || userId === null) {
    return '-';
  }
  const selected = userOptions.value.find(user => String(user.userId) === String(userId));
  return selected ? `${formatUserName(selected)}（${userId}）` : String(userId);
}

function formatUserName(user: IdentityUserVO): string {
  return user.nickname || user.memberName || user.username || String(user.userId || '-');
}

function formatUserOption(user: IdentityUserVO): string {
  const name = formatUserName(user);
  return user.username && user.username !== name ? `${name}（${user.username}）` : name;
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
.home-list-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
}

.home-list-error {
  margin-bottom: 12px;
}

.home-job-search-actions {
  margin-left: auto;
}

.home-list-user-filter {
  min-width: 360px;
}

.home-list-user-filter :deep(.el-select) {
  width: 360px;
}

.home-list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.home-list-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.home-list-name strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-list-name span {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-list-preview {
  min-height: 520px;
  padding: 4px;
  background: var(--el-bg-color-page);
}

.home-list-designer {
  min-height: 560px;
}

@media (max-width: 768px) {
  .home-list-user-filter,
  .home-list-user-filter :deep(.el-select) {
    width: 100%;
    min-width: 0;
  }

  .home-list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
