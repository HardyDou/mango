<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>告警规则</h2>
          <p>维护失败实例告警规则，触发后提交到 mango-notice 的消息模板和收件人规则。</p>
        </div>
        <el-button v-auth="'job:alarm:add'" type="primary" :icon="Plus" @click="openEditor()">新增规则</el-button>
      </div>

      <el-form :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="关键字" class="job-search-item job-search-item-wide">
          <el-input v-model="query.keyword" clearable placeholder="规则/场景/模板" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="应用" class="job-search-item">
          <el-input v-model="query.appCode" clearable placeholder="appCode" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="任务" class="job-search-item job-search-item-wide">
          <el-select
            v-model="query.jobId"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchJobs"
            :loading="jobLoading"
            placeholder="全部任务"
          >
            <el-option v-for="item in jobOptions" :key="item.id" :label="jobOptionLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="job-search-item job-search-item-small">
          <el-select v-model="query.enabled" clearable placeholder="全部">
            <el-option v-for="item in enabledOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="job-search-actions">
          <el-button v-auth="'job:alarm:list'" type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="job-panel">
      <div class="job-panel-head">
        <div>
          <h3>规则列表</h3>
          <p>任务级规则优先匹配指定任务；应用级默认规则在同应用失败实例上生效。</p>
        </div>
        <el-button v-auth="'job:alarm:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
      </div>

      <el-alert v-if="errorMessage" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无告警规则">
        <el-table-column label="规则" min-width="230" fixed="left">
          <template #default="{ row }">
            <div class="job-name-cell">
              <strong>{{ row.ruleName }}</strong>
              <span>{{ optionLabel(alarmTypeOptions, row.alarmType) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="appCode" label="应用" min-width="130" show-overflow-tooltip />
        <el-table-column label="作用对象" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="job-name-cell">
              <strong>{{ row.jobName || '应用级默认' }}</strong>
              <span>{{ row.jobCode || '全部失败实例' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="noticeSceneCode" label="通知场景" min-width="170" show-overflow-tooltip />
        <el-table-column prop="noticeTemplateCode" label="通知模板" min-width="190" show-overflow-tooltip />
        <el-table-column label="启用" width="88">
          <template #default="{ row }">
            <el-tag :type="booleanOptionTagType(enabledOptions, row.enabled)" size="small">
              {{ booleanOptionLabel(enabledOptions, row.enabled) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="job-actions">
              <el-button v-auth="'job:alarm:edit'" link type="primary" :icon="Edit" @click="openEditor(row)">编辑</el-button>
              <el-button v-auth="'job:alarm:status'" link type="primary" @click="toggleEnabled(row)">
                {{ row.enabled ? '停用' : '启用' }}
              </el-button>
              <el-button v-auth="'job:alarm:delete'" link type="danger" :icon="Delete" @click="deleteRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="job-pagination">
        <Pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadRows" />
      </div>
    </section>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑告警规则' : '新增告警规则'" width="760px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="118px">
        <div class="job-form-section-title">匹配范围</div>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="所属应用" prop="appCode">
              <el-input v-model="form.appCode" placeholder="例如 internal-admin" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作用任务">
              <el-select
                v-model="form.jobId"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchJobs"
                :loading="jobLoading"
                placeholder="为空表示应用级默认"
                style="width: 100%"
              >
                <el-option v-for="item in jobOptions" :key="item.id" :label="jobOptionLabel(item)" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="例如 核心任务失败告警" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="告警类型" prop="alarmType">
              <el-select v-model="form.alarmType" disabled style="width: 100%">
                <el-option v-for="item in alarmTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="job-form-section-title">通知配置</div>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="通知场景" prop="noticeSceneCode">
              <el-input v-model="form.noticeSceneCode" placeholder="mango-notice bizType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消息模板" prop="noticeTemplateCode">
              <el-input v-model="form.noticeTemplateCode" placeholder="noticeTemplateCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收件规则">
              <el-input v-model="form.recipientRuleCode" clearable placeholder="recipientRuleCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单个用户">
              <el-input v-model="form.userId" clearable placeholder="用户 ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="多个用户">
              <el-input v-model="form.userIdsText" type="textarea" :rows="3" placeholder="多个用户 ID 使用逗号或换行分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  alarmTypeOptions,
  booleanOptionLabel,
  booleanOptionTagType,
  enabledOptions,
  jobApi,
  optionLabel,
  requestErrorMessage,
  type ApiId,
  type JobAlarmRule,
  type JobAlarmRuleQuery,
  type JobAlarmType,
  type JobDefinition,
  type SaveJobAlarmRulePayload,
} from '../../api/job';
import '../job-admin.css';

interface AlarmRuleForm {
  id?: ApiId;
  appCode: string;
  jobId?: ApiId | '';
  ruleName: string;
  alarmType: JobAlarmType;
  noticeSceneCode: string;
  noticeTemplateCode: string;
  recipientRuleCode: string;
  userId: string;
  userIdsText: string;
  enabled: boolean;
}

interface NoticeParams {
  recipientRuleCode?: string;
  userId?: string | number;
  userIds?: Array<string | number>;
}

const query = reactive<JobAlarmRuleQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  appCode: '',
  jobId: '',
  alarmType: '',
  enabled: '',
});

const rows = ref<JobAlarmRule[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const editorVisible = ref(false);
const formRef = ref<FormInstance>();
const jobLoading = ref(false);
const jobOptions = ref<JobDefinition[]>([]);

const form = reactive<AlarmRuleForm>(defaultForm());

const rules: FormRules<AlarmRuleForm> = {
  appCode: [{ required: true, message: '请输入所属应用', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  alarmType: [{ required: true, message: '请选择告警类型', trigger: 'change' }],
  noticeSceneCode: [{ required: true, message: '请输入通知场景', trigger: 'blur' }],
  noticeTemplateCode: [{ required: true, message: '请输入消息模板', trigger: 'blur' }],
};

function defaultForm(): AlarmRuleForm {
  return {
    appCode: 'internal-admin',
    jobId: '',
    ruleName: '',
    alarmType: 'INSTANCE_FAILED',
    noticeSceneCode: 'MANGO_JOB_FAILED',
    noticeTemplateCode: 'MANGO_JOB_FAILED_TEMPLATE',
    recipientRuleCode: '',
    userId: '',
    userIdsText: '',
    enabled: true,
  };
}

function resetForm() {
  Object.assign(form, defaultForm());
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await jobApi.pageAlarmRules(query);
    rows.value = page.list;
    total.value = page.total;
    await ensureJobOptions(page.list);
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '告警规则加载失败');
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    appCode: '',
    jobId: '',
    alarmType: '',
    enabled: '',
  });
  loadRows();
}

async function searchJobs(keyword = '') {
  jobLoading.value = true;
  try {
    const page = await jobApi.pageDefinitions({
      pageNum: 1,
      pageSize: 20,
      keyword,
      appCode: query.appCode || form.appCode || undefined,
    });
    jobOptions.value = mergeJobOptions(jobOptions.value, page.list);
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '任务列表加载失败'));
  } finally {
    jobLoading.value = false;
  }
}

async function ensureJobOptions(alarmRules: JobAlarmRule[]) {
  const missing = alarmRules.some(rule => rule.jobId && !jobOptions.value.some(item => item.id === rule.jobId));
  if (missing || jobOptions.value.length === 0) {
    await searchJobs('');
  }
}

function mergeJobOptions(current: JobDefinition[], incoming: JobDefinition[]) {
  const map = new Map<ApiId, JobDefinition>();
  current.forEach(item => {
    if (item.id) {
      map.set(item.id, item);
    }
  });
  incoming.forEach(item => {
    if (item.id) {
      map.set(item.id, item);
    }
  });
  return Array.from(map.values());
}

function jobOptionLabel(job: JobDefinition) {
  return `${job.jobName || job.jobCode} / ${job.jobCode}`;
}

async function openEditor(row?: JobAlarmRule) {
  resetForm();
  await searchJobs('');
  if (row?.id) {
    try {
      const detail = await jobApi.detailAlarmRule(row.id);
      assignForm(detail);
    } catch (error) {
      ElMessage.error(requestErrorMessage(error, '告警规则详情加载失败'));
      return;
    }
  }
  editorVisible.value = true;
}

function assignForm(row: JobAlarmRule) {
  const noticeParams = parseNoticeParams(row.noticeParams);
  Object.assign(form, {
    id: row.id,
    appCode: row.appCode,
    jobId: row.jobId || '',
    ruleName: row.ruleName,
    alarmType: row.alarmType,
    noticeSceneCode: row.noticeSceneCode,
    noticeTemplateCode: row.noticeTemplateCode,
    recipientRuleCode: noticeParams.recipientRuleCode || '',
    userId: noticeParams.userId === undefined ? '' : String(noticeParams.userId),
    userIdsText: Array.isArray(noticeParams.userIds) ? noticeParams.userIds.join('\n') : '',
    enabled: row.enabled !== false,
  });
}

function parseNoticeParams(value?: string): NoticeParams {
  if (!value) {
    return {};
  }
  try {
    const parsed = JSON.parse(value) as unknown;
    if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
      return parsed as NoticeParams;
    }
  } catch {
    ElMessage.warning('通知参数 JSON 无法解析，请重新保存结构化字段');
  }
  return {};
}

