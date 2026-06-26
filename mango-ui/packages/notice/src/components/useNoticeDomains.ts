import { computed, ref } from 'vue';
import { getNoticeDomains } from '../api/notice';
import type { NoticeDomainOption } from '../types/notice';

export interface NoticeDomainSelectOption {
  label: string;
  value: string;
}

export function useNoticeDomains() {
  const loading = ref(false);
  const domains = ref<NoticeDomainOption[]>([]);

  const flatDomains = computed(() => flattenDomainOptions(domains.value));

  const domainOptions = computed<NoticeDomainSelectOption[]>(() => flatDomains.value
    .filter(item => Boolean(item.domainCode))
    .map(item => ({
      label: domainDisplayName(item),
      value: item.domainCode,
    }))
    .sort((left, right) => left.label.localeCompare(right.label, 'zh-CN')));

  function domainText(code?: string) {
    const normalizedCode = code?.trim();
    if (!normalizedCode) return '-';
    const domain = flatDomains.value.find(item => item.domainCode === normalizedCode);
    return domain ? domainDisplayName(domain) : normalizedCode;
  }

  async function loadDomains() {
    loading.value = true;
    try {
      domains.value = await getNoticeDomains();
    } finally {
      loading.value = false;
    }
  }

  return {
    domainLoading: loading,
    domains,
    domainOptions,
    domainText,
    loadDomains,
  };
}

function flattenDomainOptions(options: NoticeDomainOption[]): NoticeDomainOption[] {
  return options.flatMap(item => [item, ...flattenDomainOptions(item.children || [])]);
}

function domainDisplayName(domain: NoticeDomainOption) {
  return domain.domainName && domain.domainName !== domain.domainCode
    ? `${domain.domainName}（${domain.domainCode}）`
    : domain.domainCode;
}
