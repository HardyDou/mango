<template>
  <MangoListPage class="micro-application-page" data-page="system.micro-application">
    <template #search>
      <MangoSearchPanel :model="query" :columns="2" @search="applySearch" @reset="resetSearch">
        <el-form-item label="微应用">
          <el-input
            v-model="query.keyword"
            clearable
            data-field="micro-application.keyword"
            placeholder="名称或运行单元编码"
            @keyup.enter="applySearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-alert
          class="micro-application-page__notice"
          type="info"
          :closable="false"
          show-icon
          title="微应用由部署资源声明登记，页面只提供运行单元总览；模块是否需要微前端在“逻辑应用 → 模块”中配置。可达性只检查静态入口能否访问，不校验页面内容和版本。"
        />
      </template>

      <el-table v-loading="loading" :data="filteredApps" stripe empty-text="暂无微应用，当前部署可能使用单体前端">
        <el-table-column prop="appName" label="微应用" min-width="200">
          <template #default="{ row }">
            <div>{{ row.appName }}</div>
            <div class="code-text ellipsis" :title="row.appCode" data-field="micro-application.runtime-code">
              {{ row.appCode }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="入口与健康检查" min-width="220">
          <template #default="{ row }">
            <div class="code-text ellipsis" :title="row.entryUrl || ''">{{ row.entryUrl || '-' }}</div>
            <div class="muted ellipsis" :title="row.healthCheckUrl || ''">
              探测：{{ row.healthCheckUrl || '未配置，回落到入口' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="运行配置" width="150">
          <template #default="{ row }">
            <div class="muted">{{ row.framework || '-' }} · {{ row.version || '未标注版本' }}</div>
            <div class="muted">{{ deployModeLabel(row.deployMode) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="被引用模块" width="110">
          <template #default="{ row }">
            <el-popover v-if="referenceCount(row.appCode)" placement="left" width="320" trigger="click">
              <template #reference>
                <el-tag type="success" size="small" class="reference-tag" data-action="micro-application.references">
                  {{ referenceCount(row.appCode) }} 个
                </el-tag>
              </template>
              <div class="reference-list">
                <div v-for="item in referencesOf(row.appCode)" :key="`${item.appCode}:${item.moduleCode}`">
                  {{ item.appCode }} / {{ item.moduleCode }}（{{ item.deployProfile }}）
                </div>
              </div>
            </el-popover>
            <span v-else class="muted">未引用</span>
          </template>
        </el-table-column>
        <el-table-column label="可达性" width="130">
          <template #default="{ row }">
            <el-tag
              v-if="healthResults[row.appCode]"
              :type="healthResults[row.appCode].success ? 'success' : 'danger'"
              size="small"
              :title="healthResults[row.appCode].message || ''"
            >
              {{ healthResults[row.appCode].success ? '可达' : '不可达' }}
              <template v-if="healthResults[row.appCode].elapsedMs !== undefined">
                · {{ healthResults[row.appCode].elapsedMs }}ms
              </template>
            </el-tag>
            <span v-else class="muted">未检测</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{
              row.status === 1 ? '启用' : '停用'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :loading="healthLoading[row.appCode]"
              data-action="micro-application.check-health"
              @click="checkHealth(row)"
              >检测</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </MangoListPanel>
  </MangoListPage>
</template>

<script setup lang="ts" name="SystemMicroApplication">
import { MangoListPage, MangoListPanel, MangoSearchPanel } from '@mango/common';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  appApi,
  appModuleApi,
  frontendAppApi,
  type AppModuleRuntimeStrategy,
  type AuthorizationApp,
  type FrontendAppHealthCheckResult,
} from '../../api/app';

const DEPLOY_PROFILES = ['monolith', 'hybrid', 'micro'];

const loading = ref(false);
const tableData = ref<AuthorizationApp[]>([]);
const strategies = ref<AppModuleRuntimeStrategy[]>([]);
const query = reactive({ keyword: '', status: undefined as number | undefined });
const appliedQuery = reactive({ ...query });

const microApps = computed(() => tableData.value.filter((app) => app.appType === 'MICRO_APP'));

const filteredApps = computed(() => {
  const keyword = appliedQuery.keyword.trim().toLowerCase();
  return microApps.value.filter((app) => {
    const matchesKeyword =
      !keyword ||
      (app.appName || '').toLowerCase().includes(keyword) ||
      (app.appCode || '').toLowerCase().includes(keyword);
    const matchesStatus = appliedQuery.status === undefined || app.status === appliedQuery.status;
    return matchesKeyword && matchesStatus;
  });
});

const referencesMap = computed(() => {
  const map = new Map<string, AppModuleRuntimeStrategy[]>();
  strategies.value
    .filter((item) => item.pageType === 'MICRO_ROUTE' && item.runtimeCode)
    .forEach((item) => {
      const code = item.runtimeCode as string;
      const list = map.get(code) || [];
      list.push(item);
      map.set(code, list);
    });
  return map;
});

const referenceCounts = computed(() => {
  const counts = new Map<string, number>();
  strategies.value
    .filter((item) => item.pageType === 'MICRO_ROUTE' && item.runtimeCode)
    .forEach((item) => {
      const code = item.runtimeCode as string;
      counts.set(code, (counts.get(code) || 0) + 1);
    });
  return counts;
});

const healthResults = reactive<Record<string, FrontendAppHealthCheckResult>>({});
const healthLoading = reactive<Record<string, boolean>>({});

function referencesOf(code?: string) {
  if (!code) {
    return [];
  }
  return referencesMap.value.get(code) || [];
}

async function checkHealth(row: AuthorizationApp) {
  if (!row.appCode) {
    return;
  }
  healthLoading[row.appCode] = true;
  try {
    const result = await frontendAppApi.checkHealth(row.appCode);
    healthResults[row.appCode] = result;
    if (result.success) {
      ElMessage.success(`${row.appName} 入口可达（${result.statusCode}，${result.elapsedMs}ms）`);
    } else {
      ElMessage.warning(`${row.appName} 入口不可达：${result.message || '未知原因'}`);
    }
  } finally {
    healthLoading[row.appCode] = false;
  }
}

function referenceCount(code?: string) {
  if (!code) {
    return 0;
  }
  return referenceCounts.value.get(code) || 0;
}

function deployModeLabel(value?: string) {
  if (value === 'REMOTE') return '远程';
  if (value === 'HYBRID') return '混合';
  if (value === 'EMBEDDED') return '内置';
  return value || '-';
}

function applySearch() {
  Object.assign(appliedQuery, query);
}

function resetSearch() {
  query.keyword = '';
  query.status = undefined;
  applySearch();
}

async function loadData() {
  loading.value = true;
  try {
    const apps = (await appApi.list()) || [];
    tableData.value = apps;
    const hosts = apps.filter((app) => app.appType !== 'MICRO_APP');
    const strategyGroups = await Promise.all(
      hosts.flatMap((host) =>
        DEPLOY_PROFILES.map((deployProfile) =>
          appModuleApi.listRuntimeStrategies({ appCode: host.appCode, deployProfile }),
        ),
      ),
    );
    strategies.value = strategyGroups.flat().filter(Boolean);
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped lang="scss">
.micro-application-page {
  min-width: 0;

  &__notice {
    width: 100%;
  }

  .code-text {
    font-family: SFMono-Regular, Consolas, monospace;
    font-size: 12px;
    color: var(--el-text-color-regular);
  }

  .ellipsis {
    overflow: hidden;
    max-width: 100%;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .muted {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .reference-tag {
    cursor: pointer;
  }

  .reference-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 12px;
    line-height: 1.5;
  }
}
</style>