async function saveRow() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = buildPayload();
    if (payload.id) {
      await jobApi.updateAlarmRule(payload);
      ElMessage.success('告警规则已更新');
    } else {
      await jobApi.createAlarmRule(payload);
      ElMessage.success('告警规则已创建');
    }
    editorVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '告警规则保存失败'));
  } finally {
    saving.value = false;
  }
}

function buildPayload(): SaveJobAlarmRulePayload {
  return {
    id: form.id,
    jobId: form.jobId || undefined,
    appCode: form.appCode.trim(),
    ruleName: form.ruleName.trim(),
    alarmType: form.alarmType,
    triggerCondition: '{"status":"FAILED"}',
    noticeSceneCode: form.noticeSceneCode.trim(),
    noticeTemplateCode: form.noticeTemplateCode.trim(),
    noticeParams: buildNoticeParams(),
    enabled: form.enabled,
  };
}

function buildNoticeParams() {
  const params: NoticeParams = {};
  if (form.recipientRuleCode.trim()) {
    params.recipientRuleCode = form.recipientRuleCode.trim();
  }
  if (form.userId.trim()) {
    params.userId = normalizeId(form.userId.trim());
  }
  const userIds = form.userIdsText
    .split(/[\n,，]+/)
    .map(item => item.trim())
    .filter(Boolean)
    .map(normalizeId);
  if (userIds.length > 0) {
    params.userIds = userIds;
  }
  return Object.keys(params).length > 0 ? JSON.stringify(params) : undefined;
}

function normalizeId(value: string) {
  return /^\d+$/.test(value) ? Number(value) : value;
}

async function toggleEnabled(row: JobAlarmRule) {
  if (!row.id) {
    return;
  }
  const nextEnabled = !row.enabled;
  await ElMessageBox.confirm(
    `确认${nextEnabled ? '启用' : '停用'}告警规则「${row.ruleName}」？`,
    '更新告警规则状态',
    { type: 'warning' },
  );
  try {
    await jobApi.updateAlarmRuleStatus({ id: row.id, enabled: nextEnabled });
    ElMessage.success(nextEnabled ? '告警规则已启用' : '告警规则已停用');
    await loadRows();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '告警规则状态更新失败'));
  }
}

async function deleteRow(row: JobAlarmRule) {
  if (!row.id) {
    return;
  }
  await ElMessageBox.confirm(`确认删除告警规则「${row.ruleName}」？删除后失败实例不再匹配该规则。`, '删除告警规则', {
    type: 'warning',
  });
  try {
    await jobApi.deleteAlarmRule(row.id);
    ElMessage.success('告警规则已删除');
    await loadRows();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '告警规则删除失败'));
  }
}

onMounted(() => {
  loadRows();
  searchJobs('');
});
</script>
