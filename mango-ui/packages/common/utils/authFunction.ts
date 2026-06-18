import { Session } from './storage';

/**
 * User information interface
 */
export interface UserInfo {
  username: string;
  nickname: string;
  photo: string;
  time: number;
  roles: string[];
  permissions: string[];
  buttonRules?: ButtonDisplayRule[];
  authBtnList: string[];
  tenantId: string;
  tenantName: string;
}

export interface ButtonRuleContext {
  row?: unknown;
  pageState?: unknown;
  query?: unknown;
  selectedRows?: unknown[];
}

export interface ButtonDisplayRule {
  code: string;
  displayRule?: string;
  buttonType?: 'TABLE' | 'NON_TABLE' | string;
}

export interface AuthRuleBindingValue extends ButtonRuleContext {
  code: string;
  permissions?: string[];
  buttonRules?: ButtonDisplayRule[];
  displayRule?: string;
}

type RuleTokenType = 'identifier' | 'string' | 'number' | 'boolean' | 'null' | 'operator' | 'paren' | 'dot' | 'eof';

interface RuleToken {
  type: RuleTokenType;
  value: string;
}

const RULE_CONTEXT_KEYS = new Set(['row', 'pageState', 'query', 'selectedRows']);
const RULE_BINARY_OPERATORS = new Set(['||', '&&', '===', '!==', '==', '!=', '>=', '<=', '>', '<']);
const RULE_OPERATOR_PRECEDENCE: Record<string, number> = {
  '||': 1,
  '&&': 2,
  '==': 3,
  '!=': 3,
  '===': 3,
  '!==': 3,
  '>': 4,
  '>=': 4,
  '<': 4,
  '<=': 4,
};

/**
 * 检查是否有权限
 * @param permission 权限码
 *
 * IMPORTANT: 前端权限检查仅用于 UI 渲染，不作为安全判断。
 * 所有 API 操作的安全校验必须在后端完成。
 * 后端返回的 permissions 列表应包含用户所有有效权限（非 '*' 通配符）。
 */
export function hasPermission(permission: string): boolean {
  const userInfo = Session.get('userInfo') as UserInfo | null;
  if (!userInfo) return false;

  const { permissions = [] } = userInfo;

  // Issue 009: 不再信任 '*' 作为超级权限标志
  // 后端应返回具体权限码列表，而非 '*' 通配符
  // 前端 UI 显示仅作为辅助，真正的安全控制在后端 API 层

  return permissions.includes(permission);
}

/**
 * 检查是否有任意一个权限
 * @param permissionList 权限码列表
 */
export function hasAnyPermission(permissionList: string[]): boolean {
  return permissionList.some((permission) => hasPermission(permission));
}

/**
 * 检查是否有所有权限
 * @param permissionList 权限码列表
 */
export function hasAllPermissions(permissionList: string[]): boolean {
  return permissionList.every((permission) => hasPermission(permission));
}

/**
 * 获取用户权限列表
 */
export function getPermissions(): string[] {
  const userInfo = Session.get('userInfo') as UserInfo | null;
  return userInfo?.permissions || [];
}

/**
 * 获取当前登录人的按钮展示规则。
 */
export function getButtonRules(): ButtonDisplayRule[] {
  const userInfo = Session.get('userInfo') as UserInfo | null;
  return Array.isArray(userInfo?.buttonRules) ? userInfo.buttonRules : [];
}

/**
 * 解析按钮展示表达式。
 *
 * 注意：这里仅用于前端 UI 显示控制，不是安全边界；真实权限仍必须由后端接口校验。
 * 空规则默认显示，表达式异常时隐藏，避免错误配置把不该展示的按钮暴露出来。
 */
export function evaluateButtonDisplayRule(rule?: string, context: ButtonRuleContext = {}): boolean {
  const expression = (rule || '').trim();
  if (!expression) {
    return true;
  }

  try {
    const parser = new ButtonRuleParser(expression, context);
    return Boolean(parser.evaluate());
  } catch {
    return false;
  }
}

class ButtonRuleParser {
  private readonly tokens: RuleToken[];
  private index = 0;

  constructor(expression: string, private readonly context: ButtonRuleContext) {
    this.tokens = tokenizeButtonRule(expression);
  }

  evaluate(): unknown {
    const value = this.parseExpression(0);
    this.expect('eof');
    return value;
  }

