<template>
  <div class="information-page" data-page="information.form.config">
    <MangoPageBackBar title="返回详情页" :show-refresh="false" @back="goDetail">
      <template #extra>
        <el-button type="primary" data-action="information.form.save" :loading="saving" @click="save">
          保存配置
        </el-button>
      </template>
    </MangoPageBackBar>

    <el-alert v-if="errorMessage" type="error" :closable="false" show-icon>
      <template #title>
        {{ errorMessage }}
        <el-button link type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-card v-loading="loading" shadow="never" class="information-card">
      <el-form class="information-config-meta" label-position="top" size="small">
        <div class="information-config-meta__grid">
          <el-form-item label="表单名称">
            <el-input v-model="form.formName" data-form-meta="name" placeholder="请输入表单名称" />
          </el-form-item>
          <el-form-item label="表单编码">
            <el-input v-model="form.formCode" data-form-meta="code" :disabled="!!formId" placeholder="请输入表单编码" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" data-form-meta="status" style="width: 100%">
              <el-option label="启用" value="PUBLISHED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.description" data-form-meta="remark" placeholder="请输入备注" />
          </el-form-item>
          <el-form-item label="提交成功提示">
            <el-input v-model="form.successMessage" data-form-meta="success" placeholder="提交成功" />
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="information-card information-designer-card">
      <div class="information-designer-head">
        <span class="information-section-title">表单设计</span>
        <el-button link type="primary" data-action="information.form.designer-json" @click="jsonVisible = true">
          查看表单 JSON
        </el-button>
      </div>
      <div class="information-designer-host" data-surface="information.form.designer">
        <FcDesigner ref="designerRef" :config="designerConfig" :height="designerHeight" @save="syncFromDesigner" />
      </div>
    </el-card>

    <el-dialog v-model="jsonVisible" title="表单 JSON" width="720px" destroy-on-close>
      <pre class="information-json">{{ prettyRules }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MangoPageBackBar } from '@mango/common';
import FcDesigner from 'form-create-designer';
import 'form-create-designer/src/style/index.css';
import 'form-create-designer/src/style/icon.css';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationFormStatus } from '../api/information';
import {
  normalizeDesignerRules,
  parseRules,
  rulesToFields,
  stringifyRules,
  type FormCreateRule,
} from '../domain/rules';

const api = useInformationApi();
const route = useRoute();
const router = useRouter();

const formId = computed(() => String(route.params.id || ''));
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const jsonVisible = ref(false);
const designerRef = ref<InstanceType<typeof FcDesigner>>();
const designerConfig = { showSaveBtn: false, showJsonPreview: false, showDevice: false, showLanguage: false };

const form = reactive({
  formName: '',
  formCode: '',
  description: '',
  successMessage: '提交成功',
  status: 'PUBLISHED' as InformationFormStatus,
});
const rules = ref<FormCreateRule[]>([]);

const columnFields = computed(() => rulesToFields(rules.value));
const designerHeight = computed(() => `calc(100vh - ${formId.value ? 300 : 300}px)`);
const prettyRules = computed(() => JSON.stringify(rules.value, null, 2));

function syncFromDesigner() {
  if (!designerRef.value) {
    return;
  }
  rules.value = normalizeDesignerRules((designerRef.value.getRule() || []) as unknown as FormCreateRule[]);
}

function applyRulesToDesigner() {
  if (!designerRef.value) {
    return;
  }
  designerRef.value.setRule(rules.value as never);
}

async function load() {
  if (!formId.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    const detail = await api.detailForm(formId.value);
    form.formName = detail.formName || '';
    form.formCode = detail.formCode || '';
    form.description = detail.description || '';
    form.successMessage = detail.successMessage || '提交成功';
    form.status = detail.status || 'PUBLISHED';
    rules.value = normalizeDesignerRules(parseRules(detail.definitionJson));
    applyRulesToDesigner();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单配置加载失败';
  } finally {
    loading.value = false;
  }
}

async function save() {
  syncFromDesigner();
  const fields = columnFields.value;
  if (!form.formName.trim()) {
    ElMessage.warning('请输入表单名称');
    return;
  }
  if (!form.formCode.trim()) {
    ElMessage.warning('请输入表单编码');
    return;
  }
  if (!fields.length) {
    ElMessage.warning('请至少添加一个可填写控件');
    return;
  }
  saving.value = true;
  try {
    await api.saveForm({
      id: formId.value,
      formCode: form.formCode.trim(),
      formName: form.formName.trim(),
      description: form.description,
      definitionJson: stringifyRules(rules.value),
      successMessage: form.successMessage,
      status: form.status,
    });
    ElMessage.success('配置已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

function goDetail() {
  return router.push(`/information/forms/${formId.value}/detail`);
}

onMounted(async () => {
  await load();
  applyRulesToDesigner();
});
</script>
