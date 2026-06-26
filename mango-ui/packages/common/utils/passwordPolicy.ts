export interface PasswordPolicy {
  minLength: number;
  requireLetter: boolean;
  requireDigit: boolean;
  requireSpecialChar: boolean;
  allowWhitespace: boolean;
  pattern?: string;
}

export interface PasswordRuleResult {
  key: string;
  label: string;
  passed: boolean;
}

export type PasswordStrength = 'empty' | 'weak' | 'medium' | 'strong';

export const defaultPasswordPolicy: PasswordPolicy = {
  minLength: 8,
  requireLetter: true,
  requireDigit: true,
  requireSpecialChar: false,
  allowWhitespace: false,
  pattern: '',
};

export function evaluatePasswordRules(password: string, policy: PasswordPolicy = defaultPasswordPolicy): PasswordRuleResult[] {
  const value = password || '';
  const rules: PasswordRuleResult[] = [
    {
      key: 'minLength',
      label: `至少 ${policy.minLength} 位`,
      passed: value.length >= policy.minLength,
    },
  ];
  if (policy.requireLetter) {
    rules.push({ key: 'letter', label: '包含字母', passed: /[A-Za-z]/.test(value) });
  }
  if (policy.requireDigit) {
    rules.push({ key: 'digit', label: '包含数字', passed: /\d/.test(value) });
  }
  if (policy.requireSpecialChar) {
    rules.push({ key: 'special', label: '包含特殊字符', passed: /[^A-Za-z0-9\s]/.test(value) });
  }
  if (!policy.allowWhitespace) {
    rules.push({ key: 'whitespace', label: '不能包含空白字符', passed: !/\s/.test(value) });
  }
  if (policy.pattern) {
    rules.push({ key: 'pattern', label: '符合自定义规则', passed: new RegExp(policy.pattern).test(value) });
  }
  return rules;
}

export function isPasswordPolicyPassed(password: string, policy: PasswordPolicy = defaultPasswordPolicy) {
  return evaluatePasswordRules(password, policy).every((rule) => rule.passed);
}

export function getPasswordPolicyMessage(policy: PasswordPolicy = defaultPasswordPolicy) {
  return evaluatePasswordRules('', policy).map((rule) => rule.label).join('，');
}

export function getPasswordStrength(password: string, policy: PasswordPolicy = defaultPasswordPolicy): PasswordStrength {
  if (!password) return 'empty';
  const passedCount = evaluatePasswordRules(password, policy).filter((rule) => rule.passed).length;
  const variety = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9\s]/].filter((rule) => rule.test(password)).length;
  const score = passedCount + Math.max(0, variety - 1);
  if (!isPasswordPolicyPassed(password, policy) || score <= 2) return 'weak';
  if (score <= 4) return 'medium';
  return 'strong';
}