  private parseExpression(minPrecedence: number): unknown {
    let left = this.parseUnary();
    while (this.current().type === 'operator' && RULE_BINARY_OPERATORS.has(this.current().value)) {
      const operator = this.current().value;
      const precedence = RULE_OPERATOR_PRECEDENCE[operator];
      if (precedence < minPrecedence) {
        break;
      }
      this.advance();
      const right = this.parseExpression(precedence + 1);
      left = applyBinaryOperator(operator, left, right);
    }
    return left;
  }

  private parseUnary(): unknown {
    if (this.match('operator', '!')) {
      return !this.parseUnary();
    }
    return this.parsePrimary();
  }

  private parsePrimary(): unknown {
    const token = this.current();
    if (this.match('paren', '(')) {
      const value = this.parseExpression(0);
      this.expect('paren', ')');
      return value;
    }
    if (token.type === 'string') {
      this.advance();
      return token.value;
    }
    if (token.type === 'number') {
      this.advance();
      return Number(token.value);
    }
    if (token.type === 'boolean') {
      this.advance();
      return token.value === 'true';
    }
    if (token.type === 'null') {
      this.advance();
      return null;
    }
    if (token.type === 'identifier') {
      return this.parseIdentifierPath();
    }
    throw new Error(`Unsupported button rule token: ${token.value}`);
  }

  private parseIdentifierPath(): unknown {
    const root = this.expect('identifier').value;
    if (!RULE_CONTEXT_KEYS.has(root)) {
      throw new Error(`Unsupported button rule root: ${root}`);
    }

    let value = this.resolveRoot(root);
    while (this.match('dot')) {
      const property = this.expect('identifier').value;
      value = readProperty(value, property);
    }
    return value;
  }

  private resolveRoot(root: string): unknown {
    if (root === 'selectedRows') {
      return this.context.selectedRows || [];
    }
    return this.context[root as keyof ButtonRuleContext];
  }

  private current(): RuleToken {
    return this.tokens[this.index] || { type: 'eof', value: '' };
  }

  private advance(): RuleToken {
    const token = this.current();
    this.index += 1;
    return token;
  }

  private match(type: RuleTokenType, value?: string): boolean {
    const token = this.current();
    if (token.type !== type || (value !== undefined && token.value !== value)) {
      return false;
    }
    this.advance();
    return true;
  }

  private expect(type: RuleTokenType, value?: string): RuleToken {
    const token = this.current();
    if (token.type !== type || (value !== undefined && token.value !== value)) {
      throw new Error(`Expected ${value || type}`);
    }
    return this.advance();
  }
}

function tokenizeButtonRule(expression: string): RuleToken[] {
  const tokens: RuleToken[] = [];
  let index = 0;

  while (index < expression.length) {
    const char = expression[index];
    if (/\s/.test(char)) {
      index += 1;
      continue;
    }
    if (char === '"' || char === "'") {
      const [value, nextIndex] = readStringToken(expression, index);
      tokens.push({ type: 'string', value });
      index = nextIndex;
      continue;
    }
    if (/\d/.test(char)) {
      const match = expression.slice(index).match(/^\d+(?:\.\d+)?/);
      if (!match) {
        throw new Error('Invalid number');
      }
      tokens.push({ type: 'number', value: match[0] });
      index += match[0].length;
      continue;
    }
    if (/[A-Za-z_$]/.test(char)) {
      const match = expression.slice(index).match(/^[A-Za-z_$][\w$]*/);
      if (!match) {
        throw new Error('Invalid identifier');
      }
      const value = match[0];
      if (value === 'true' || value === 'false') {
        tokens.push({ type: 'boolean', value });
      } else if (value === 'null') {
        tokens.push({ type: 'null', value });
      } else {
        tokens.push({ type: 'identifier', value });
      }
      index += value.length;
      continue;
    }
    const threeCharOperator = expression.slice(index, index + 3);
    if (threeCharOperator === '===' || threeCharOperator === '!==') {
      tokens.push({ type: 'operator', value: threeCharOperator });
      index += 3;
      continue;
    }
    const twoCharOperator = expression.slice(index, index + 2);
    if (['&&', '||', '==', '!=', '>=', '<='].includes(twoCharOperator)) {
      tokens.push({ type: 'operator', value: twoCharOperator });
      index += 2;
      continue;
    }
    if (['!', '>', '<'].includes(char)) {
      tokens.push({ type: 'operator', value: char });
      index += 1;
      continue;
    }
    if (char === '(' || char === ')') {
      tokens.push({ type: 'paren', value: char });
      index += 1;
      continue;
    }
    if (char === '.') {
      tokens.push({ type: 'dot', value: char });
      index += 1;
      continue;
    }
    throw new Error(`Unsupported button rule character: ${char}`);
  }

  tokens.push({ type: 'eof', value: '' });
  return tokens;
}

