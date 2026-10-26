<template>
  <div class="information-upload-field">
    <el-upload
      :show-file-list="false"
      :limit="limit"
      :multiple="multiple"
      :disabled="disabled || readonly"
      :http-request="doUpload"
      :on-exceed="onExceed"
      accept="*"
    >
      <el-button :disabled="disabled || readonly || reachedLimit" :loading="uploading" data-upload-action="add">
        {{ buttonText }}
      </el-button>
    </el-upload>

    <ul v-if="items.length" class="information-upload-list" :class="{ 'is-card': pictureMode }">
      <li v-for="item in items" :key="item.id" class="information-upload-item">
        <el-image
          v-if="pictureMode && item.previewUrl"
          :src="item.previewUrl"
          fit="cover"
          class="information-upload-thumb"
          :preview-src-list="[item.previewUrl]"
          preview-teleported
        />
        <span class="information-upload-name" :title="item.name">{{ item.name }}</span>
        <el-button link type="primary" data-upload-action="preview" @click="openPreview(item)">预览</el-button>
        <el-button
          v-if="!disabled && !readonly"
          link
          type="danger"
          data-upload-action="remove"
          @click="remove(item.id)"
        >
          移除
        </el-button>
      </li>
    </ul>
    <p class="information-upload-tip">文件统一保存到文件管理{{ multiple ? `，最多 ${limit} 个` : '' }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, type UploadRequestOptions } from 'element-plus';
import { useUploadApi } from '@mango/common';

/**
 * 设计器的上传控件在运行时改用平台跨域上传契约：
 * 文件统一落到 mango-file，表单只保存文件 ID，与平台其它模块保持一致。
 */
const props = withDefaults(
  defineProps<{
    modelValue?: string | string[];
    /** 设计器里的 limit */
    limit?: number;
    listType?: string;
    multiple?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    buttonText?: string;
  }>(),
  {
    modelValue: '',
    limit: 1,
    listType: 'text',
    multiple: false,
    disabled: false,
    readonly: false,
    buttonText: '上传文件',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: string | string[]];
}>();

interface UploadItem {
  id: string;
  name: string;
  previewUrl: string;
}

const uploadApi = useUploadApi();
const uploading = ref(false);
const items = ref<UploadItem[]>([]);

const limit = computed(() => (props.multiple ? Math.max(props.limit || 1, 2) : 1));
const pictureMode = computed(() => props.listType === 'picture' || props.listType === 'picture-card');
const reachedLimit = computed(() => items.value.length >= limit.value);

function currentIds(): string[] {
  if (Array.isArray(props.modelValue)) {
    return props.modelValue.filter(Boolean).map(String);
  }
  return props.modelValue ? [String(props.modelValue)] : [];
}

/** 值只保存文件 ID，展示时补一次详情拿文件名与预览地址。 */
async function hydrate(ids: string[]) {
  const next: UploadItem[] = [];
  for (const id of ids) {
    const existing = items.value.find((item) => item.id === id);
    if (existing) {
      next.push(existing);
      continue;
    }
    try {
      const record = await uploadApi.getUploadedFileDetail(id);
      next.push({ id, name: record.fileName || id, previewUrl: record.previewUrl || record.url || '' });
    } catch {
      next.push({ id, name: id, previewUrl: '' });
    }
  }
  items.value = next;
}

function commit(nextIds: string[]) {
  emit('update:modelValue', props.multiple ? nextIds : nextIds[0] || '');
}

async function doUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    const file = options.file as File;
    const result = pictureMode.value ? await uploadApi.uploadImage(file) : await uploadApi.uploadFile(file);
    const id = String(result.id || '');
    if (!id) {
      ElMessage.error('上传接口未返回文件 ID');
      return;
    }
    items.value = [
      ...items.value,
      { id, name: result.fileName || file.name, previewUrl: result.previewUrl || result.url || '' },
    ];
    commit(items.value.map((item) => item.id));
    ElMessage.success('上传成功，文件已保存到文件管理');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败');
  } finally {
    uploading.value = false;
  }
}

function remove(id: string) {
  items.value = items.value.filter((item) => item.id !== id);
  commit(items.value.map((item) => item.id));
}

function onExceed() {
  ElMessage.warning(`最多上传 ${limit.value} 个文件`);
}

async function openPreview(item: UploadItem) {
  if (item.previewUrl) {
    window.open(item.previewUrl, '_blank', 'noopener');
    return;
  }
  try {
    const record = await uploadApi.getUploadedFileDetail(item.id);
    const url = record.previewUrl || record.url;
    if (url) {
      window.open(url, '_blank', 'noopener');
      return;
    }
    ElMessage.warning('该文件暂无可预览地址');
  } catch {
    ElMessage.warning('无法获取文件预览地址');
  }
}

onMounted(() => {
  void hydrate(currentIds());
});
</script>
