<template>
  <section class="home-admin-page" data-page="home.template" :data-state="pageState">
    <section class="job-toolbar" data-surface="home.template.search">
      <div class="job-toolbar-head">
        <div>
          <h2>首页模板</h2>
          <p>维护首页模板、发布版本和授权范围。</p>
        </div>
        <el-button
          v-auth="'home:templates:add'"
          type="primary"
          :icon="Plus"
          data-action="home.template.create"
          @click="openTemplateEditor()"
        >
          新建模板
        </el-button>
      </div>

      <el-form ref="queryFormRef" :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="模板名称" prop="keyword" class="job-search-item job-search-item-wide">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="请输入模板名称"
            data-field="home.template.keyword"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" prop="enabled" class="job-search-item job-search-item-small">
          <el-select v-model="query.enabled" clearable placeholder="全部" data-field="home.template.enabled">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item class="job-search-actions home-job-search-actions">
          <el-button
            v-auth="'home:templates:list'"
            type="primary"
            :icon="Search"
            :loading="loading"
            data-action="home.template.search"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button :icon="RefreshLeft" data-action="home.template.reset" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="job-panel" data-surface="home.template.list">
      <el-alert v-if="errorMessage" class="home-admin-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" data-action="home.template.retry" @click="loadTemplates">重试</el-button>
        </template>
      </el-alert>

      <el-table
        v-loading="loading"
        :data="pagedTemplates"
        row-key="id"
        stripe
        data-surface="home.template.table"
      >
        <template #empty>
          <el-empty :description="emptyDescription" />
        </template>
        <el-table-column label="模板名称" min-width="240" fixed="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="home-template-name" :data-record-key="`home-template:${row.id}`">
              <strong>{{ row.name }}</strong>
              <span>{{ row.id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="120">
          <template #default="{ row }">
            <span>{{ formatVersion(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="授权数" width="96">
          <template #default="{ row }">{{ row.authorizationCount ?? 0 }}</template>
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
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="home-admin-actions">
              <el-button
                v-auth="'home:templates:edit'"
                link
                type="primary"
                :disabled="!canEditDraft(row)"
                data-action="home.template.edit"
                :data-record-key="`home-template:${row.id}`"
                @click="openTemplateEditor(row)"
              >
                编辑
              </el-button>
              <el-button
                v-auth="'home:templates:add'"
                link
                type="primary"
                data-action="home.template.copy"
                :data-record-key="`home-template:${row.id}`"
                @click="copyTemplate(row)"
              >
                复制
              </el-button>
              <el-dropdown trigger="click" @command="handleRowCommand(row, $event)">
                <el-button link type="primary" data-action="home.template.more" :data-record-key="`home-template:${row.id}`">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-auth="'home:templates:publish'"
                      command="publish"
                      :disabled="!canPublish(row)"
                      data-action="home.template.publish"
                    >
                      发布
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-auth="'home:templates:auth'"
                      command="auth"
                      :disabled="!row.activeVersionId"
                      data-action="home.template.auth"
                    >
                      授权
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-auth="'home:templates:status'"
                      command="status"
                      data-action="home.template.status"
                    >
                      {{ row.enabled ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-auth="'home:templates:delete'"
                      divided
                      command="delete"
                      data-action="home.template.delete"
                    >
                      <span class="home-admin-danger">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="job-pagination">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="filteredTemplates.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          data-surface="home.template.pagination"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="editor.visible"
      :title="editor.form.id ? '编辑模板草稿' : '新建模板'"
      width="1180px"
      destroy-on-close
      append-to-body
      :close-on-click-modal="false"
      :before-close="beforeCloseEditor"
      @closed="resetEditor"
    >
      <el-form
        ref="templateFormRef"
        :model="editor.form"
        :rules="templateRules"
        label-width="96px"
        data-surface="home.template.form"
      >
        <div class="home-admin-form-section">基本信息</div>
        <el-row :gutter="14">
          <el-col :xs="24" :md="12">
            <el-form-item label="模板名称" prop="name">
              <el-input
                v-model="editor.form.name"
                maxlength="64"
                show-word-limit
                clearable
                placeholder="请输入模板名称"
                data-field="home.template.name"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="home-admin-form-section">布局配置</div>
        <div class="home-template-editor">
          <div class="home-template-editor__designer" data-surface="home.template.designer">
            <MangoGridDesigner
              v-model="draftItems"
              :widgets="templateWidgets"
              :default-width="3"
              :default-height="10"
              :row-height="15"
              :gap="15"
            />
          </div>
          <div class="home-template-editor__preview" data-surface="home.template.preview">
            <MangoGridLayout :items="draftItems" :widgets="templateWidgets" :row-height="15" :gap="15" />
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="closeEditor">取消</el-button>
        <el-button type="primary" :loading="saving" data-action="home.template.save" @click="saveTemplateDraft">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="authorization.visible"
      title="模板授权"
      width="1040px"
      destroy-on-close
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="home-admin-toolbar home-admin-toolbar--dialog" data-surface="home.template.auth-toolbar">
        <div class="home-admin-toolbar__left">
          <el-button plain data-action="home.template.auth.add-user" @click="addAuthorization('USER')">添加个人</el-button>
          <el-button plain data-action="home.template.auth.add-org" @click="addAuthorization('ORG')">添加部门</el-button>
          <el-button plain data-action="home.template.auth.add-role" @click="addAuthorization('ROLE')">添加角色</el-button>
        </div>
      </div>
      <el-table :data="authorization.rows" row-key="clientId" data-surface="home.template.auth-table">
        <template #empty>
          <el-empty description="暂无授权对象" />
        </template>
        <el-table-column label="授权类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.subjectType" data-field="home.template.auth.subject-type">
              <el-option label="个人" value="USER" />
              <el-option label="部门" value="ORG" />
              <el-option label="角色" value="ROLE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="对象 ID" width="180">
          <template #default="{ row }">
            <el-input
              v-model="row.subjectId"
              :disabled="row.subjectType === 'ROLE'"
              placeholder="用户或部门 ID"
              data-field="home.template.auth.subject-id"
            />
          </template>
        </el-table-column>
        <el-table-column label="角色编码" width="180">
          <template #default="{ row }">
            <el-input
              v-model="row.subjectCode"
              :disabled="row.subjectType !== 'ROLE'"
              placeholder="例如 ROLE_ADMIN"
              data-field="home.template.auth.subject-code"
            />
          </template>
        </el-table-column>
        <el-table-column label="显示名称" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.subjectName" placeholder="授权来源展示名称" data-field="home.template.auth.subject-name" />
          </template>
        </el-table-column>
        <el-table-column label="默认" width="86">
          <template #default="{ row }">
            <el-switch v-model="row.defaultFlag" data-field="home.template.auth.default" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.sort" :min="0" :step="10" controls-position="right" data-field="home.template.auth.sort" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="88" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" data-action="home.template.auth.remove" @click="authorization.rows.splice($index, 1)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button :disabled="saving" @click="authorization.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" data-action="home.template.auth.save" @click="saveAuthorizations">保存授权</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts" name="HomeTemplatePage">
import { computed, onMounted, reactive, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, type LocationQueryRaw } from 'vue-router';
import { ArrowDown, Plus, RefreshLeft, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  homeTemplateApi,
  type HomeTemplateAuthorizationItem,
  type HomeTemplateAuthorizationSubjectType,
  type HomeTemplateVO,
} from '@mango/home';
import { MangoGridDesigner, MangoGridLayout, parseGridLayoutValue, type GridLayoutItem } from '@mango/grid-layout';
import { mergeGridWidgets, systemGridWidgets } from '@mango/grid-widgets';
import type { MangoWidgetNavigateTarget, MangoWidgetRuntimeContext } from '@mango/grid-widgets';
import { ensureFeatureRegistrars } from '../../../runtime/featureRegistrars';
import { useMangoAdminHomeWidgets } from '../../../runtime/homeWidgets';
import { useRoutesList } from '../../../stores/routesList';
import { useUserInfo } from '../../../stores/userInfo';
import '../../../../../job/src/views/job-admin.css';

type TemplateRowCommand = 'publish' | 'auth' | 'status' | 'delete';
type AuthorizationRow = HomeTemplateAuthorizationItem & { clientId: string };

const PAGE_CODE = 'admin-home-template';

const router = useRouter();
const userInfo = useUserInfo();
const routesListStore = useRoutesList();
const { routesList } = storeToRefs(routesListStore);
const businessHomeWidgets = useMangoAdminHomeWidgets();
const queryFormRef = ref<FormInstance>();
const templateFormRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const loaded = ref(false);
const errorMessage = ref('');
const templates = ref<HomeTemplateVO[]>([]);
const draftItems = ref<GridLayoutItem[]>(defaultLayoutItems());
const query = reactive<{ keyword: string; enabled?: boolean }>({
  keyword: '',
  enabled: undefined,
});
const pager = reactive({
  page: 1,
  size: 20,
});
const editor = reactive({
  visible: false,
  form: {
    id: '',
    name: '',
  },
});
const authorization = reactive({
  visible: false,
  templateId: '',
  rows: [] as AuthorizationRow[],
});

const templateRules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { max: 64, message: '模板名称不能超过 64 个字符', trigger: 'blur' },
  ],
};
const pageState = computed(() => {
  if (errorMessage.value) return 'error';
  if (loading.value) return 'loading';
  if (!loaded.value) return 'idle';
  return filteredTemplates.value.length > 0 ? 'ready' : 'empty';
});
const hasQuery = computed(() => Boolean(query.keyword.trim() || query.enabled !== undefined));
const emptyDescription = computed(() => hasQuery.value ? '暂无符合条件的首页模板' : '暂无首页模板');
const filteredTemplates = computed(() => {
  const keyword = query.keyword.trim().toLowerCase();
  return templates.value.filter(item => {
    const matchedKeyword = !keyword || item.name.toLowerCase().includes(keyword) || String(item.id || '').includes(keyword);
    const matchedStatus = query.enabled === undefined || item.enabled === query.enabled;
    return matchedKeyword && matchedStatus;
  });
});
const pagedTemplates = computed(() => {
  const start = (pager.page - 1) * pager.size;
  return filteredTemplates.value.slice(start, start + pager.size);
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
const templateWidgets = computed(() => mergeGridWidgets({
  runtime: widgetRuntime.value,
  systemWidgets: systemGridWidgets,
  businessWidgets: businessHomeWidgets.value,
}));

onMounted(initializePage);

async function initializePage(): Promise<void> {
  try {
    await ensureFeatureRegistrars();
  } catch (error) {
    console.error('[mango-shell] failed to register shell features', error);
  }
  await loadTemplates();
}

async function loadTemplates(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    templates.value = await homeTemplateApi.list();
    loaded.value = true;
    normalizeCurrentPage();
  } catch (error) {
    errorMessage.value = '首页模板加载失败，请稍后重试。';
  } finally {
    loading.value = false;
  }
}

function handleSearch(): void {
  pager.page = 1;
}

function handleReset(): void {
  query.keyword = '';
  query.enabled = undefined;
  pager.page = 1;
  queryFormRef.value?.clearValidate();
}

function handlePageChange(): void {
  normalizeCurrentPage();
}

function handlePageSizeChange(): void {
  pager.page = 1;
}

async function openTemplateEditor(row?: HomeTemplateVO): Promise<void> {
  if (row && !canEditDraft(row)) {
    ElMessage.warning('已发布模板不可直接修改，请先复制为新草稿。');
    return;
  }
  editor.form.id = row?.id ? String(row.id) : '';
  editor.form.name = row?.name || '';
  draftItems.value = row ? resolveLayoutItems(row.draftLayoutJson || row.activeLayoutJson) : defaultLayoutItems();
  if (row?.id) {
    try {
      const detail = await homeTemplateApi.detail(String(row.id));
      editor.form.name = detail.name;
      draftItems.value = resolveLayoutItems(detail.draftLayoutJson || detail.activeLayoutJson);
    } catch (error) {
      ElMessage.error('模板详情加载失败');
      return;
    }
  }
  editor.visible = true;
}

function closeEditor(): void {
  editor.visible = false;
}

function beforeCloseEditor(done: () => void): void {
  if (!saving.value) {
    done();
  }
}

function resetEditor(): void {
  editor.form.id = '';
  editor.form.name = '';
  draftItems.value = defaultLayoutItems();
  templateFormRef.value?.clearValidate();
}

async function saveTemplateDraft(): Promise<void> {
  const valid = await templateFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    const layoutJson = stringifyLayout(draftItems.value);
    if (editor.form.id) {
      await homeTemplateApi.updateDraft(editor.form.id, {
        name: editor.form.name.trim(),
        layoutJson,
      });
      ElMessage.success('模板草稿已保存');
    } else {
      await homeTemplateApi.create({
        name: editor.form.name.trim(),
        layoutJson,
      });
      ElMessage.success('模板已创建');
    }
    editor.visible = false;
    await loadTemplates();
  } finally {
    saving.value = false;
  }
}

async function copyTemplate(row: HomeTemplateVO): Promise<void> {
  if (!row.id) return;
  await homeTemplateApi.copy(String(row.id));
  ElMessage.success('已复制为新草稿');
  await loadTemplates();
}

async function publishTemplate(row: HomeTemplateVO): Promise<void> {
  if (!row.id) return;
  if (!canPublish(row)) {
    ElMessage.warning('当前模板没有可发布的草稿');
    return;
  }
  await ElMessageBox.confirm(`确认发布模板「${row.name}」？发布后授权用户将看到新版本。`, '发布模板', {
    type: 'warning',
  });
  await homeTemplateApi.publish(String(row.id));
  ElMessage.success('模板已发布');
  await loadTemplates();
}

async function toggleTemplate(row: HomeTemplateVO): Promise<void> {
  if (!row.id) return;
  await homeTemplateApi.updateStatus(String(row.id), !row.enabled);
  ElMessage.success(row.enabled ? '模板已停用' : '模板已启用');
  await loadTemplates();
}

async function deleteTemplate(row: HomeTemplateVO): Promise<void> {
  if (!row.id) return;
  await ElMessageBox.confirm(`确认删除模板「${row.name}」？仅未授权模板允许删除。`, '删除模板', {
    type: 'warning',
    confirmButtonText: '确认删除',
    confirmButtonClass: 'el-button--danger',
  });
  await homeTemplateApi.delete(String(row.id));
  ElMessage.success('模板已删除');
  await loadTemplates();
}

async function handleRowCommand(row: HomeTemplateVO, command: string | number | object): Promise<void> {
  switch (command as TemplateRowCommand) {
    case 'publish':
      await publishTemplate(row);
      break;
    case 'auth':
      await openAuthorizationDialog(row);
      break;
    case 'status':
      await toggleTemplate(row);
      break;
    case 'delete':
      await deleteTemplate(row);
      break;
    default:
      break;
  }
}

async function openAuthorizationDialog(row: HomeTemplateVO): Promise<void> {
  if (!row.id) return;
  authorization.templateId = String(row.id);
  try {
    const rows = await homeTemplateApi.listAuthorizations({ templateId: row.id });
    authorization.rows = rows.map((item, index) => ({
      clientId: `${item.id || row.id}-${index}`,
      subjectType: item.subjectType,
      subjectId: item.subjectId,
      subjectCode: item.subjectCode,
      subjectName: item.subjectName,
      defaultFlag: Boolean(item.defaultFlag),
      sort: item.sort ?? (index + 1) * 10,
    }));
    authorization.visible = true;
  } catch (error) {
    ElMessage.error('模板授权加载失败');
  }
}

function addAuthorization(subjectType: HomeTemplateAuthorizationSubjectType): void {
  authorization.rows.push({
    clientId: `${Date.now()}-${authorization.rows.length}`,
    subjectType,
    defaultFlag: false,
    sort: (authorization.rows.length + 1) * 10,
  });
}

async function saveAuthorizations(): Promise<void> {
  saving.value = true;
  try {
    await homeTemplateApi.saveAuthorizations({
      templateId: authorization.templateId,
      authorizations: authorization.rows.map(row => ({
        subjectType: row.subjectType,
        subjectId: row.subjectType === 'ROLE' ? undefined : normalizeId(row.subjectId),
        subjectCode: row.subjectType === 'ROLE' ? row.subjectCode?.trim() : undefined,
        subjectName: row.subjectName?.trim(),
        defaultFlag: Boolean(row.defaultFlag),
        sort: row.sort,
      })),
    });
    authorization.visible = false;
    ElMessage.success('授权已保存');
    await loadTemplates();
  } finally {
    saving.value = false;
  }
}

function canEditDraft(row: HomeTemplateVO): boolean {
  return Boolean(row.draftVersionId && !row.activeVersionId);
}

function canPublish(row: HomeTemplateVO): boolean {
  return Boolean(row.draftVersionId);
}

function formatVersion(row: HomeTemplateVO): string {
  if (row.activeVersionNo) {
    return `V${row.activeVersionNo}`;
  }
  return row.draftVersionId ? '草稿' : '-';
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function normalizeId(value: unknown): string | undefined {
  const text = String(value || '').trim();
  return text || undefined;
}

function normalizeCurrentPage(): void {
  const maxPage = Math.max(1, Math.ceil(filteredTemplates.value.length / pager.size));
  if (pager.page > maxPage) {
    pager.page = maxPage;
  }
}

function resolveLayoutItems(layoutJson?: string | null): GridLayoutItem[] {
  const parsed = parseGridLayoutValue(PAGE_CODE, layoutJson);
  return parsed?.items?.length ? parsed.items : defaultLayoutItems();
}

function stringifyLayout(items: GridLayoutItem[]): string {
  return JSON.stringify({ schemaVersion: 1, items });
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
.home-admin-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
}

.home-admin-error {
  margin-bottom: 12px;
}

.home-job-search-actions {
  margin-left: auto;
}

.home-admin-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.home-admin-toolbar--dialog {
  margin-bottom: 10px;
}

.home-admin-toolbar__left,
.home-admin-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.home-template-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.home-template-name strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-template-name span {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-admin-danger {
  color: var(--el-color-danger);
}

.home-admin-form-section {
  margin: 2px 0 12px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.home-admin-form-section:not(:first-child) {
  margin-top: 10px;
}

.home-template-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
}

.home-template-editor__designer,
.home-template-editor__preview {
  min-height: 520px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: auto;
  background: var(--el-fill-color-extra-light);
}

.home-template-editor__preview {
  padding: 10px;
}

@media (max-width: 900px) {
  .home-template-editor {
    grid-template-columns: 1fr;
  }

  .home-template-editor__preview {
    min-height: 320px;
  }
}
</style>
