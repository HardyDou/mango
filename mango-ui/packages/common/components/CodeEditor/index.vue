<template>
  <div
    ref="containerRef"
    class="code-editor-container"
    :style="{ height: normalizedHeight, width }"
  >
    <textarea ref="textareaRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/material-darker.css';
import 'codemirror/theme/material-ocean.css';
import type CodeMirrorNamespace from 'codemirror';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    language?: string;
    theme?: 'default' | 'material-darker' | 'material-ocean';
    readonly?: boolean;
    lineNumbers?: boolean;
    matchBrackets?: boolean;
    autoCloseBrackets?: boolean;
    height?: string | number;
    width?: string;
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
    width: '100%',
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
let editor: CodeMirrorNamespace.Editor | null = null;
let resizeObserver: ResizeObserver | null = null;
let codeMirrorLoader: Promise<typeof CodeMirrorNamespace> | null = null;

const normalizedHeight = computed(() => normalizeCssSize(props.height));

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
const initEditor = async () => {
  if (!textareaRef.value) return;
  if (editor) return;

  textareaRef.value.value = props.modelValue || '';
  const CodeMirror = await loadCodeMirror();
  if (!textareaRef.value || editor) return;

  editor = CodeMirror.fromTextArea(textareaRef.value, {
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

  editor.setValue(props.modelValue || '');
  editor.setSize('100%', normalizedHeight.value);

  // Listen for changes
  editor.on('change', (cm) => {
    const value = cm.getValue();
    emit('update:modelValue', value);
    emit('change', value);
  });

  nextTick(() => {
    editor?.refresh();
    observeResize();
  });
};

// Destroy editor
const destroyEditor = () => {
  resizeObserver?.disconnect();
  resizeObserver = null;
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
      editor.setSize('100%', normalizeCssSize(newHeight));
      nextTick(() => editor?.refresh());
    }
  }
);

watch(
  () => props.width,
  () => {
    nextTick(() => editor?.refresh());
  }
);

onMounted(() => {
  nextTick(() => {
    void initEditor();
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

function normalizeCssSize(value?: string | number) {
  return typeof value === 'number' ? `${value}px` : String(value || '300px');
}

function observeResize() {
  if (!containerRef.value || typeof ResizeObserver === 'undefined') return;
  resizeObserver?.disconnect();
  resizeObserver = new ResizeObserver(() => {
    editor?.refresh();
  });
  resizeObserver.observe(containerRef.value);
}

async function loadCodeMirror() {
  if (!codeMirrorLoader) {
    codeMirrorLoader = import('codemirror').then(async (module) => {
      const CodeMirror = module.default || module;
      await import('codemirror/mode/javascript/javascript');
      await import('codemirror/mode/xml/xml');
      await import('codemirror/mode/css/css');
      await import('codemirror/mode/htmlmixed/htmlmixed');
      await import('codemirror/mode/python/python');
      await import('codemirror/mode/sql/sql');
      await import('codemirror/mode/markdown/markdown');
      await import('codemirror/mode/clike/clike');
      await import('codemirror/addon/edit/matchbrackets');
      await import('codemirror/addon/edit/closebrackets');
      await import('codemirror/addon/selection/active-line');
      return CodeMirror;
    });
  }
  return codeMirrorLoader;
}
</script>

<style scoped lang="scss">
.code-editor-container {
  display: block;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  position: relative;

  // The original textarea should be hidden but present for CodeMirror
  textarea {
    display: none;
  }

  :deep(.CodeMirror) {
    width: 100%;
    height: 100%;
    box-sizing: border-box;
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
