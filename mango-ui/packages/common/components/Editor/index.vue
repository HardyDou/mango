<template>
  <div class="editor-wrapper">
    <Toolbar
      v-if="!disabled && editorInstance"
      class="editor-toolbar"
      :editor="editorInstance"
      :default-config="toolbarConfig"
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
import { computed, ref, watch, onBeforeUnmount, shallowRef, nextTick, onMounted } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import '@wangeditor/editor/dist/css/style.css';
import { fileToken, uploadImage, type UploadResult } from '../../api/upload';

defineOptions({
  name: 'MangoEditor',
});

type EditorMode = 'default' | 'simple';
type EditorImageValueType = 'url' | 'id' | 'token';
type EditorToolbarKey =
  | string
  | {
      key: string;
      title?: string;
      iconSvg?: string;
      menuKeys?: string[];
    };

const staleSelectionErrorMessage = 'Cannot resolve a Slate range from DOM range';
let staleSelectionGuardRefs = 0;
let staleSelectionGuardCleanupTimer: ReturnType<typeof window.setTimeout> | undefined;

function isWangEditorStaleSelectionError(error: unknown, message: string) {
  if (!message.includes(staleSelectionErrorMessage)) {
    return false;
  }
  const stack = error instanceof Error ? error.stack || '' : '';
  return stack.includes('@wangeditor') || stack.includes('wangeditor') || stack.includes('toSlateRange');
}

function handleStaleSelectionError(event: ErrorEvent) {
  if (isWangEditorStaleSelectionError(event.error, event.message)) {
    event.preventDefault();
    event.stopImmediatePropagation();
  }
}

function retainStaleSelectionGuard() {
  if (typeof window === 'undefined') {
    return;
  }
  if (staleSelectionGuardCleanupTimer) {
    window.clearTimeout(staleSelectionGuardCleanupTimer);
    staleSelectionGuardCleanupTimer = undefined;
  }
  if (staleSelectionGuardRefs === 0) {
    window.addEventListener('error', handleStaleSelectionError, true);
  }
  staleSelectionGuardRefs += 1;
}

function releaseStaleSelectionGuard() {
  if (typeof window === 'undefined' || staleSelectionGuardRefs === 0) {
    return;
  }
  staleSelectionGuardRefs -= 1;
  if (staleSelectionGuardRefs === 0) {
    staleSelectionGuardCleanupTimer = window.setTimeout(() => {
      window.removeEventListener('error', handleStaleSelectionError, true);
      staleSelectionGuardCleanupTimer = undefined;
    }, 500);
  }
}

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    height?: number | string;
    disabled?: boolean;
    mode?: EditorMode;
    toolbarKeys?: EditorToolbarKey[];
    imageValueType?: EditorImageValueType;
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
    height: 300,
    disabled: false,
    mode: 'default',
    toolbarKeys: () => [],
    imageValueType: 'url',
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}>();

// Use shallowRef to store the editor instance as per WangEditor docs
const editorInstance = shallowRef<any>(null);
const valueHtml = ref(props.modelValue);

// 显示完整工具栏；业务可通过 toolbarKeys 精简为指定按钮集合。
const defaultToolbarKeys: EditorToolbarKey[] = [
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
];

const toolbarConfig = computed(() => {
  if (props.toolbarKeys?.length) {
    return { toolbarKeys: props.toolbarKeys };
  }
  return props.mode === 'simple' ? {} : { toolbarKeys: defaultToolbarKeys };
});

const editorConfig = computed(() => ({
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 10 * 1024 * 1024, // 10MB
      customUpload: async (file: File, insertFn: (url: string, alt: string, href: string) => void) => {
        try {
          const result = await uploadImage(file);
          const imageValue = resolveImageInsertValue(result);
          insertFn(imageValue, result.fileName, imageValue);
        } catch (error) {
          console.error('Image upload failed:', error);
        }
      },
    },
  },
}));

function resolveImageInsertValue(result: UploadResult) {
  if (props.imageValueType === 'id') {
    return result.id ? String(result.id) : fallbackImageUrl(result);
  }
  if (props.imageValueType === 'token') {
    return result.id ? fileToken(result.id) : fallbackImageUrl(result);
  }
  return result.url;
}

function fallbackImageUrl(result: UploadResult) {
  console.warn('[MangoEditor] imageValueType requires upload result id; fallback to url.');
  return result.url;
}

// Handle editor created
function handleCreated(editor: any) {
  editorInstance.value = editor;
}

// Handle content change
function handleChange(editor: any) {
  const html = editor.getHtml();
  valueHtml.value = html;
  emit('update:modelValue', html);
  emit('change', html);
}

function updateContent(content: string) {
  valueHtml.value = content;
  if (editorInstance.value && editorInstance.value.getHtml?.() !== content) {
    editorInstance.value.setHtml?.(content);
  }
  emit('update:modelValue', content);
  emit('change', content);
}

function blurEditorSelection() {
  editorInstance.value?.blur?.();
  if (typeof window !== 'undefined') {
    window.getSelection()?.removeAllRanges();
  }
}

onMounted(() => {
  retainStaleSelectionGuard();
});

// Watch for external value changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (valueHtml.value !== newValue && editorInstance.value) {
      valueHtml.value = newValue;
    }
  },
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
  },
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
  },
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
  },
);

// Cleanup
onBeforeUnmount(() => {
  const editor = editorInstance.value;
  if (editor) {
    blurEditorSelection();
    editor.destroy();
  }
  releaseStaleSelectionGuard();
});

// Expose methods
defineExpose({
  getEditor: () => editorInstance.value,
  getText: () => editorInstance.value?.getText() || '',
  getHtml: () => editorInstance.value?.getHtml() || '',
  setContent: updateContent,
  clear: () => updateContent(''),
  blur: blurEditorSelection,
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

:global(.w-e-full-screen-container) {
  z-index: 3000 !important;
  background: #fff;
}
</style>
