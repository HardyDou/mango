<template>
  <MangoListPage class="information-page" data-page="information.forms">
    <template #search>
      <MangoSearchPanel :model="query" :columns="3" @search="load" @reset="reset">
        <el-form-item label="关键字"
          ><el-input v-model="query.keyword" clearable placeholder="表单名称/编码" @keyup.enter="load"
        /></el-form-item>
        <el-form-item label="状态"
          ><el-select v-model="query.status" clearable placeholder="全部"
            ><el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" /><el-option
              label="已停用"
              value="DISABLED" /></el-select
        ></el-form-item>
      </MangoSearchPanel>
    </template>
    <MangoListPanel>
      <template #actions
        ><el-button data-action="information.form.create" type="primary" plain @click="openEditor()"
          >新建表单</el-button
        ></template
      >
      <el-alert v-if="error" type="error" :closable="false" show-icon>{{ error }}</el-alert>
      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无表单">
        <el-table-column prop="formName" label="表单名称" min-width="180" />
        <el-table-column prop="formCode" label="表单编码" min-width="160" />
        <el-table-column prop="fieldCount" label="字段数" width="90" />
        <el-table-column prop="submissionCount" label="提交数" width="90" />
        <el-table-column label="状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'DISABLED' ? 'info' : 'warning'">{{
              statusLabel(row.status)
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showDetail(row)">详情</el-button
            ><el-button link type="primary" @click="openEditor(row)">配置</el-button
            ><el-button link type="danger" @click="remove(row)">删除</el-button></template
          ></el-table-column
        >
      </el-table>
      <template #pagination
        ><Pagination
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @pagination="load"
      /></template>
    </MangoListPanel>

    <el-dialog v-model="editorVisible" :title="editor.id ? '配置表单' : '新建表单'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="editor" :rules="rules" label-width="110px">
        <el-form-item label="表单编码" prop="formCode"
          ><el-input v-model="editor.formCode" :disabled="Boolean(editor.id)" placeholder="例如 customer-contact"
        /></el-form-item>
        <el-form-item label="表单名称" prop="formName"
          ><el-input v-model="editor.formName" placeholder="例如 客户联系表"
        /></el-form-item>
        <el-form-item label="说明"><el-input v-model="editor.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="字段定义" prop="definitionJson"
          ><el-input
            v-model="editor.definitionJson"
            type="textarea"
            :rows="12"
            placeholder="JSON 数组：[{fieldCode,fieldName,fieldType,required,sort}]"
        /></el-form-item>
        <el-form-item label="成功提示"><el-input v-model="editor.successMessage" /></el-form-item>
        <el-form-item label="提交状态"
          ><el-switch v-model="editor.published" active-text="保存并发布" inactive-text="保存草稿"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="editorVisible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存</el-button></template
      >
    </el-dialog>

    <el-dialog v-model="detailVisible" title="表单详情" width="720px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="表单名称">{{ detail.formName }}</el-descriptions-item>
        <el-descriptions-item label="表单编码">{{ detail.formCode }}</el-descriptions-item>
        <el-descriptions-item label="托管入口"
          ><el-link type="primary" @click="openHosted(detail.hostedPath)">{{
            detail.hostedPath
          }}</el-link></el-descriptions-item
        >
        <el-descriptions-item label="服务端 SK"
          ><el-input :model-value="detail.serverSecret" readonly
            ><template #append><el-button @click="copy(detail.serverSecret)">复制</el-button></template></el-input
          ></el-descriptions-item
        >
        <el-descriptions-item label="字段定义">
          <pre class="information-json">{{ pretty(detail.definitionJson) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationForm, InformationFormDetail } from '../api/information';

const api = useInformationApi();
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' });
const rows = ref<InformationForm[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const editorVisible = ref(false);
const detailVisible = ref(false);
const formRef = ref<FormInstance>();
const detail = ref<InformationFormDetail>();
const editor = reactive<InformationFormDetail & { published: boolean }>({
  formCode: '',
  formName: '',
  description: '',
  definitionJson: '[\n  {"fieldCode":"name","fieldName":"姓名","fieldType":"text","required":true,"sort":1}\n]',
  successMessage: '提交成功',
  published: false,
});
const rules: FormRules = {
  formCode: [{ required: true, message: '请输入表单编码', trigger: 'blur' }],
  formName: [{ required: true, message: '请输入表单名称', trigger: 'blur' }],
  definitionJson: [{ required: true, message: '请输入字段定义', trigger: 'blur' }],
};
const statusLabel = (status?: string) =>
  ({ DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' })[status || ''] || status || '-';
async function load() {
  loading.value = true;
  error.value = '';
  try {
    const result = await api.pageForms(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败';
  } finally {
    loading.value = false;
  }
}
function reset() {
  query.keyword = '';
  query.status = '';
  query.pageNum = 1;
  load();
}
function openEditor(row?: InformationForm) {
  Object.assign(
    editor,
    {
      id: undefined,
      formCode: '',
      formName: '',
      description: '',
      definitionJson: '[\n  {"fieldCode":"name","fieldName":"姓名","fieldType":"text","required":true,"sort":1}\n]',
      successMessage: '提交成功',
      published: false,
    },
    row || {},
  );
  if (row)
    api
      .detailForm(row.id!)
      .then((data: InformationFormDetail) => Object.assign(editor, data, { published: data.status === 'PUBLISHED' }));
  editorVisible.value = true;
}
async function save() {
  if (!(await formRef.value?.validate().catch(() => false))) return;
  try {
    JSON.parse(editor.definitionJson || '');
  } catch {
    ElMessage.error('字段定义必须是合法 JSON');
    return;
  }
  saving.value = true;
  try {
    await api.saveForm({
      id: editor.id,
      formCode: editor.formCode || '',
      formName: editor.formName || '',
      description: editor.description,
      definitionJson: editor.definitionJson || '',
      successMessage: editor.successMessage,
      published: editor.published,
    });
    ElMessage.success('保存成功');
    editorVisible.value = false;
    await load();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
}
async function showDetail(row: InformationForm) {
  detail.value = await api.detailForm(row.id!);
  detailVisible.value = true;
}
async function remove(row: InformationForm) {
  try {
    await ElMessageBox.confirm('确认删除该表单？已有提交记录的表单不可删除。', '删除确认', { type: 'warning' });
    await api.deleteForm(row.id!);
    ElMessage.success('删除成功');
    await load();
  } catch {
    /* 用户取消 */
  }
}
function pretty(json?: string) {
  try {
    return JSON.stringify(JSON.parse(json || ''), null, 2);
  } catch {
    return json || '';
  }
}
async function copy(value?: string) {
  if (value) {
    await navigator.clipboard?.writeText(value);
    ElMessage.success('已复制');
  }
}
function openHosted(path?: string) {
  if (path) window.open(path, '_blank', 'noopener');
}
onMounted(load);
</script>
