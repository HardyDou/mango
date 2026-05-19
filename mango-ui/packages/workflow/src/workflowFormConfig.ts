import type { WorkflowApplyRenderMode, WorkflowDefinition } from './api/workflow';

export interface WorkflowCustomFormConfig {
  submitPath: string;
  viewPath: string;
  applyPageKey?: string;
  approvePageKey?: string;
}

export interface WorkflowFormConfig {
  mode: WorkflowApplyRenderMode;
  rules: any[];
  fields: any[];
  customConfig: WorkflowCustomFormConfig;
}

export function parseWorkflowFormConfig(formJson?: string): WorkflowFormConfig {
  const emptyConfig = defaultWorkflowFormConfig();
  if (!formJson) {
    return emptyConfig;
  }
  try {
    const parsed = JSON.parse(formJson);
    if (Array.isArray(parsed)) {
      return { ...emptyConfig, rules: parsed, fields: parsed };
    }
    const mode = parsed?.mode === 'CUSTOM' || parsed?.mode === 'CUSTOM_PAGE' ? 'CUSTOM_PAGE' : 'DYNAMIC_FORM';
    const rules = Array.isArray(parsed?.rules) ? parsed.rules : [];
    const fields = Array.isArray(parsed?.fields) ? parsed.fields : rules;
    return {
      mode,
      rules,
      fields,
      customConfig: normalizeCustomConfig(parsed?.customConfig || parsed),
    };
  } catch {
    return emptyConfig;
  }
}

export function customApplyRouteOf(definition: WorkflowDefinition) {
  const config = parseWorkflowFormConfig(definition.formJson);
  if (config.mode !== 'CUSTOM_PAGE' || !config.customConfig.submitPath) {
    return null;
  }
  return {
    path: config.customConfig.submitPath,
    query: {
      definitionId: definition.id,
      definitionKey: definition.definitionKey,
      applyPageKey: config.customConfig.applyPageKey,
    },
  };
}

function defaultWorkflowFormConfig(): WorkflowFormConfig {
  return {
    mode: 'DYNAMIC_FORM',
    rules: [],
    fields: [],
    customConfig: {
      submitPath: '',
      viewPath: '',
    },
  };
}

function normalizeCustomConfig(value: any): WorkflowCustomFormConfig {
  return {
    submitPath: String(value?.submitPath || value?.createPath || ''),
    viewPath: String(value?.viewPath || value?.detailPath || ''),
    applyPageKey: optionalString(value?.applyPageKey),
    approvePageKey: optionalString(value?.approvePageKey),
  };
}

function optionalString(value: unknown) {
  const text = String(value || '').trim();
  return text || undefined;
}
