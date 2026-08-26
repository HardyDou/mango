<template>
  <MangoListPage class="mango-ai-config-page" data-page="ai.prompts">
    <template #search>
      <MangoSearchPanel :model="query" @search="handleSearch" @reset="handleReset">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="名称或编码" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel data-surface="ai.prompts.list">
      <template #actions>
        <el-button v-auth="'ai:prompt:add'" type="primary" plain data-action="ai.prompts.add" @click="openPrompt()">
          新增提示词
        </el-button>
      </template>
      <el-alert v-if="errorMessageText" :title="errorMessageText" type="error" :closable="false" show-icon>
        <el-button link type="primary" @click="load">重试</el-button>
      </el-alert>
      <el-table v-else v-loading="loading" :data="pagePrompts" row-key="id" data-surface="ai.prompts.table">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="code" label="编码" min-width="150" />
        <el-table-column label="状态" width="110"
          ><template #default="{ row }"
            ><el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag></template
          ></el-table-column
        >
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="publishedAt" label="发布时间" min-width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'ai:prompt:edit'" link type="primary" @click="openPrompt(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              v-auth="'ai:prompt:publish'"
              link
              type="success"
              @click="publish(row)"
              >发布</el-button
            >
            <el-button v-auth="'ai:prompt:delete'" link type="danger" @click="removePrompt(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无提示词模板" /></template>
      </el-table>
      <template #pagination>
        <Pagination
          v-model:page="query.page"
          v-model:limit="query.size"
          :total="filteredPrompts.length"
          @pagination="normalizePage"
        />
      </template>
    </MangoListPanel>

    <MangoDialog
      v-model="dialog"
      :title="form.id ? '编辑提示词' : '新增提示词'"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="110px">
        <el-form-item label="编码" required
          ><el-input v-model="form.code" maxlength="64" :disabled="Boolean(form.id)"
        /></el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" maxlength="500" /></el-form-item>
        <el-form-item label="模板正文" required
          ><el-input v-model="form.template" type="textarea" :rows="8" maxlength="65535" show-word-limit
        /></el-form-item>
        <el-form-item label="变量 JSON"
          ><el-input
            v-model="form.variablesJson"
            type="textarea"
            :rows="4"
            placeholder='例如 {"customerName":{"type":"string"}}'
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="savePrompt">保存</el-button></template
      >
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import type { AiPrompt, AiPromptStatus } from '@mango/ai-api';
import { MangoDialog, MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useAiConfigurationApi } from '../../composables/useAiConfigurationApi';
import { isDialogCancellation, isRequestAborted, requestErrorMessage } from '../../utils/requestError';

defineOptions({ name: 'AiPromptsView' });
const api = useAiConfigurationApi();
const prompts = ref<AiPrompt[]>([]);
const loading = ref(false);
const saving = ref(false);
const dialog = ref(false);
const errorMessageText = ref('');
let controller: AbortController | undefined;
const query = reactive<{ keyword: string; status: AiPromptStatus | ''; page: number; size: number }>({
  keyword: '',
  status: '',
  page: 1,
  size: 20,
});
const criteria = reactive<{ keyword: string; status: AiPromptStatus | '' }>({ keyword: '', status: '' });
const form = reactive<{
  id?: string;
  code: string;
  name: string;
  description: string;
  template: string;
  variablesJson: string;
}>({
  code: '',
  name: '',
  description: '',
  template: '',
  variablesJson: '',
});
const filteredPrompts = computed(() =>
  prompts.value.filter((item) => {
    const matchesKeyword =
      !criteria.keyword ||
      item.name.toLowerCase().includes(criteria.keyword) ||
      item.code.toLowerCase().includes(criteria.keyword);
    return matchesKeyword && (!criteria.status || item.status === criteria.status);
  }),
);
const pagePrompts = computed(() => {
  const start = (query.page - 1) * query.size;
  return filteredPrompts.value.slice(start, start + query.size);
});

function normalizePage() {
  query.page = Math.min(query.page, Math.max(1, Math.ceil(filteredPrompts.value.length / query.size)));
}
function handleSearch() {
  criteria.keyword = query.keyword.trim().toLowerCase();
  criteria.status = query.status;
  query.page = 1;
}
function handleReset() {
  query.keyword = '';
  query.status = '';
  criteria.keyword = '';
  criteria.status = '';
  query.page = 1;
}

function statusName(status: AiPromptStatus) {
  return ({ DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' } as Record<AiPromptStatus, string>)[status];
}
function statusType(status: AiPromptStatus) {
  return status === 'PUBLISHED' ? 'success' : status === 'ARCHIVED' ? 'info' : 'warning';
}
async function load() {
  controller?.abort();
  controller = new AbortController();
  loading.value = true;
  errorMessageText.value = '';
  try {
    prompts.value = await api.prompts(controller.signal);
    normalizePage();
  } catch (error) {
    if (!isRequestAborted(error)) errorMessageText.value = requestErrorMessage(error, '加载提示词失败');
  } finally {
    loading.value = false;
  }
}
function openPrompt(item?: AiPrompt) {
  Object.assign(
    form,
    item
      ? { ...item, variablesJson: item.variablesJson ?? '' }
      : { id: undefined, code: '', name: '', description: '', template: '', variablesJson: '' },
  );
  dialog.value = true;
}
async function savePrompt() {
  if (!form.code.trim() || !form.name.trim() || !form.template.trim()) return ElMessage.error('请完整填写提示词信息');
  if (form.variablesJson.trim()) {
    try {
      const value: unknown = JSON.parse(form.variablesJson);
      if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error();
    } catch {
      return ElMessage.error('变量 JSON 必须是对象');
    }
  }
  saving.value = true;
  try {
    if (form.id)
      await api.updatePrompt({
        id: form.id,
        code: form.code,
        name: form.name,
        description: form.description,
        template: form.template,
        variablesJson: form.variablesJson || undefined,
      });
    else
      await api.createPrompt({
        code: form.code,
        name: form.name,
        description: form.description,
        template: form.template,
        variablesJson: form.variablesJson || undefined,
      });
    dialog.value = false;
    ElMessage.success('提示词已保存');
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存提示词失败'));
  } finally {
    saving.value = false;
  }
}
async function publish(item: AiPrompt) {
  try {
    await api.publishPrompt(item.id);
    ElMessage.success('提示词已发布');
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '发布提示词失败'));
  }
}
async function removePrompt(item: AiPrompt) {
  try {
    await ElMessageBox.confirm(`确认删除提示词“${item.name}”？`, '删除提示词', { type: 'warning' });
    await api.deletePrompt(item.id);
    ElMessage.success('提示词已删除');
    await load();
  } catch (error) {
    if (!isDialogCancellation(error)) ElMessage.error(requestErrorMessage(error, '删除提示词失败'));
  }
}
onMounted(() => {
  void load();
});
onBeforeUnmount(() => controller?.abort());
</script>
