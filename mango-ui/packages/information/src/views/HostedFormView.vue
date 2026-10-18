<template>
  <div class="information-hosted-page">
    <el-card class="information-hosted-card"
      ><template #header
        ><div>
          <h2>{{ form?.formName || '信息采集' }}</h2>
          <p v-if="form?.description">{{ form.description }}</p>
        </div></template
      ><el-alert v-if="error" type="error" :closable="false" show-icon>{{ error }}</el-alert
      ><el-form
        v-else-if="form && !submitted"
        ref="formRef"
        :model="values"
        :rules="rules"
        label-position="top"
        @submit.prevent="submit"
        ><el-form-item v-for="field in fields" :key="field.fieldCode" :label="field.fieldName" :prop="field.fieldCode"
          ><el-input
            v-if="field.fieldType === 'textarea'"
            v-model="values[field.fieldCode]"
            type="textarea"
            :rows="4" /><el-select
            v-else-if="field.fieldType === 'select'"
            v-model="values[field.fieldCode]"
            style="width: 100%"
            ><el-option
              v-for="option in field.options || []"
              :key="String(option.value)"
              :label="option.label"
              :value="option.value" /></el-select
          ><el-input v-else v-model="values[field.fieldCode]" /></el-form-item
        ><el-button type="primary" native-type="submit" :loading="submitting">提交</el-button></el-form
      ><el-result v-if="submitted" icon="success" title="提交成功" :sub-title="message"
    /></el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useInformationApi } from '../composables/useInformationApi';

type Field = {
  fieldCode: string;
  fieldName: string;
  fieldType: string;
  required?: boolean;
  options?: Array<{ label: string; value: unknown }>;
};
const api = useInformationApi();
const form = ref<{
  formCode: string;
  formName: string;
  description?: string;
  definitionJson: string;
  successMessage?: string;
}>();
const fields = ref<Field[]>([]);
const values = reactive<Record<string, unknown>>({});
const rules = reactive<FormRules>({});
const formRef = ref<FormInstance>();
const error = ref('');
const submitting = ref(false);
const submitted = ref(false);
const message = ref('');
async function load() {
  const hash = window.location.hash || '';
  const queryIndex = hash.indexOf('?');
  const query = new URLSearchParams(queryIndex >= 0 ? hash.slice(queryIndex + 1) : '');
  const pathCode = hash.split('/hosted/')[1]?.split(/[/?]/)[0];
  const code = decodeURIComponent(pathCode || query.get('formCode') || '');
  if (!code) {
    error.value = '缺少表单编码';
    return;
  }
  try {
    const loaded = await api.publicForm(code);
    form.value = loaded;
    fields.value = JSON.parse(loaded.definitionJson) as Field[];
    fields.value.forEach((field) => {
      values[field.fieldCode] = '';
      if (field.required)
        rules[field.fieldCode] = [{ required: true, message: `请输入${field.fieldName}`, trigger: 'blur' }];
    });
  } catch (e) {
    error.value = e instanceof Error ? e.message : '表单加载失败';
  }
}
async function submit() {
  if (!(await formRef.value?.validate().catch(() => false))) return;
  submitting.value = true;
  try {
    const result = await api.submitPublic({
      formCode: form.value!.formCode,
      values,
      sourcePage: window.location.href,
      idempotencyKey: `information-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    });
    message.value = result.message || form.value?.successMessage || '提交成功';
    submitted.value = true;
    ElMessage.success(message.value);
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败');
  } finally {
    submitting.value = false;
  }
}
onMounted(load);
</script>
