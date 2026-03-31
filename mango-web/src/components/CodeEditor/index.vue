<template>
  <div
    ref="containerRef"
    class="code-editor-container"
  >
    <textarea ref="textareaRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import CodeMirror from 'codemirror';
// Import modes
import 'codemirror/mode/javascript/javascript';
import 'codemirror/mode/xml/xml';
import 'codemirror/mode/css/css';
import 'codemirror/mode/htmlmixed/htmlmixed';
import 'codemirror/mode/python/python';
import 'codemirror/mode/sql/sql';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/mode/clike/clike';
// Import themes
import 'codemirror/theme/material-darker.css';
import 'codemirror/theme/material-ocean.css';
// Import addons
import 'codemirror/addon/edit/matchbrackets';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/selection/active-line';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    language?: string;
    theme?: 'default' | 'material-darker' | 'material-ocean';
    readonly?: boolean;
    lineNumbers?: boolean;
    matchBrackets?: boolean;
    autoCloseBrackets?: boolean;
    height?: string;
  }>(),
  {
    modelValue: '',
    language: 'javascript',
    theme: 'default',
    readonly: false,
    lineNumbers: true,
    matchBrackets: true,
    autoCloseBrackets: true,
    height: '300px',
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
let editor: CodeMirror.Editor | null = null;

// Language mode mapping
const getMode = (lang: string): string | object => {
  const modeMap: Record<string, string> = {
    js: 'javascript',
    javascript: 'javascript',
    xml: 'xml',
    html: 'htmlmixed',
    css: 'css',
    python: 'python',
    java: 'text/x-java',
    sql: 'sql',
    markdown: 'markdown',
    json: { name: 'javascript', json: true },
    c: 'text/x-csrc',
    cpp: 'text/x-c++src',
    csharp: 'text/x-csharp',
    go: 'text/x-go',
    rust: 'text/x-rust',
  };
  return modeMap[lang] || 'javascript';
};

// Initialize editor
const initEditor = () => {
  if (!textareaRef.value) return;

  editor = CodeMirror.fromTextArea(textareaRef.value, {
    value: props.modelValue || '',
    mode: getMode(props.language),
    theme: props.theme === 'default' ? 'default' : props.theme,
    readOnly: props.readonly,
    lineNumbers: props.lineNumbers,
    matchBrackets: props.matchBrackets,
    autoCloseBrackets: props.autoCloseBrackets,
    styleActiveLine: true,
    lineWrapping: true,
    extraKeys: {
      'Ctrl-/': 'toggleComment',
      'Cmd-/': 'toggleComment',
    },
  });

  // Set height - using explicit pixel value
  const heightStr = typeof props.height === 'number' ? `${props.height}px` : String(props.height);
  editor.setSize('100%', heightStr);

  // Apply CSS to container after editor is created
  if (containerRef.value) {
    containerRef.value.style.height = heightStr;
  }

  // Listen for changes
  editor.on('change', (cm) => {
    const value = cm.getValue();
    emit('update:modelValue', value);
    emit('change', value);
  });
};

// Destroy editor
const destroyEditor = () => {
  if (editor) {
    editor.toTextArea();
    editor = null;
  }
};

// Watch for external value changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (editor && newValue !== editor.getValue()) {
      editor.setValue(newValue || '');
    }
  }
);

// Watch for language changes
watch(
  () => props.language,
  (newLang) => {
    if (editor) {
      editor.setOption('mode', getMode(newLang));
    }
  }
);

// Watch for theme changes
watch(
  () => props.theme,
  (newTheme) => {
    if (editor) {
      editor.setOption('theme', newTheme === 'default' ? 'default' : newTheme);
    }
  }
);

// Watch for readonly state
watch(
  () => props.readonly,
  (readonly) => {
    if (editor) {
      editor.setOption('readOnly', readonly);
    }
  }
);

// Watch for height changes
watch(
  () => props.height,
  (newHeight) => {
    if (editor) {
      const heightStr = typeof newHeight === 'number' ? `${newHeight}px` : String(newHeight);
      editor.setSize('100%', heightStr);
      if (containerRef.value) {
        containerRef.value.style.height = heightStr;
      }
    }
  }
);

onMounted(() => {
  nextTick(() => {
    initEditor();
  });
});

onBeforeUnmount(() => {
  destroyEditor();
});

// Expose methods
defineExpose({
  getEditor: () => editor,
  getValue: () => editor?.getValue() || '',
  setValue: (value: string) => editor?.setValue(value),
  clear: () => editor?.setValue(''),
  refresh: () => editor?.refresh(),
  focus: () => editor?.focus(),
});
</script>

<style scoped lang="scss">
.code-editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  height: 300px;
  position: relative;

  // The original textarea should be hidden but present for CodeMirror
  textarea {
    display: none;
  }

  :deep(.CodeMirror) {
    height: 100%;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
    font-size: 14px;
    line-height: 1.5;
  }

  :deep(.CodeMirror-focused) {
    outline: none;
  }

  :deep(.CodeMirror-gutters) {
    border-right: 1px solid #e8e8e8;
    background-color: #fafafa;
  }
}
</style>