function readStringToken(expression: string, start: number): [string, number] {
  const quote = expression[start];
  let value = '';
  let index = start + 1;
  while (index < expression.length) {
    const char = expression[index];
    if (char === '\\') {
      const next = expression[index + 1];
      if (next === undefined) {
        throw new Error('Invalid string escape');
      }
      value += next;
      index += 2;
      continue;
    }
    if (char === quote) {
      return [value, index + 1];
    }
    value += char;
    index += 1;
  }
  throw new Error('Unterminated string');
}

function readProperty(value: unknown, property: string): unknown {
  if (property === '__proto__' || property === 'prototype' || property === 'constructor') {
    throw new Error('Unsupported property');
  }
  if (value === null || value === undefined) {
    return undefined;
  }
  if (Array.isArray(value) && property === 'length') {
    return value.length;
  }
  if (typeof value === 'object') {
    return (value as Record<string, unknown>)[property];
  }
  return undefined;
}

function applyBinaryOperator(operator: string, left: unknown, right: unknown): boolean {
  if (operator === '&&') {
    return Boolean(left) && Boolean(right);
  }
  if (operator === '||') {
    return Boolean(left) || Boolean(right);
  }
  if (operator === '===') {
    return left === right;
  }
  if (operator === '!==') {
    return left !== right;
  }
  if (operator === '==') {
    // eslint-disable-next-line eqeqeq
    return left == right;
  }
  if (operator === '!=') {
    // eslint-disable-next-line eqeqeq
    return left != right;
  }
  if (operator === '>') {
    return compareValues(left, right) > 0;
  }
  if (operator === '>=') {
    return compareValues(left, right) >= 0;
  }
  if (operator === '<') {
    return compareValues(left, right) < 0;
  }
  if (operator === '<=') {
    return compareValues(left, right) <= 0;
  }
  throw new Error(`Unsupported button rule operator: ${operator}`);
}

function compareValues(left: unknown, right: unknown): number {
  if (typeof left === 'number' && typeof right === 'number') {
    return left - right;
  }
  return String(left).localeCompare(String(right));
}

function isRuleBindingValue(value: unknown): value is AuthRuleBindingValue {
  return Boolean(value)
    && typeof value === 'object'
    && !Array.isArray(value)
    && typeof (value as AuthRuleBindingValue).code === 'string';
}

function resolveButtonRule(value: AuthRuleBindingValue): string {
  if (typeof value.displayRule === 'string') {
    return value.displayRule;
  }

  const rules = Array.isArray(value.buttonRules) ? value.buttonRules : getButtonRules();
  return rules.find(item => item.code === value.code)?.displayRule || '';
}

/**
 * 按钮是否展示：先校验权限码，再校验展示规则。
 */
export function canShowButton(value: string | AuthRuleBindingValue): boolean {
  if (typeof value === 'string') {
    return hasPermission(value);
  }
  if (!isRuleBindingValue(value)) {
    return false;
  }

  const permissions = Array.isArray(value.permissions) ? value.permissions : getPermissions();
  const hasCodePermission = permissions.includes(value.code);
  if (!hasCodePermission) {
    return false;
  }

  return evaluateButtonDisplayRule(resolveButtonRule(value), {
    row: value.row,
    pageState: value.pageState,
    query: value.query,
    selectedRows: value.selectedRows,
  });
}

/**
 * 检查是否登录
 */
export function isLoggedIn(): boolean {
  return !!Session.getToken();
}

/**
 * 获取用户信息
 */
export function getUserInfo(): UserInfo | null {
  return Session.get('userInfo') as UserInfo | null;
}

/**
 * 获取用户名
 */
export function getUsername(): string {
  const userInfo = getUserInfo();
  return userInfo?.username || '未知用户';
}

// 权限检查别名（供 directive 使用）
export const auth = hasPermission;
export const auths = hasAnyPermission;
export const authAll = hasAllPermissions;
