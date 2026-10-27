import { computed, onMounted, ref, unref, watch, type Ref } from 'vue';
import { clearDictOptionCache, listDictOptions, type DictOption } from '../api/dict';

export type DictTypeRef = string | Ref<string | undefined>;

export function useDict(typeCode: DictTypeRef) {
  const options = ref<DictOption[]>([]);
  const loading = ref(false);

  const resolvedTypeCode = computed(() => unref(typeCode));

  const load = async (force = false) => {
    const code = resolvedTypeCode.value;
    if (!code) {
      options.value = [];
      return options.value;
    }

    loading.value = true;
    try {
      // 选项缓存统一由 api 层维护，避免 hook 与 api 两份缓存不同步。
      const data = await listDictOptions(code, force);
      options.value = data;
      return data;
    } finally {
      loading.value = false;
    }
  };

  const getLabel = (value: string | number | boolean | null | undefined) => {
    if (value === null || value === undefined || value === '') {
      return '';
    }
    const normalized = String(value);
    return options.value.find((item) => String(item.value) === normalized)?.label || normalized;
  };

  const getOption = (value: string | number | boolean | null | undefined) => {
    if (value === null || value === undefined || value === '') {
      return undefined;
    }
    const normalized = String(value);
    return options.value.find((item) => String(item.value) === normalized);
  };

  watch(
    resolvedTypeCode,
    () => {
      load();
    },
    { immediate: true }
  );

  onMounted(() => {
    load();
  });

  return {
    options,
    loading,
    load,
    getLabel,
    getOption,
  };
}

export function clearDictCache(typeCode?: string) {
  clearDictOptionCache(typeCode);
}
