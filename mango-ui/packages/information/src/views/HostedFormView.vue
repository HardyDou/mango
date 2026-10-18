<template>
  <div class="information-hosted-public" data-page="information.hosted">
    <div class="information-hosted-public__card">
      <div v-if="loading" class="information-hosted-public__state">表单加载中…</div>
      <el-result v-else-if="errorMessage" icon="warning" title="表单暂不可用" :sub-title="errorMessage" />

      <template v-else>
        <div class="information-hosted-logo">MANGO · ENTERPRISE</div>
        <InformationFormSurface
          :form-name="form?.formName"
          :description="form?.description"
          :rules="rules"
          :values="values"
          :surface-key="surfaceKey"
          :submitting="submitting"
          footnote="提交即表示您同意我们按隐私政策使用相关信息"
          @update:values="replaceValues"
          @submit="submit"
        />
      </template>
    </div>

    <el-dialog v-model="successVisible" width="420px" :show-close="false" :close-on-click-modal="false" align-center>
      <el-result icon="success" title="提交成功" :sub-title="successMessage" />
      <template #footer>
        <el-button type="primary" @click="successVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import InformationFormSurface from '../components/InformationFormSurface.vue';
import { useInformationApi } from '../composables/useInformationApi';
import type { PublicInformationForm } from '../api/information';
import { parseRules, rulesToFields, type FormCreateRule } from '../domain/rules';

const api = useInformationApi();
const form = ref<PublicInformationForm>();
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref('');
const successVisible = ref(false);
const successMessage = ref('');
const values = reactive<Record<string, unknown>>({});
const rules = ref<FormCreateRule[]>([]);
const surfaceKey = ref(0);

function currentFormCode(): string {
  const hash = window.location.hash || '';
  const queryIndex = hash.indexOf('?');
  const query = new URLSearchParams(queryIndex >= 0 ? hash.slice(queryIndex + 1) : '');
  const pathCode = hash.split('/hosted/')[1]?.split(/[/?]/)[0];
  return decodeURIComponent(pathCode || query.get('formCode') || '');
}

function resetValues() {
  Object.keys(values).forEach((key) => {
    delete values[key];
  });
}

function applyFields() {
  rules.value = form.value ? parseRules(form.value.definitionJson) : [];
  resetValues();
  rulesToFields(rules.value).forEach((field) => {
    values[field.fieldCode] = field.fieldType === 'checkbox' ? [] : '';
  });
  surfaceKey.value += 1;
}

function replaceValues(next: Record<string, unknown>) {
  Object.assign(values, next);
}

function validate(): string | null {
  for (const field of rulesToFields(rules.value)) {
    if (!field.required) {
      continue;
    }
    const value = values[field.fieldCode];
    const empty = value === undefined || value === null || value === '' || (Array.isArray(value) && !value.length);
    if (empty) {
      return `请填写${field.fieldName}`;
    }
  }
  return null;
}

async function load() {
  const formCode = currentFormCode();
  if (!formCode) {
    errorMessage.value = '缺少表单编码';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    form.value = await api.publicForm(formCode);
    applyFields();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单加载失败';
  } finally {
    loading.value = false;
  }
}

async function submit() {
  const invalid = validate();
  if (invalid) {
    ElMessage.warning(invalid);
    return;
  }
  if (!form.value) {
    return;
  }
  submitting.value = true;
  try {
    const result = await api.submitPublic({
      formCode: form.value.formCode,
      values: { ...values },
      sourcePage: window.location.href,
      idempotencyKey: `information-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    });
    successMessage.value = result.message || form.value.successMessage || '提交成功';
    successVisible.value = true;
    applyFields();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>
