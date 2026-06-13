import { reactive } from 'vue';
import type { ApiId } from '@mango/api-schema';

interface PaymentSelectOption {
  value: ApiId;
  label: string;
  description?: string;
}

export function usePaymentEntityLabels() {
  const labels = reactive<Record<string, string>>({});

  function setOptions(options: PaymentSelectOption[]) {
    options.forEach((option) => {
      labels[String(option.value)] = option.description ? `${option.label} / ${option.description}` : option.label;
    });
  }

  function labelOf(value: ApiId | string | number | null | undefined) {
    if (value === undefined || value === null || value === '') {
      return '-';
    }
    return labels[String(value)] || String(value);
  }

  function labelsOf(value: string | null | undefined) {
    if (!value) {
      return '-';
    }
    return value.split(',').map(item => labelOf(item.trim())).join('，');
  }

  return {
    labelOf,
    labelsOf,
    setOptions,
  };
}
