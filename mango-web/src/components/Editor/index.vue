<template>
  <div class="editor-wrapper">
    <Toolbar
      v-if="!disabled && editorInstance"
      class="editor-toolbar"
      :editor="editorInstance"
      :default-config="mode === 'simple' ? {} : toolbarConfig"
      :mode="mode"
    />
    <Editor
      ref="editorComponentRef"
      v-model="valueHtml"
      class="editor-content"
      :default-config="editorConfig"
      :mode="mode"
      :disabled="disabled"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, shallowRef, nextTick } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import '@wangeditor/editor/dist/css/style.css';
import { uploadImage } from '@/api/admin/upload';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    height?: number | string;
    disabled?: boolean;
    mode?: 'default' | 'simple';
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
    height: 300,
    disabled: false,
    mode: 'default',
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}>();

// Use shallowRef to store the editor instance as per WangEditor docs
const editorInstance = shallowRef<any>(null);
const valueHtml = ref(props.modelValue);

// 显示所有工具栏
const toolbarConfig = {
  toolbarKeys: [
    'headerSelect',
    '|',
    'bold',
    'underline',
    'italic',
    '|',
    'color',
    'bgColor',
    '|',
    'fontSize',
    'fontFamily',
    '|',
    'insertLink',
    'unLink',
    '|',
    'bulletedList',
    'numberedList',
    'indent',
    'delIndent',
    '|',
    'justifyLeft',
    'justifyRight',
    'justifyCenter',
    'justifyJustify',
    '|',
    'blockquote',
    '|',
    'insertImage',
    '|',
    'insertVideo',
    '|',
    'codeBlock',
    '|',
    'undo',
    'redo',
    '|',
    'fullScreen',
  ],
};

const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 10 * 1024 * 1024, // 10MB
      customUpload: async (file: File, insertFn: (url: string, alt: string, href: string) => void) => {
        try {
          const result = await uploadImage(file);
          insertFn(result.url, result.fileName, result.url);
        } catch (error) {
          console.error('Image upload failed:', error);
        }
      },
    },
  },
};

// Handle editor created
function handleCreated(editor: any) {
  editorInstance.value = editor;
}

// Handle content change
function handleChange(editor: any) {
  const html = editor.getHtml();
  emit('update:modelValue', html);
  emit('change', html);
}

// Watch for external value changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (valueHtml.value !== newValue && editorInstance.value) {
      valueHtml.value = newValue;
    }
  }
);

// Watch for disabled state
watch(
  () => props.disabled,
  (disabled) => {
    if (editorInstance.value) {
      if (disabled) {
        editorInstance.value.disable();
      } else {
        editorInstance.value.enable();
      }
    }
  }
);

// Watch for mode changes - need to recreate editor when mode changes
watch(
  () => props.mode,
  () => {
    if (editorInstance.value) {
      // Destroy and recreate editor with new mode
      editorInstance.value.destroy();
      editorInstance.value = null;
      nextTick(() => {
        // Reinitialize will happen via component re-render
      });
    }
  }
);

// Watch for height changes
watch(
  () => props.height,
  (newHeight) => {
    nextTick(() => {
      const heightStr = typeof newHeight === 'number' ? `${newHeight}px` : String(newHeight);
      // Set CSS variable on wrapper
      const wrapper = document.querySelector('.editor-wrapper');
      if (wrapper) {
        (wrapper as HTMLElement).style.setProperty('--editor-height', heightStr);
      }
    });
  }
);

// Cleanup
onBeforeUnmount(() => {
  const editor = editorInstance.value;
  if (editor) {
    editor.destroy();
  }
});

// Expose methods
defineExpose({
  getEditor: () => editorInstance.value,
  getText: () => editorInstance.value?.getText() || '',
  getHtml: () => editorInstance.value?.getHtml() || '',
  setContent: (content: string) => {
    valueHtml.value = content;
  },
  clear: () => {
    valueHtml.value = '';
  },
});
</script>

<style scoped lang="scss">
.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  --editor-height: 300px;

  .editor-toolbar {
    border-bottom: 1px solid #dcdfe6;
  }

  .editor-content {
    height: var(--editor-height);
    overflow-y: auto;

    :deep(.w-e-text-container) {
      height: var(--editor-height);
      min-height: var(--editor-height);
    }

    :deep(.w-e-scroll) {
      height: var(--editor-height);
    }
  }
}
</style>
