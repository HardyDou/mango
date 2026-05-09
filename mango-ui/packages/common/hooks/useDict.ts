import { computed, onMounted, ref, unref, watch, type Ref } from 'vue';
import { listDictOptions, type DictOption } from '../api/dict';

const dictCache = new Map<string, DictOption[]>();
const loadingCache = new Map<string, Promise<DictOption[]>>();

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

    if (!force && dictCache.has(code)) {
      options.value = dictCache.get(code) || [];
      return options.value;
    }

    loading.value = true;
    try {
      const request = force || !loadingCache.has(code)
        ? listDictOptions(code)
        : loadingCache.get(code)!;
      loadingCache.set(code, request);
      const data = await request;
      dictCache.set(code, data);
      options.value = data;
      return data;
    } finally {
      loading.value = false;
      loadingCache.delete(code);
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
  if (typeCode) {
    dictCache.delete(typeCode);
    loadingCache.delete(typeCode);
    return;
  }
  dictCache.clear();
  loadingCache.clear();
}
