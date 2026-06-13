export const CHANNEL_FIELD_COMPONENTS = [
  { label: '单行文本', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '密码密钥', value: 'password' },
  { label: '文件上传', value: 'fileId' },
  { label: '枚举选择', value: 'select' },
  { label: '数字', value: 'number' },
  { label: '开关', value: 'switch' },
  { label: 'URL', value: 'url' },
  { label: '日期时间', value: 'datetime' },
  { label: 'JSON 配置', value: 'json' },
] as const;

export type ChannelFieldComponent = typeof CHANNEL_FIELD_COMPONENTS[number]['value'];
export type ChannelFieldDataType = 'string' | 'number' | 'boolean' | 'url' | 'datetime' | 'json' | 'fileId';

export interface ChannelFieldOption {
  label: string;
  value: string;
}

export interface ChannelFieldDefinition {
  name: string;
  label: string;
  component: ChannelFieldComponent;
  dataType?: ChannelFieldDataType;
  required?: boolean;
  sensitive?: boolean;
  encrypted?: boolean;
  masked?: boolean;
  validationRule?: string;
  defaultValue?: string | number | boolean;
  sort?: number;
  group?: string;
  options?: ChannelFieldOption[];
  placeholder?: string;
  description?: string;
}

export type ChannelConfigValues = Record<string, string | number | boolean | undefined>;

export function parseChannelFieldTemplate(value?: string): ChannelFieldDefinition[] {
  if (!value) {
    return [];
  }
  try {
    const parsed = JSON.parse(value);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .map(toFieldDefinition)
      .filter((item): item is ChannelFieldDefinition => Boolean(item));
  } catch {
    return [];
  }
}

export function stringifyChannelFieldTemplate(fields: ChannelFieldDefinition[]): string | undefined {
  const normalized = fields
    .map(field => ({
      name: trimToUndefined(field.name),
      label: trimToUndefined(field.label),
      component: normalizeComponent(field.component),
      dataType: normalizeDataType(field.dataType, field.component),
      required: Boolean(field.required),
      sensitive: Boolean(field.sensitive),
      encrypted: Boolean(field.encrypted),
      masked: Boolean(field.masked),
      validationRule: trimToUndefined(field.validationRule),
      defaultValue: normalizeValue(field.defaultValue),
      sort: typeof field.sort === 'number' ? field.sort : undefined,
      group: trimToUndefined(field.group),
      options: normalizeOptions(field.options),
      placeholder: trimToUndefined(field.placeholder),
      description: trimToUndefined(field.description),
    }))
    .filter(field => field.name && field.label);
  return normalized.length > 0 ? JSON.stringify(normalized) : undefined;
}

export function parseChannelConfigValues(value?: string): ChannelConfigValues {
  if (!value) {
    return {};
  }
  try {
    const parsed = JSON.parse(value);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {};
    }
    return Object.fromEntries(
      Object.entries(parsed)
        .filter(([key]) => Boolean(key))
        .map(([key, entry]) => [key, normalizeValue(entry)]),
    );
  } catch {
    return {};
  }
}

export function stringifyChannelConfigValues(values: ChannelConfigValues): string | undefined {
  const normalized = Object.fromEntries(
    Object.entries(values)
      .map(([key, value]) => [key, normalizeValue(value)])
      .filter(([, value]) => value !== undefined && value !== ''),
  );
  return Object.keys(normalized).length > 0 ? JSON.stringify(normalized) : undefined;
}

function toFieldDefinition(value: unknown): ChannelFieldDefinition | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined;
  }
  const source = value as Record<string, unknown>;
  const name = trimToUndefined(source.name);
  const label = trimToUndefined(source.label);
  if (!name || !label) {
    return undefined;
  }
  return {
    name,
    label,
    component: normalizeComponent(source.component),
    dataType: normalizeDataType(source.dataType, normalizeComponent(source.component)),
    required: Boolean(source.required),
    sensitive: Boolean(source.sensitive),
    encrypted: Boolean(source.encrypted),
    masked: Boolean(source.masked),
    validationRule: trimToUndefined(source.validationRule),
    defaultValue: normalizeValue(source.defaultValue),
    sort: normalizeSort(source.sort),
    group: trimToUndefined(source.group),
    options: normalizeOptions(source.options),
    placeholder: trimToUndefined(source.placeholder),
    description: trimToUndefined(source.description),
  };
}

function normalizeComponent(value: unknown): ChannelFieldComponent {
  const text = String(value || '').trim();
  return CHANNEL_FIELD_COMPONENTS.some(item => item.value === text) ? text as ChannelFieldComponent : 'input';
}

function normalizeDataType(value: unknown, component?: unknown): ChannelFieldDataType {
  const text = String(value || '').trim();
  const componentText = String(component || '').trim();
  if (['string', 'number', 'boolean', 'url', 'datetime', 'json', 'fileId'].includes(text)) {
    return text as ChannelFieldDataType;
  }
  if (componentText === 'number') return 'number';
  if (componentText === 'switch') return 'boolean';
  if (componentText === 'url') return 'url';
  if (componentText === 'datetime') return 'datetime';
  if (componentText === 'json') return 'json';
  if (componentText === 'fileId') return 'fileId';
  return 'string';
}

function normalizeOptions(value: unknown): ChannelFieldOption[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }
  const options = value
    .map((item) => {
      if (typeof item === 'string') {
        return { label: item, value: item };
      }
      if (!item || typeof item !== 'object' || Array.isArray(item)) {
        return undefined;
      }
      const source = item as Record<string, unknown>;
      const label = trimToUndefined(source.label);
      const optionValue = trimToUndefined(source.value);
      return label && optionValue ? { label, value: optionValue } : undefined;
    })
    .filter((item): item is ChannelFieldOption => Boolean(item));
  return options.length > 0 ? options : undefined;
}

function normalizeSort(value: unknown): number | undefined {
  if (typeof value === 'number') {
    return value;
  }
  const text = trimToUndefined(value);
  if (!text) {
    return undefined;
  }
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function normalizeValue(value: unknown): string | number | boolean | undefined {
  if (typeof value === 'boolean' || typeof value === 'number') {
    return value;
  }
  const text = String(value || '').trim();
  return text || undefined;
}

function trimToUndefined(value: unknown): string | undefined {
  const text = String(value || '').trim();
  return text || undefined;
}
