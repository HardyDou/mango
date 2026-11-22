/**
 * 数据权限 Grant（`X-Mango-Data-Grant`）的本地缓存与请求头装配。
 *
 * 后端在列表响应里签发 Grant，详情请求必须原样回传它。前端没有 domain 概念、也不允许按 URL 前缀猜域，
 * 因此这里只按后端下发的 `X-Mango-Data-Grant-Domain` 建键，一条响应最多维护一个键。
 *
 * 约定（fail-closed）：
 *
 * - 只接受更大的 `seq`，乱序到达的旧响应不会覆盖新 Grant；
 * - 缓存项绑定 `userId` / `tenantId`，发送前比对，身份不一致直接丢弃并清键；
 * - `absent`、`oversize`、`scope-unresolved` 以及任何未知的 Skipped 取值都删除该域的键，不做 stale 标记；
 * - 超限按「最近使用优先」截断，绝不做「超限就一个都不发」的 fail-open。
 */

/** 请求头：Grant 多值，逗号分隔。 */
export const DATA_GRANT_HEADER = 'X-Mango-Data-Grant';

/** 响应头：Grant 所属数据域，前端只把它当缓存键。 */
export const DATA_GRANT_DOMAIN_HEADER = 'X-Mango-Data-Grant-Domain';

/** 响应头：缺少 Grant 且需要刷新，值为 `1`。 */
export const DATA_GRANT_REQUIRED_HEADER = 'X-Mango-Data-Grant-Required';

/** 响应头：未签发 Grant 的原因。 */
export const DATA_GRANT_SKIPPED_HEADER = 'X-Mango-Data-Grant-Skipped';

/** `X-Mango-Data-Grant` 的「未携带」标记值。 */
export const DATA_GRANT_ABSENT = 'absent';

/** 单次请求最多回传的 Grant 个数，与后端上限一致。 */
export const MAX_DATA_GRANT_COUNT = 16;

/** 单次请求 Grant 头序列化后的最大字符数，与后端上限一致。 */
export const MAX_DATA_GRANT_LENGTH = 4096;

/** 缓存项绑定的请求身份。 */
export interface DataGrantIdentity {
  readonly userId?: string | number | null;
  readonly tenantId?: string | number | null;
}

/** 可回传的 Grant 头内容。 */
export interface DataGrantHeaderValue {
  /** 逗号分隔的多值头。 */
  readonly value: string;
  /** 本次实际携带的 domain，用于「Required:1 但没带」的 dev 断言。 */
  readonly domains: string[];
}

/** 处理一次响应头所需的上下文。 */
export interface DataGrantResponseInput {
  /** 响应头，读取时大小写不敏感。 */
  readonly headers?: unknown;
  /** 当前请求身份。 */
  readonly identity?: DataGrantIdentity;
  /** 本次请求发出时取的序列号。 */
  readonly seq?: number;
  /** 本次请求携带了哪些 domain 的 Grant；不传表示该请求不参与接力（如下载流）。 */
  readonly sentDomains?: readonly string[];
}

interface DataGrantEntry {
  grant: string;
  seq: number;
  userId: string | null;
  tenantId: string | null;
  /** 最近使用序，仅用于超限截断时的排序。 */
  used: number;
}

const entries = new Map<string, DataGrantEntry>();

let sequence = 0;

let useSequence = 0;

/** 旧后端缺少 `X-Mango-Data-Grant-Domain` 时只提示一次，避免每个响应都刷屏。 */
let warnedMissingDomain = false;

function nextSequence(): number {
  sequence += 1;
  return sequence;
}

function devWarn(message: string): void {
  if (import.meta.env.DEV) {
    console.warn(`[mango:data-grant] ${message}`);
  }
}

function normalizeIdentityValue(value: string | number | null | undefined): string | null {
  if (value === null || value === undefined) return null;
  const text = String(value).trim();
  return text === '' ? null : text;
}

function readHeader(headers: unknown, name: string): string | undefined {
  if (!headers) return undefined;
  const bag = headers as Record<string, unknown> & { get?: (key: string) => unknown };
  let value = bag[name] ?? bag[name.toLowerCase()];
  if (value === undefined && typeof bag.get === 'function') {
    value = bag.get(name);
  }
  if (value === undefined || value === null) return undefined;
  if (Array.isArray(value)) return value.length === 0 ? undefined : String(value[0]);
  return String(value);
}

/** 取一个新的 Grant 序列号；每次请求（含重试）都应重新取号。 */
export function nextDataGrantSequence(): number {
  return nextSequence();
}

/**
 * 清空全部缓存，登出、切换租户或切换用户时必须调用。
 *
 * 序列号不归零：清空后在途的旧响应仍带着更大的 seq，归零会让它盖掉清空后新签发的 Grant。
 */
export function clearDataGrants(): void {
  entries.clear();
  warnedMissingDomain = false;
}

/** 删除单个数据域的缓存键（真删除，不做 stale 标记）。 */
export function deleteDataGrant(domain: string): void {
  const key = domain.trim();
  if (!key) return;
  entries.delete(key);
}

