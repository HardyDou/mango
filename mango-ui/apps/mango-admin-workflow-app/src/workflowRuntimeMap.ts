import { getPageLoader, normalizeComponentPath } from '@mango/admin-pages';

export const WORKFLOW_MODULE_CODE = 'mango-workflow';

export function resolveWorkflowComponent(componentPath?: string) {
  return getPageLoader(WORKFLOW_MODULE_CODE, normalizeComponentPath(componentPath));
}
