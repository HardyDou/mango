/**
 * Auth Directive
 *
 * Frontend permission display control directive.
 *
 * IMPORTANT: This directive only controls UI display, NOT security.
 * All button operations must be enforced by backend mango-permission microservice.
 * Frontend permission can be bypassed via console, so backend auth is the only trusted control point.
 */

import type { Directive, DirectiveBinding, App } from 'vue';
import { useUserInfo } from '@/stores/userInfo';
import { auth, auths, authAll } from '@/utils/authFunction';

interface AuthDirectiveBinding extends DirectiveBinding {
  value?: string | string[];
  modifiers?: {
    all?: boolean;
    some?: boolean;
  };
}

/**
 * v-auth: Check single permission (OR logic - any match)
 * v-auth-some: Alias for v-auth (OR logic)
 * v-auth-all: Check multiple permissions (AND logic - all must match)
 *
 * Usage:
 * <el-button v-auth="'user:add'">Add User</el-button>
 * <el-button v-auth="['user:add', 'user:edit']">Add or Edit</el-button>
 * <el-button v-auth.all="['user:add', 'user:edit']">Add AND Edit</el-button>
 */

// Store authBtnList in a module-level variable to avoid multiple store subscriptions
let cachedAuthBtnList: string[] = [];

/**
 * Update cached authBtnList from store
 */
function updateCachedAuthBtnList(): void {
  try {
    const userInfoStore = useUserInfo();
    cachedAuthBtnList = userInfoStore.userInfos.authBtnList || [];
  } catch {
    // Store not available yet
    cachedAuthBtnList = [];
  }
}

/**
 * Check if element should be visible based on auth directive
 */
function checkAuth(binding: AuthDirectiveBinding): boolean {
  const { value, modifiers } = binding;

  // No value means show element (no restriction)
  if (!value) return true;

  // Get latest authBtnList from store
  updateCachedAuthBtnList();

  // If authBtnList is empty, don't hide (waiting for data to load)
  if (cachedAuthBtnList.length === 0) return true;

  // Determine check type: v-auth-all requires ALL permissions
  const requireAll = modifiers?.all === true;

  if (Array.isArray(value)) {
    if (requireAll) {
      return authAll(value);
    }
    return auths(value);
  }

  // Single string value - use auth (OR logic)
  return auth(value as string);
}

/**
 * Remove element from DOM
 */
function removeElement(el: HTMLElement): void {
  const parent = el.parentNode;
  if (parent) {
    parent.removeChild(el);
  }
}

/**
 * v-auth directive
 * - Single permission: OR logic (any match shows)
 * - Array of permissions: OR logic (any match shows)
 * - v-auth.all: AND logic (all must match)
 */
const vAuth: Directive = {
  mounted(el: HTMLElement, binding: AuthDirectiveBinding) {
    if (!checkAuth(binding)) {
      removeElement(el);
    }
  },
  updated(el: HTMLElement, binding: AuthDirectiveBinding) {
    if (!checkAuth(binding)) {
      removeElement(el);
    }
  },
};

/**
 * v-auths directive (alias for v-auth with OR logic)
 */
const vAuths: Directive = {
  mounted(el: HTMLElement, binding: AuthDirectiveBinding) {
    if (!auths(binding.value as string[])) {
      removeElement(el);
    }
  },
  updated(el: HTMLElement, binding: AuthDirectiveBinding) {
    if (!auths(binding.value as string[])) {
      removeElement(el);
    }
  },
};

/**
 * v-auth-all directive (AND logic - all permissions required)
 */
const vAuthAll: Directive = {
  mounted(el: HTMLElement, binding: AuthDirectiveBinding) {
    if (!authAll(binding.value as string[])) {
      removeElement(el);
    }
  },
  updated(el: HTMLElement, binding: AuthDirectiveBinding) {
    if (!authAll(binding.value as string[])) {
      removeElement(el);
    }
  },
};

/**
 * Register all auth directives
 */
export function registerAuthDirectives(app: App): void {
  app.directive('auth', vAuth);
  app.directive('auths', vAuths);
  app.directive('auth-all', vAuthAll);
}

export { vAuth, vAuths, vAuthAll };
