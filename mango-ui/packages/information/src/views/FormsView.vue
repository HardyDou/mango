<template>
  <MangoListPage class="information-page" data-page="information.forms">
    <template #search>
      <MangoSearchPanel :model="query" :columns="4" @search="search" @reset="reset">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="名称/编码" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="启用" value="PUBLISHED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button type="primary" data-action="information.form.create" @click="openCreate">新增</el-button>
      </template>

      <el-alert v-if="errorMessage" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="load">重试</el-button>
        </template>
      </el-alert>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        :empty-text="
          query.keyword || query.status ? '没有匹配的表单，试试调整筛选条件' : '暂无表单，点击右上角「新增」开始'
        "
      >
        <el-table-column label="表单名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.formName || '-' }}</template>
        </el-table-column>
        <el-table-column label="表单编码" min-width="164">
          <template #default="{ row }">
            <span class="information-code">{{ row.formCode }}</span>
            <el-button link type="primary" data-row-action="copy-code" @click="copyCode(row)">复制</el-button>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <MangoStatusText :tone="formStatusTone(row.status)">{{ formStatusLabel(row.status) }}</MangoStatusText>
          </template>
        </el-table-column>
        <el-table-column label="提交量" width="96" align="right">
          <template #default="{ row }">
            <el-button link type="primary" data-row-action="submissions" @click="openSubmissions(row)">
              {{ formatCount(row.submissionCount) }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="创建日期" width="118">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新日期" width="118">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="创建人" width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createdByName || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="272">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openConfig(row)">配置</el-button>
            <el-button link type="primary" @click="openSubmissions(row)">采集记录</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="toggleStatus(row, 'DISABLED')">
              停用
            </el-button>
            <el-button v-else link type="success" @click="toggleStatus(row, 'PUBLISHED')">启用</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @pagination="load"
        />
      </template>
    </MangoListPanel>

    <el-dialog v-model="createVisible" title="新增表单" width="560px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="表单名称" prop="formName">
          <el-input v-model="createForm.formName" placeholder="请输入表单名称" />
        </el-form-item>
        <el-form-item label="表单编码" prop="formCode">
          <el-input v-model="createForm.formCode" placeholder="请输入表单编码" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="createForm.status">
            <el-option label="启用" value="PUBLISHED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { MangoListPage, MangoListPanel, MangoSearchPanel, MangoStatusText, Pagination } from '@mango/common';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationForm, InformationFormStatus } from '../api/information';
import { formatCount, formatDate, formStatusLabel, formStatusTone } from '../domain/status';
import { stringifyRules } from '../domain/rules';

const api = useInformationApi();
const router = useRouter();

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' });
const rows = ref<InformationForm[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');

const createVisible = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive({
  formName: '',
  formCode: '',
  description: '',
  status: 'PUBLISHED' as InformationFormStatus,
});
const createRules: FormRules = {
  formName: [{ required: true, message: '请输入表单名称', trigger: 'blur' }],
  formCode: [
    { required: true, message: '请输入表单编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9-]*$/, message: '编码以小写字母开头，仅含小写字母、数字和短横线', trigger: 'blur' },
  ],
};

async function load() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await api.pageForms(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单列表加载失败';
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  return load();
}

function reset() {
  query.keyword = '';
  query.status = '';
  return search();
}

function openCreate() {
  Object.assign(createForm, { formName: '', formCode: '', description: '', status: 'PUBLISHED' });
  createVisible.value = true;
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    const id = await api.saveForm({
      formCode: createForm.formCode.trim(),
      formName: createForm.formName.trim(),
      description: createForm.description,
      definitionJson: stringifyRules([]),
      status: createForm.status,
    });
    createVisible.value = false;
    ElMessage.success('表单已创建，接下来配置采集项');
    await router.push(`/information/forms/${id}/config`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

/** 停用会让托管页面和 API 立即不可提交，先确认再执行。 */
async function toggleStatus(row: InformationForm, status: InformationFormStatus) {
  if (status === 'DISABLED') {
    try {
      await ElMessageBox.confirm(`停用后「${row.formName}」的托管页面和 API 将无法提交，确认停用？`, '停用表单', {
        type: 'warning',
        confirmButtonText: '停用',
        cancelButtonText: '取消',
      });
    } catch {
      return;
    }
  }
  try {
    await api.updateFormStatus(row.id!, status);
    ElMessage.success(status === 'PUBLISHED' ? '已启用' : '已停用');
    await load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '状态更新失败');
  }
}

async function copyCode(row: InformationForm) {
  try {
    await navigator.clipboard.writeText(row.formCode || '');
    ElMessage.success('表单编码已复制');
  } catch {
    ElMessage.warning('浏览器未授权剪贴板，请手动选择编码复制');
  }
}

async function remove(row: InformationForm) {
  try {
    await ElMessageBox.confirm('确认删除该表单？已有采集记录的表单不可删除。', '删除确认', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await api.deleteForm(row.id!);
    ElMessage.success('删除成功');
    await load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

function openDetail(row: InformationForm) {
  return router.push(`/information/forms/${row.id}/detail`);
}
function openConfig(row: InformationForm) {
  return router.push(`/information/forms/${row.id}/config`);
}
function openSubmissions(row: InformationForm) {
  return router.push(`/information/forms/${row.id}/submissions`);
}

onMounted(load);
</script>
