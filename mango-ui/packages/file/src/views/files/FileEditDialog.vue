<template>
  <MangoDialog v-model="visible" :title="dialogTitle" width="720px" destroy-on-close>
    <el-form :model="form" label-width="110px" class="file-edit-form">
      <MangoPageSection title="文件信息">
        <el-form-item label="文件名" required>
          <el-input v-model="form.fileName" maxlength="255" show-word-limit placeholder="请输入文件名" />
          <div class="form-hint">扩展名必须与原文件一致，当前为 {{ recordExt || '无' }}。</div>
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="form.purpose" maxlength="64" placeholder="如 contract、attachment" />
        </el-form-item>
        <el-form-item label="访问级别">
          <DictSelect v-model="form.accessLevel" dict-type="file_access_level" placeholder="请选择访问级别" />
        </el-form-item>
        <el-form-item label="逻辑目录">
          <el-select v-model="form.directoryId" class="full-width" placeholder="请选择逻辑目录">
            <el-option v-for="item in directoryOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
      </MangoPageSection>

      <MangoPageSection title="业务标记">
        <el-form-item label="业务类型">
          <el-input v-model="form.bizType" maxlength="64" placeholder="请填写业务类型" />
        </el-form-item>
        <el-form-item label="业务ID">
          <el-input v-model="form.bizId" maxlength="128" placeholder="请填写业务ID" />
        </el-form-item>
        <el-form-item label="业务参数">
          <el-input
            v-model="form.bizMeta"
            type="textarea"
            :rows="3"
            maxlength="4000"
            show-word-limit
            placeholder="JSON 文本，留空表示清除"
          />
        </el-form-item>
      </MangoPageSection>

      <MangoPageSection v-auth="'file:files:replace'" title="替换文件内容">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="不选择新文件时只更新文件信息；选择新文件会替换当前文件内容，文件ID和业务引用保持不变。"
        />
        <el-upload
          v-model:file-list="fileList"
          class="file-edit-upload"
          :auto-upload="false"
          :limit="1"
          :accept="accept"
          :on-exceed="handleExceed"
        >
          <el-button :icon="Upload">选择新文件</el-button>
          <template #tip>
            <div class="form-hint">
              新文件扩展名必须为 {{ recordExt || '原扩展名' }}，大小不超过 {{ maxSizeText }}。
            </div>
          </template>
        </el-upload>
      </MangoPageSection>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
    </template>
  </MangoDialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import { DictSelect, MangoDialog, MangoPageSection } from '@mango/common';
import { useFileApi } from '../../composables/useFileApi';
import type { FileRecord } from '../../api/file';
import { buildEditMetadata, extensionOf, toBizMetaText, validateEditInput } from './fileEdit';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    record: FileRecord | null;
    directoryOptions?: Array<{ id: string; label: string }>;
    maxSize?: number;
  }>(),
  {
    directoryOptions: () => [{ id: '0', label: '根目录' }],
    maxSize: 0,
  },
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'saved'): void;
}>();

const fileApi = useFileApi();
const saving = ref(false);
const fileList = ref<UploadUserFile[]>([]);
const form = reactive({
  fileName: '',
  purpose: '',
  accessLevel: '',
  bizType: '',
  bizId: '',
  bizMeta: '',
  directoryId: '0',
});

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
});
const dialogTitle = computed(() => (props.record ? `编辑文件：${props.record.fileName}` : '编辑文件'));
const recordExt = computed(() => extensionOf(props.record?.fileName || ''));
const accept = computed(() => (recordExt.value ? `.${recordExt.value}` : undefined));
const maxSizeText = computed(() => {
  if (!props.maxSize) return '文件中心配置';
  const value = props.maxSize;
  if (value >= 1024 * 1024 * 1024) return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`;
  return `${Math.floor(value / 1024 / 1024)} MB`;
});
const selectedFile = computed(() => fileList.value[0]?.raw ?? null);

watch(
  () => [props.modelValue, props.record] as const,
  ([opened, record]) => {
    if (!opened || !record) return;
    form.fileName = record.fileName || '';
    form.purpose = record.purpose || '';
    form.accessLevel = record.accessLevel || '';
    form.bizType = record.bizType || '';
    form.bizId = record.bizId || '';
    form.bizMeta = toBizMetaText(record.bizMeta);
    form.directoryId = record.directoryId ? String(record.directoryId) : '0';
    fileList.value = [];
  },
  { immediate: true },
);

function handleExceed() {
  ElMessage.warning('一次只能选择一个新文件，请先移除已选文件。');
}

async function handleSubmit() {
  if (!props.record) return;
  const metadata = buildEditMetadata(form);
  const file = selectedFile.value;
  const error = validateEditInput(metadata, recordExt.value, {
    newFileSize: file?.size,
    maxSize: props.maxSize,
  });
  if (error) {
    ElMessage.error(error);
    return;
  }
  if (file && extensionOf(file.name) !== recordExt.value) {
    ElMessage.error(`新文件扩展名必须为 ${recordExt.value || '原扩展名'}`);
    return;
  }
  if (file) {
    await ElMessageBox.confirm(
      `将用新文件替换“${props.record.fileName}”的内容。文件ID和业务引用保持不变，旧内容按文件中心清理策略处理。`,
      '确认替换文件内容',
      {
        confirmButtonText: '确认替换',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        type: 'warning',
      },
    );
  }
  saving.value = true;
  try {
    if (file) {
      await fileApi.replaceContent(file, { id: props.record.id, ...metadata });
    } else {
      await fileApi.updateRecord({ id: props.record.id, ...metadata });
    }
    ElMessage.success('保存成功');
    emit('saved');
    visible.value = false;
  } finally {
    saving.value = false;
  }
}

async function handleCancel() {
  if (isDirty()) {
    await ElMessageBox.confirm('关闭后将丢失本次修改，确认关闭？', '提示', {
      confirmButtonText: '确认关闭',
      cancelButtonText: '继续编辑',
      type: 'warning',
    });
  }
  visible.value = false;
}

function isDirty() {
  const record = props.record;
  if (!record) return false;
  if (selectedFile.value) return true;
  return (
    form.fileName.trim() !== (record.fileName || '') ||
    form.purpose.trim() !== (record.purpose || '') ||
    form.accessLevel !== (record.accessLevel || '') ||
    form.bizType.trim() !== (record.bizType || '') ||
    form.bizId.trim() !== (record.bizId || '') ||
    form.bizMeta.trim() !== toBizMetaText(record.bizMeta) ||
    form.directoryId !== (record.directoryId ? String(record.directoryId) : '0')
  );
}
</script>

<style scoped>
.file-edit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-edit-upload {
  margin-top: 12px;
}

.full-width {
  width: 100%;
}

.form-hint {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