/**
 * 记录一次签发的 Grant。只有更大的 `seq` 才会覆盖已有值，避免乱序响应回退。
 *
 * @returns 是否写入了缓存。
 */
export function recordDataGrant(
  domain: string,
  grant: string,
  identity: DataGrantIdentity = {},
  seq: number = nextDataGrantSequence(),
): boolean {
  const key = domain.trim();
  if (!key || !grant) return false;
  const existing = entries.get(key);
  if (existing && existing.seq >= seq) {
    return false;
  }
  useSequence += 1;
  entries.set(key, {
    grant,
    seq,
    userId: normalizeIdentityValue(identity.userId),
    tenantId: normalizeIdentityValue(identity.tenantId),
    used: useSequence,
  });
  return true;
}

/**
 * 汇总当前身份可回传的 Grant。
 *
 * 身份不一致的缓存项在这里被丢弃并清键；超过个数或长度上限时按最近使用优先截断。
 *
 * @returns 多值头内容；没有可回传的 Grant 时返回 `undefined`。
 */
export function collectDataGrants(identity: DataGrantIdentity = {}): DataGrantHeaderValue | undefined {
  const userId = normalizeIdentityValue(identity.userId);
  const tenantId = normalizeIdentityValue(identity.tenantId);
  const candidates: Array<DataGrantEntry & { domain: string }> = [];
  for (const [domain, entry] of entries) {
    if (entry.userId !== userId || entry.tenantId !== tenantId) {
      entries.delete(domain);
      devWarn(`数据权限 Grant 与当前身份不一致，已清除该域的键, domain=${domain}`);
      continue;
    }
    candidates.push({ domain, ...entry });
  }
  if (candidates.length === 0) return undefined;
  candidates.sort((left, right) => right.used - left.used || right.seq - left.seq);

  const selected = candidates.slice(0, MAX_DATA_GRANT_COUNT);
  if (selected.length < candidates.length) {
    devWarn(
      `数据权限 Grant 超过 ${MAX_DATA_GRANT_COUNT} 个数据域，按最近使用优先截断，丢弃 ${candidates.length - selected.length} 个`,
    );
  }

  const kept: typeof selected = [];
  let value = '';
  for (const candidate of selected) {
    const next = value ? `${value}, ${candidate.grant}` : candidate.grant;
    if (next.length > MAX_DATA_GRANT_LENGTH) break;
    value = next;
    kept.push(candidate);
  }
  if (kept.length < selected.length) {
    devWarn(
      `数据权限 Grant 序列化长度超过 ${MAX_DATA_GRANT_LENGTH} 字符，按最近使用优先截断，丢弃 ${selected.length - kept.length} 个`,
    );
  }
  if (kept.length === 0) return undefined;

  useSequence += 1;
  for (const candidate of kept) {
    const entry = entries.get(candidate.domain);
    if (entry) entry.used = useSequence;
  }
  return { value, domains: kept.map((entry) => entry.domain) };
}

/**
 * 根据一次非下载响应更新缓存。
 *
 * 只有带 `X-Mango-Data-Grant-Domain` 的响应才会被采纳；Skipped 与 `absent` 一律删除该域的键。
 */
export function applyDataGrantResponse(input: DataGrantResponseInput): void {
  const domain = readHeader(input.headers, DATA_GRANT_DOMAIN_HEADER)?.trim();
  const grant = readHeader(input.headers, DATA_GRANT_HEADER);
  const skipped = readHeader(input.headers, DATA_GRANT_SKIPPED_HEADER);
  if (!domain) {
    if ((grant !== undefined || skipped !== undefined) && !warnedMissingDomain) {
      warnedMissingDomain = true;
      devWarn('响应带有数据权限 Grant 头但缺少 X-Mango-Data-Grant-Domain，已忽略（只提示一次）');
    }
    return;
  }
  const issued = grant !== undefined && grant.trim() !== '' && grant.trim().toLowerCase() !== DATA_GRANT_ABSENT;
  // 只在「本地本来有这个域的 Grant，但请求没真的带出去」时告警：这正是跨源白名单缺失 / preflight 缓存的症状。
  // 首次加载时本地为空、后端要求携带属于正常流程，不报警，否则这条 warn 会变成噪音。
  const required = readHeader(input.headers, DATA_GRANT_REQUIRED_HEADER) === '1';
  const sentThisDomain = input.sentDomains?.includes(domain) ?? false;
  if (required && !issued && skipped === undefined && entries.has(domain) && !sentThisDomain) {
    devWarn(`后端要求携带数据权限 Grant，但本次请求没有真正带出该域（检查 CORS allowed-headers / 代理）, domain=${domain}`);
  }
  if (skipped !== undefined && skipped.trim() !== '') {
    deleteDataGrant(domain);
    return;
  }
  if (grant === undefined) return;
  const value = grant.trim();
  if (value === '') return;
  if (value.toLowerCase() === DATA_GRANT_ABSENT) {
    deleteDataGrant(domain);
    return;
  }
  recordDataGrant(domain, value, input.identity, input.seq);
}
