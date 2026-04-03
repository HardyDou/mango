/**
 * Validation Utilities - Legacy wrapper
 *
 * @deprecated Use toolsValidate instead. This file is kept for backward compatibility.
 * All validation functions now delegate to toolsValidate for consistent regex implementations.
 */

// Re-export all functions from toolsValidate for backward compatibility
export {
  verifyUrl as validateUrl,
  verifyMobile as validatePhone,
  verifyEmail as validateEmail,
  verifyIdCard as validateIdCard,
  verifyIP as validateIP,
  verifyPassword as validatePassword,
  isEmpty,
  hasChinese,
  trim,
} from './toolsValidate';

// Alias for backward compatibility
export const validateNull = isEmpty;
