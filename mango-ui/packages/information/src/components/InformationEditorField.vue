<template>
  <div class="information-editor-field">
    <Editor
      :model-value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :height="height"
      image-value-type="token"
      paste-image-mode="upload"
      @update:model-value="emit('update:modelValue', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { Editor } from '@mango/common';

/**
 * 富文本控件复用平台 Editor：正文图片与附件走 mango-file 托管上传，
 * 与平台其它模块的富文本行为保持一致，不额外配置第三方上传地址。
 */
withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    disabled?: boolean;
    height?: number | string;
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
    disabled: false,
    height: 320,
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>
