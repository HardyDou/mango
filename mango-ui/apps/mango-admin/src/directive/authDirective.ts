/**
 * Auth Directive
 *
 * Frontend permission display control directive.
 *
 * IMPORTANT: This directive only controls UI display, NOT security.
 * All button operations must be enforced by backend API layer.
 * Frontend permission can be bypassed via console, so backend auth is the only trusted control point.
 *
 * Issue 010 Fix: Use watch + nextTick to ensure authBtnList is loaded before checking permissions.
 * This prevents race conditions where the directive checks permissions before the data arrives.
 */

import type { Directive, DirectiveBinding, App, Ref } from 'vue';
import { nextTick, watch } from 'vue';
import { useUserInfo } from '@/stores/userInfo';
import { auth, auths, authAll } from '@mango/common';

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

/**
 * Check if element should be visible based on auth directive
 */
function checkAuth(binding: AuthDirectiveBinding, authBtnList: string[]): boolean {
  const { value, modifiers } = binding;

  // No value means show element (no restriction)
  if (!value) return true;

  // If authBtnList is empty, don't hide (waiting for data to load)
  if (!authBtnList || authBtnList.length === 0) return true;

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
 * Create auth directive with proper watch handling
 */
function createAuthDirective(): Directive {
  // Track if we've ever checked permissions (to avoid infinite loops)
  let hasCheckedOnce = false;
  // Timeout handle for cleanup
  let timeoutHandle: ReturnType<typeof setTimeout> | null = null;

  return {
    mounted(el: HTMLElement, binding: AuthDirectiveBinding) {
      const userInfoStore = useUserInfo();

      // Get the authBtnList as a reactive reference
      const authBtnListRef = userInfoStore.userInfos.authBtnList;

      // Initial check (may show element if data not loaded yet)
      const initialCheck = () => {
        updateAndCheck();
      };

      // Update cached list and check auth
      const updateAndCheck = () => {
        try {
          const authBtnList = userInfoStore.userInfos.authBtnList || [];
          if (!checkAuth(binding, authBtnList)) {
            removeElement(el);
          }
        } catch {
          // Store not available yet, show element
        }
      };

      // Issue 010 Fix: Use watch to observe authBtnList changes
      // This ensures we check permissions AFTER data is loaded
      const stopWatch = watch(
        () => userInfoStore.userInfos.authBtnList,
        (newList, oldList) => {
          // Only react to actual changes (not initial undefined state)
          if (newList !== oldList) {
            hasCheckedOnce = true;
            nextTick(() => {
              updateAndCheck();
              // After first successful check with data, stop watching
              if (newList && newList.length > 0) {
                stopWatch();
                if (timeoutHandle) {
                  clearTimeout(timeoutHandle);
                }
              }
            });
          }
        },
        { immediate: false }
      );

      // Initial check
      initialCheck();

      // Safety timeout: if authBtnList never loads (e.g., request failed),
      // stop watching after 10 seconds to avoid memory leaks
      timeoutHandle = setTimeout(() => {
        stopWatch();
        hasCheckedOnce = false;
      }, 10000);
    },

    updated(el: HTMLElement, binding: AuthDirectiveBinding) {
      // Re-check on updates (binding value may have changed)
      nextTick(() => {
        try {
          const userInfoStore = useUserInfo();
          const authBtnList = userInfoStore.userInfos.authBtnList || [];
          if (!checkAuth(binding, authBtnList)) {
            removeElement(el);
          }
        } catch {
          // Store not available
        }
      });
    },
  };
}

/**
 * v-auth directive - Single permission OR logic
 */
const vAuth: Directive = createAuthDirective();

/**
 * v-auths directive - OR logic (any permission match shows element)
 */
const vAuths: Directive = {
  mounted(el: HTMLElement, binding: AuthDirectiveBinding) {
    const userInfoStore = useUserInfo();

    const updateAndCheck = () => {
      try {
        const authBtnList = userInfoStore.userInfos.authBtnList || [];
        if (authBtnList.length === 0) return; // Waiting for data

        const permissions = Array.isArray(binding.value)
          ? binding.value
          : [binding.value as string];

        if (!auths(permissions)) {
          removeElement(el);
        }
      } catch {
        // Store not available
      }
    };

    // Issue 010 Fix: Use watch to observe authBtnList changes
    const stopWatch = watch(
      () => userInfoStore.userInfos.authBtnList,
      (newList) => {
        if (newList && newList.length > 0) {
          nextTick(() => {
            updateAndCheck();
            stopWatch();
          });
        }
      },
      { immediate: true }
    );
  },

  updated(el: HTMLElement, binding: AuthDirectiveBinding) {
    nextTick(() => {
      const userInfoStore = useUserInfo();
      const authBtnList = userInfoStore.userInfos.authBtnList || [];
      if (authBtnList.length === 0) return;

      const permissions = Array.isArray(binding.value)
        ? binding.value
        : [binding.value as string];

      if (!auths(permissions)) {
        removeElement(el);
      }
    });
  },
};

/**
 * v-auth-all directive - AND logic (all permissions required)
 */
const vAuthAll: Directive = {
  mounted(el: HTMLElement, binding: AuthDirectiveBinding) {
    const userInfoStore = useUserInfo();

    const updateAndCheck = () => {
      try {
        const authBtnList = userInfoStore.userInfos.authBtnList || [];
        if (authBtnList.length === 0) return; // Waiting for data

        const permissions = Array.isArray(binding.value)
          ? binding.value
          : [binding.value as string];

        if (!authAll(permissions)) {
          removeElement(el);
        }
      } catch {
        // Store not available
      }
    };

    // Issue 010 Fix: Use watch to observe authBtnList changes
    const stopWatch = watch(
      () => userInfoStore.userInfos.authBtnList,
      (newList) => {
        if (newList && newList.length > 0) {
          nextTick(() => {
            updateAndCheck();
            stopWatch();
          });
        }
      },
      { immediate: true }
    );
  },

  updated(el: HTMLElement, binding: AuthDirectiveBinding) {
    nextTick(() => {
      const userInfoStore = useUserInfo();
      const authBtnList = userInfoStore.userInfos.authBtnList || [];
      if (authBtnList.length === 0) return;

      const permissions = Array.isArray(binding.value)
        ? binding.value
        : [binding.value as string];

      if (!authAll(permissions)) {
        removeElement(el);
      }
    });
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
