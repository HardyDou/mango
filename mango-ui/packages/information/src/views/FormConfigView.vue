<template>
  <div class="information-page" data-page="information.form.config">
    <MangoPageBackBar title="返回详情页" :show-refresh="false" @back="goDetail">
      <template #extra>
        <el-button @click="previewVisible = true">预览</el-button>
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

    <div v-loading="loading" class="information-builder">
      <el-card shadow="never" class="information-builder__side">
        <h3 class="information-builder__title">添加控件</h3>
        <div v-for="group in FIELD_TOOLBOX_GROUPS" :key="group.title" class="information-toolbox-group">
          <div class="information-toolbox-title">{{ group.title }}</div>
          <button
            v-for="item in group.items"
            :key="item.fieldType"
            type="button"
            class="information-field-option"
            :class="{ 'is-disabled': isUnsupported(item.fieldType) }"
            :disabled="isUnsupported(item.fieldType)"
            @click="addField(item.fieldType)"
          >
            <span class="information-field-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
            <span class="information-field-add">＋</span>
          </button>
        </div>
        <p class="information-helper">
          控件添加后可配置标签、提示、默认值、必填和校验规则。所有控件由托管页面和 API 共用同一份定义。
        </p>
      </el-card>

      <div class="information-builder__canvas">
        <FieldCanvas
          :form-name="form.formName"
          :description="form.description"
          :fields="fields"
          :active-index="activeIndex"
          @select="activeIndex = $event"
          @remove="removeField"
        />
      </div>

      <el-card shadow="never" class="information-builder__config">
        <div class="information-config-section">
          <h3 class="information-builder__title">表单设置</h3>
          <label class="information-config-label">表单名称</label>
          <el-input v-model="form.formName" placeholder="请输入表单名称" />
          <label class="information-config-label">表单编码</label>
          <el-input v-model="form.formCode" :disabled="Boolean(formId)" placeholder="请输入表单编码" />
          <label class="information-config-label">备注</label>
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入备注" />
          <label class="information-config-label">状态</label>
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="PUBLISHED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
          <label class="information-config-label">提交按钮文案</label>
          <el-input v-model="form.successMessage" placeholder="提交成功" />
        </div>

        <div class="information-config-section">
          <h3 class="information-builder__title">字段属性</h3>
          <template v-if="activeField">
            <label class="information-config-label">字段标签</label>
            <el-input v-model="activeField.fieldName" data-field-config="label" />
            <label class="information-config-label">字段类型</label>
            <el-select v-model="activeField.fieldType" style="width: 100%" data-field-config="type">
              <el-option v-for="type in selectableTypes" :key="type" :label="fieldTypeLabel(type)" :value="type" />
            </el-select>
            <label class="information-config-label">字段编码</label>
            <el-input v-model="activeField.fieldCode" data-field-config="code" />
            <label class="information-config-label">提示文字</label>
            <el-input v-model="activeField.placeholder" />
            <label v-if="activeField.fieldType === 'static'" class="information-config-label">说明内容</label>
            <el-input
              v-if="activeField.fieldType === 'static'"
              v-model="activeField.content"
              type="textarea"
              :rows="2"
            />
            <template v-if="needsOptions(activeField.fieldType)">
              <label class="information-config-label">选项（每行一个，格式：标签=值）</label>
              <el-input
                :model-value="optionsText"
                type="textarea"
                :rows="4"
                placeholder="产品咨询=product"
                @update:model-value="updateOptions"
              />
            </template>
            <div class="information-toggle-row">
              <span>必填字段</span>
              <el-switch v-model="activeField.required" />
            </div>
            <div class="information-toggle-row">
              <el-button link type="danger" @click="removeField(activeIndex)">删除该字段</el-button>
            </div>
          </template>
          <el-empty v-else description="请选择左侧控件或中间字段" :image-size="60" />
        </div>

        <div class="information-config-section">
          <h3 class="information-builder__title">当前字段</h3>
          <div v-if="fields.length" class="information-field-list">
            <button
              v-for="(field, index) in fields"
              :key="field.fieldCode + index"
              type="button"
              class="information-field-row"
              :class="{ 'is-active': index === activeIndex }"
              @click="activeIndex = index"
            >
              <span>☷ {{ field.fieldName || '未命名字段' }}</span>
              <span v-if="field.required" class="information-required">必填</span>
            </button>
          </div>
          <p v-else class="information-helper">尚未添加控件。</p>
        </div>

        <div class="information-publish-card">
          保存后可选择一种接入方式：<br />
          ① 自定义页面调用 API<br />
          ② 直接使用托管表单页面
          <span class="information-code">{{
            form.formCode ? `/information/hosted?formCode=${form.formCode}` : ''
          }}</span>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="previewVisible" title="预览" width="720px" destroy-on-close>
      <div class="information-form-card">
        <InformationFormSurface
          :form-name="form.formName"
          :description="form.description"
          :fields="fields"
          :values="previewValues"
          :submit-label="'提交'"
          show-submit
          disabled
          plain
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MangoPageBackBar } from '@mango/common';
import FieldCanvas from '../components/FieldCanvas.vue';
import InformationFormSurface from '../components/InformationFormSurface.vue';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationFormStatus } from '../api/information';
import {
  FIELD_TOOLBOX_GROUPS,
  defaultValueFor,
  fieldTypeLabel,
  isDisplayOnly,
  isUnsupported,
  needsOptions,
  nextFieldCode,
  parseFields,
  stringifyFields,
  type InformationFieldDefinition,
  type InformationFieldType,
} from '../domain/fields';

