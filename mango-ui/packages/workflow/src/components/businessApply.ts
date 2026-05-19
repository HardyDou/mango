import type { Component } from 'vue';
import type { WorkflowDefinition } from '../api/workflow';

export interface BusinessApplyContext {
  definitionId: string;
  definitionKey?: string;
  applyPageKey: string;
  definition?: WorkflowDefinition | null;
  query: Record<string, unknown>;
}

export interface BusinessApplyRegistration {
  component: Component;
  title?: string;
}

const businessApplyRegistrations = new Map<string, BusinessApplyRegistration>();

export function registerBusinessApplyComponent(key: string, registration: BusinessApplyRegistration) {
  const normalizedKey = normalizeRegistryKey(key);
  if (!normalizedKey) {
    return;
  }
  businessApplyRegistrations.set(normalizedKey, registration);
}

export function registerBusinessApplyComponents(registrations: Record<string, BusinessApplyRegistration>) {
  Object.entries(registrations).forEach(([key, registration]) => {
    registerBusinessApplyComponent(key, registration);
  });
}

export function resolveBusinessApplyRegistration(key?: string): BusinessApplyRegistration | null {
  return businessApplyRegistrations.get(normalizeRegistryKey(key)) || null;
}

function normalizeRegistryKey(key?: string) {
  return String(key || '').trim();
}