const api = useInformationApi();
const route = useRoute();
const router = useRouter();

const formId = computed(() => String(route.params.id || ''));
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const previewVisible = ref(false);

const form = reactive({
  formName: '',
  formCode: '',
  description: '',
  successMessage: '提交成功',
  status: 'PUBLISHED' as InformationFormStatus,
});
const fields = ref<InformationFieldDefinition[]>([]);
const activeIndex = ref(-1);

const activeField = computed(() => fields.value[activeIndex.value]);
const previewValues = computed(() =>
  Object.fromEntries(fields.value.map((field) => [field.fieldCode, defaultValueFor(field)])),
);
const optionsText = computed(() =>
  (activeField.value?.options || []).map((option) => `${option.label}=${option.value}`).join('\n'),
);
const selectableTypes = computed<InformationFieldType[]>(() =>
  FIELD_TOOLBOX_GROUPS.flatMap((group) => group.items)
    .map((item) => item.fieldType)
    .filter((type) => !isUnsupported(type)),
);

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
    const previousIndex = activeIndex.value;
    fields.value = parseFields(detail.definitionJson);
    activeIndex.value = fields.value.length
      ? Math.min(previousIndex >= 0 ? previousIndex : 0, fields.value.length - 1)
      : -1;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单配置加载失败';
  } finally {
    loading.value = false;
  }
}

function addField(fieldType: InformationFieldType) {
  if (isUnsupported(fieldType)) {
    ElMessage.warning('文件上传为 P1 预留能力，首版暂不提供');
    return;
  }
  fields.value.push({
    fieldCode: nextFieldCode(fields.value),
    fieldName: fieldTypeLabel(fieldType),
    fieldType,
    required: !isDisplayOnly(fieldType),
    placeholder: '',
    options: needsOptions(fieldType) ? [{ label: '选项一', value: 'option1' }] : undefined,
    content: fieldType === 'static' ? '说明文字' : '',
    sort: fields.value.length + 1,
  });
  activeIndex.value = fields.value.length - 1;
}

function removeField(index: number) {
  if (index < 0) {
    return;
  }
  fields.value.splice(index, 1);
  activeIndex.value = fields.value.length ? Math.min(index, fields.value.length - 1) : -1;
}

function updateOptions(value: string | number) {
  if (!activeField.value) {
    return;
  }
  activeField.value.options = String(value)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const separator = line.indexOf('=');
      const label = separator >= 0 ? line.slice(0, separator).trim() : line;
      const optionValue = separator >= 0 ? line.slice(separator + 1).trim() : line;
      return { label, value: optionValue };
    });
}

function validate(): string | null {
  if (!form.formName.trim()) {
    return '请输入表单名称';
  }
  if (!form.formCode.trim()) {
    return '请输入表单编码';
  }
  if (!fields.value.length) {
    return '请至少添加一个控件';
  }
  const codes = new Set<string>();
  for (const field of fields.value) {
    if (!field.fieldName.trim()) {
      return '字段标签不能为空';
    }
    if (!field.fieldCode.trim()) {
      return '字段编码不能为空';
    }
    if (codes.has(field.fieldCode)) {
      return `字段编码重复：${field.fieldCode}`;
    }
    codes.add(field.fieldCode);
    if (needsOptions(field.fieldType) && !(field.options || []).length) {
      return `字段 ${field.fieldName} 需要至少一个选项`;
    }
  }
  return null;
}

async function save() {
  const invalid = validate();
  if (invalid) {
    ElMessage.warning(invalid);
    return;
  }
  saving.value = true;
  try {
    await api.saveForm({
      id: formId.value,
      formCode: form.formCode.trim(),
      formName: form.formName.trim(),
      description: form.description,
      definitionJson: stringifyFields(fields.value),
      successMessage: form.successMessage,
      status: form.status,
    });
    ElMessage.success('配置已保存');
    await load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

function goDetail() {
  return router.push(`/information/forms/${formId.value}/detail`);
}

onMounted(load);
</script>
