<template>
  <section class="mango-link-panel" data-component="mango-link-panel">
    <header class="mango-link-panel__header">
      <h2 class="mango-link-panel__title">{{ title }}</h2>
      <div class="mango-link-panel__brand">
        <img v-if="logoUrl" :src="logoUrl" :alt="logoAlt || logoText || title" />
        <span v-else>{{ logoText || title }}</span>
      </div>
      <div class="mango-link-panel__searchbar">
        <el-input
          v-model="keyword"
          class="mango-link-panel__search"
          clearable
          placeholder="搜索导航..."
          :prefix-icon="Search"
          @keyup.enter="submitSearch"
        />
        <div class="mango-link-panel__engines" aria-label="搜索引擎">
          <button
            v-for="engine in availableSearchEngines"
            :key="engine.code"
            class="mango-link-panel__engine"
            :data-engine="engine.code"
            type="button"
            @click="openSearch(engine.code)"
          >
            {{ engine.label }}
          </button>
        </div>
      </div>
      <div class="mango-link-panel__tools">
        <el-popover v-if="loggedIn" placement="bottom-end" trigger="click" width="280" popper-class="mango-link-panel__profile-popper">
          <template #reference>
            <button class="mango-link-panel__user" type="button" :title="displayUserName">
              <img v-if="displayUserAvatarUrl" :src="displayUserAvatarUrl" :alt="`${displayUserName}头像`" />
              <span v-else>{{ userInitial }}</span>
            </button>
          </template>
          <div class="mango-link-panel__profile">
            <div class="mango-link-panel__profile-head">
              <span class="mango-link-panel__profile-avatar">
                <img v-if="displayUserAvatarUrl" :src="displayUserAvatarUrl" :alt="`${displayUserName}头像`" />
                <span v-else>{{ userInitial }}</span>
              </span>
              <div class="mango-link-panel__profile-main">
                <strong>{{ displayUserName }}</strong>
                <span v-if="userAccount">{{ userAccount }}</span>
              </div>
            </div>
            <dl class="mango-link-panel__profile-fields">
              <div v-for="field in profileFields" :key="field.label">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
            <button class="mango-link-panel__logout" type="button" :disabled="saving" @click="submitLogout">退出登录</button>
          </div>
        </el-popover>
        <button v-else class="mango-link-panel__login" type="button" @click="openLoginDialog">登录</button>
      </div>
    </header>

    <nav v-if="groups.length > 0" class="mango-link-panel__tabs" aria-label="网址分组">
      <button
        v-for="group in groups"
        :key="group.key"
        class="mango-link-panel__tab"
        :class="{ 'is-active': group.key === activeGroupKey }"
        type="button"
        @click="activeGroupKey = group.key"
      >
        <span>{{ group.title }}</span>
      </button>
    </nav>

    <el-alert v-if="errorMessage" class="mango-link-panel__error" type="error" :closable="false" show-icon>
      <template #title>{{ errorMessage }}</template>
    </el-alert>

    <div v-loading="loading" class="mango-link-panel__body">
      <el-empty v-if="!loading && (!activeGroup || activeGroup.items.length === 0)" description="暂无网址" />
      <section v-if="activeGroup" :key="activeGroup.key" class="mango-link-panel__group">
        <div class="mango-link-panel__grid">
          <article v-for="item in activeGroup.items" :key="`${item.source}:${item.id}`" class="mango-link-panel__item">
            <button class="mango-link-panel__open" type="button" @click="openLink(item)">
              <span class="mango-link-panel__icon" :class="{ 'has-initial': !displayIcon(item) }">
                <img
                  v-if="displayIcon(item)"
                  :src="displayIcon(item)"
                  :alt="`${item.name || '网址'} logo`"
                  loading="lazy"
                  @error="markIconFailed(item)"
                />
                <span v-else>{{ itemInitial(item) }}</span>
              </span>
              <span class="mango-link-panel__item-main" :title="item.summary || item.url || item.name">
                {{ item.name || '-' }}
              </span>
            </button>
            <div class="mango-link-panel__item-actions" :class="{ 'is-favorited': item.favorited }">
              <el-button
                v-if="item.source !== 'PERSONAL'"
                circle
                link
                type="primary"
                class="mango-link-panel__favorite"
                :icon="item.favorited ? StarFilled : Star"
                :title="item.favorited ? '取消收藏' : '收藏'"
                :loading="isFavoritePending(item)"
                :disabled="isFavoritePending(item)"
                @click.stop="toggleFavorite(item)"
              />
              <span v-if="item.recommended" class="mango-link-panel__badge">荐</span>
            </div>
          </article>
        </div>
      </section>
    </div>

    <el-dialog v-model="categoryDialogVisible" title="新增分组" width="420px" append-to-body>
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="72px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="categoryForm.name" maxlength="64" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="linkDialogVisible" title="添加网址" width="640px" append-to-body>
      <el-form ref="linkFormRef" :model="linkForm" :rules="linkRules" label-width="84px">
        <el-form-item label="分组">
          <el-select v-model="linkForm.categoryId" clearable placeholder="未分组">
            <el-option v-for="category in personalCategories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="linkForm.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="网址" prop="url">
          <el-input v-model="linkForm.url" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="linkForm.summary" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="linkForm.tags" multiple filterable allow-create default-first-option collapse-tags placeholder="输入后回车">
            <el-option v-for="tag in linkForm.tags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLink">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="loginDialogVisible" title="登录" width="420px" append-to-body>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="64px"
        @keyup.enter="submitLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="loginForm.username" autocomplete="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" autocomplete="current-password" placeholder="请输入密码" show-password type="password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="loginDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitLogin">登录</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { Search, Star, StarFilled } from '@element-plus/icons-vue';
import {
  createFavorite,
  createPersonalCategory,
  createPersonalLink,
  deleteFavorite,
  listPersonalCategories,
  listPublicLinks,
  type CreateLinkPersonalItemInput,
  type LinkCategory,
  type LinkOpenApiClientOptions,
  type LinkPublicItem,
} from '@mango/link-openapi';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { LinkPanelLoginInput, LinkPanelProps, LinkPanelSearchEngine } from '../types';

type LinkGroup = {
  key: string;
  title: string;
  items: LinkPublicItem[];
};

type MangoAuthResult = {
  accessToken?: string;
  token?: string;
  refreshToken?: string;
  expiresIn?: number | string;
  userInfo?: Record<string, unknown>;
  tenantId?: string | number;
  tenantCode?: string;
  tenantName?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: string | number;
  appCode?: string;
};

type MangoResult<T> = {
  code?: number | string;
  success?: boolean;
  msg?: string;
  message?: string;
  data?: T;
};

const localFavoritesKey = 'mango-link-panel:favorites';
const mangoTokenKey = 'MANGO_TOKEN';
const mangoRefreshTokenKey = 'MANGO_REFRESH_TOKEN';
const mangoTokenExpiresAtKey = 'MANGO_TOKEN_EXPIRES_AT';
const defaultSearchEngines: LinkPanelSearchEngine[] = [
  { code: 'baidu', label: '百度', searchUrl: 'https://www.baidu.com/s?wd={keyword}' },
  { code: 'google', label: 'Google', searchUrl: 'https://www.google.com/search?q={keyword}' },
];

const props = withDefaults(defineProps<LinkPanelProps>(), {
  credentials: 'same-origin',
  title: '网址导航',
  logoText: 'Mango',
  logoAlt: '公司 Logo',
  authenticated: false,
  defaultSearchEngine: 'baidu',
});
const emit = defineEmits<{
  login: [input: LinkPanelLoginInput];
  logout: [];
  opened: [item: LinkPublicItem];
  created: [];
}>();

const loading = ref(false);
const saving = ref(false);
const detectedLogin = ref(false);
const errorMessage = ref('');
const keyword = ref('');
const activeSearchEngine = ref(props.defaultSearchEngine);
const mangoAuthToken = ref(readMangoToken());
const mangoUserInfo = ref<Record<string, unknown>>(readStoredMangoUserInfo());
const links = ref<LinkPublicItem[]>([]);
const localFavorites = ref<LinkPublicItem[]>([]);
const personalCategories = ref<LinkCategory[]>([]);
const failedIcons = reactive<Record<string, boolean>>({});
const favoritePendingIds = reactive<Record<string, boolean>>({});
const activeGroupKey = ref('');
const categoryDialogVisible = ref(false);
const linkDialogVisible = ref(false);
const loginDialogVisible = ref(false);
const categoryFormRef = ref<FormInstance>();
const linkFormRef = ref<FormInstance>();
const loginFormRef = ref<FormInstance>();
const categoryForm = reactive({ name: '' });
const linkForm = reactive<CreateLinkPersonalItemInput>({ name: '', url: '', categoryId: '', summary: '', tags: [] });
const loginForm = reactive<LinkPanelLoginInput>({ username: '', password: '' });

const categoryRules: FormRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
};
const linkRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入网址', trigger: 'blur' }],
};
const loginRules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};
const loggedIn = computed(() => props.authenticated || detectedLogin.value || Boolean(mangoAuthToken.value));
const requestOptions = computed<LinkOpenApiClientOptions>(() => ({
  baseUrl: props.baseUrl,
  headers: resolveRequestHeaders,
  credentials: props.credentials,
}));
const availableSearchEngines = computed(() => props.searchEngines?.length ? props.searchEngines : defaultSearchEngines);
const displayUserName = computed(() => props.userName
  || props.userAccount
  || mangoUserText(['nickname', 'name', 'realName', 'username', 'account'])
  || '当前用户');
const displayUserAvatarUrl = computed(() => props.userAvatarUrl || mangoUserText(['avatarUrl', 'avatar', 'photo']));
const userInitial = computed(() => displayUserName.value.trim().slice(0, 1) || '我');
const profileFields = computed(() => [
  { label: '账号', value: props.userAccount || mangoUserText(['username', 'account']) },
  { label: '邮箱', value: props.userEmail || mangoUserText(['email']) },
  { label: '手机', value: props.userPhone || mangoUserText(['phone', 'mobile']) },
  { label: '部门', value: props.userDepartment || mangoUserText(['departmentName', 'deptName', 'orgName']) },
  { label: '角色', value: props.userRole || mangoUserText(['roleName']) },
].filter((field): field is { label: string; value: string } => Boolean(field.value)));
const localFavoriteIds = computed(() => new Set(localFavorites.value.map(item => String(item.id || '')).filter(Boolean)));
const visibleLinks = computed(() => links.value.map(item => ({
  ...item,
  favorited: item.favorited || (!loggedIn.value && Boolean(item.id) && localFavoriteIds.value.has(String(item.id))),
})));
const mergedLocalFavorites = computed(() => localFavorites.value.map((favorite) => {
  const latest = visibleLinks.value.find(item => item.id && favorite.id && String(item.id) === String(favorite.id));
  return {
    ...favorite,
    ...latest,
    source: 'FAVORITE' as const,
    favorited: true,
  };
}));
const favoriteLinks = computed(() => {
  if (!loggedIn.value) {
    return mergedLocalFavorites.value;
  }
  return visibleLinks.value.filter(item => item.source === 'FAVORITE');
});
const companyLinks = computed(() => visibleLinks.value.filter((item) => {
  const source = item.source || 'PUBLIC';
  return source === 'COMPANY' || source === 'PUBLIC';
}));
const personalLinks = computed(() => visibleLinks.value.filter(item => item.source === 'PERSONAL'));
const personalGroups = computed(() => {
  const result = new Map<string, LinkGroup>();
  for (const item of personalLinks.value) {
    const key = `PERSONAL:${item.categoryId || 'none'}`;
    if (!result.has(key)) {
      result.set(key, {
        key,
        title: item.categoryName || '我的网址',
        items: [],
      });
    }
    result.get(key)?.items.push(item);
  }
  return Array.from(result.values());
});
const groups = computed<LinkGroup[]>(() => [
  { key: 'FAVORITE', title: '我的收藏', items: favoriteLinks.value },
  { key: 'COMPANY', title: '公司网址', items: companyLinks.value },
  ...personalGroups.value,
]);
const activeGroup = computed(() => groups.value.find(group => group.key === activeGroupKey.value) || groups.value[0]);

watch(groups, (next) => {
  const current = next.find(group => group.key === activeGroupKey.value);
  if (current && current.items.length > 0) {
    return;
  }
  activeGroupKey.value = next.find(group => group.items.length > 0)?.key || next[0]?.key || '';
}, { immediate: true });

watch(availableSearchEngines, (next) => {
  if (!next.some(engine => engine.code === activeSearchEngine.value)) {
    activeSearchEngine.value = next[0]?.code || 'baidu';
  }
}, { immediate: true });

async function loadLinks() {
  loading.value = true;
  errorMessage.value = '';
  try {
    links.value = await listPublicLinks({}, requestOptions.value);
    detectedLogin.value = links.value.some((item) => item.source && item.source !== 'PUBLIC');
    if (loggedIn.value) {
      await loadPersonalCategories();
    }
  } catch (error) {
    errorMessage.value = errorMessageOf(error, '网址加载失败');
  } finally {
    loading.value = false;
  }
}

function submitSearch() {
  openSearch(activeSearchEngine.value);
}

function openSearch(engineCode: string) {
  const term = keyword.value.trim();
  const engine = availableSearchEngines.value.find(item => item.code === engineCode)
    || availableSearchEngines.value[0];
  if (!engine) {
    return;
  }
  const target = !term
    ? searchHomeUrl(engine.searchUrl)
    : engine.searchUrl.includes('{keyword}')
    ? engine.searchUrl.replace('{keyword}', encodeURIComponent(term))
    : `${engine.searchUrl}${encodeURIComponent(term)}`;
  window.open(target, '_blank', 'noopener,noreferrer');
}

function searchHomeUrl(searchUrl: string) {
  try {
    return new URL(searchUrl).origin;
  } catch {
    return searchUrl.replace(/[?#].*$/, '').replace(/\{keyword\}/g, '');
  }
}

async function loadPersonalCategories() {
  try {
    personalCategories.value = await listPersonalCategories(requestOptions.value);
  } catch (error) {
    if (!isUnauthorized(error)) {
      throw error;
    }
  }
}

function openCategoryDialog() {
  categoryForm.name = '';
  categoryDialogVisible.value = true;
}

function openLinkDialog() {
  Object.assign(linkForm, { name: '', url: '', categoryId: '', summary: '', tags: [] });
  linkDialogVisible.value = true;
}

function openLoginDialog() {
  loginDialogVisible.value = true;
}

async function submitLogin() {
  await loginFormRef.value?.validate();
  saving.value = true;
  try {
    const input = { ...props.loginDefaults, ...loginForm };
    if (props.loginHandler) {
      await props.loginHandler(input);
    } else {
      await loginByMangoApi(input);
    }
    emit('login', input);
    loginDialogVisible.value = false;
    await loadLinks();
  } catch (error) {
    ElMessage.error(errorMessageOf(error, '登录失败'));
  } finally {
    saving.value = false;
  }
}

async function submitLogout() {
  saving.value = true;
  try {
    if (props.logoutHandler) {
      await props.logoutHandler();
    } else {
      await logoutByMangoApi();
    }
    emit('logout');
    await loadLinks();
  } catch (error) {
    ElMessage.error(errorMessageOf(error, '退出登录失败'));
  } finally {
    saving.value = false;
  }
}

async function loginByMangoApi(input: LinkPanelLoginInput) {
  const result = await mangoRequestJson<MangoAuthResult>('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
  const token = result.accessToken || result.token;
  if (!token) {
    throw new Error('登录响应无效');
  }
  mangoAuthToken.value = token;
  persistMangoToken(token, result);
  const fallbackUserInfo = normalizeLoginUserInfo(result, input);
  mangoUserInfo.value = fallbackUserInfo;
  persistMangoUserInfo(fallbackUserInfo);
  try {
    const userInfo = await mangoRequestJson<Record<string, unknown>>('/auth/info', { method: 'GET' });
    mangoUserInfo.value = { ...fallbackUserInfo, ...userInfo };
    persistMangoUserInfo(mangoUserInfo.value);
  } catch {
    mangoUserInfo.value = fallbackUserInfo;
  }
}

async function logoutByMangoApi() {
  try {
    await mangoRequestJson<unknown>('/auth/logout', { method: 'POST' });
  } finally {
    clearMangoAuth();
    detectedLogin.value = false;
    personalCategories.value = [];
  }
}

async function resolveRequestHeaders() {
  const result = new Headers(await resolveProvidedHeaders());
  if (mangoAuthToken.value && !result.has('Authorization')) {
    result.set('Authorization', `Bearer ${mangoAuthToken.value}`);
  }
  const tenantId = mangoUserText(['tenantId']) || readStorageValue('tenantId');
  if (tenantId && !result.has('TENANT-ID')) {
    result.set('TENANT-ID', tenantId);
  }
  return result;
}

async function resolveProvidedHeaders() {
  if (typeof props.headers === 'function') {
    return props.headers();
  }
  return props.headers;
}

async function mangoRequestJson<T>(path: string, init: RequestInit = {}) {
  const response = await fetch(`${trimTrailingSlash(props.baseUrl || globalThis.location?.origin || '')}${path}`, {
    ...init,
    credentials: props.credentials,
    headers: await mergeMangoHeaders(init.headers),
  });
  if (!response.ok) {
    throw new Error(`Mango 接口请求失败：${response.status}`);
  }
  const result = await response.json() as MangoResult<T> | T;
  if (isMangoResult(result)) {
    if (!isSuccessMangoResult(result)) {
      throw new Error(result.message || result.msg || 'Mango 接口返回失败');
    }
    return result.data as T;
  }
  return result as T;
}

async function mergeMangoHeaders(headers?: HeadersInit) {
  const result = new Headers(await resolveRequestHeaders());
  if (headers) {
    new Headers(headers).forEach((value, key) => result.set(key, value));
  }
  return result;
}

function isMangoResult<T>(result: MangoResult<T> | T): result is MangoResult<T> {
  return Boolean(result && typeof result === 'object'
    && ('code' in result || 'success' in result || 'data' in result || 'msg' in result || 'message' in result));
}

function isSuccessMangoResult(result: MangoResult<unknown>) {
  return result.success === true
    || result.code === 0
    || result.code === 200
    || result.code === '0'
    || result.code === '200';
}

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '');
}

function normalizeLoginUserInfo(result: MangoAuthResult, fallback: LinkPanelLoginInput) {
  const userInfo = result.userInfo || {};
  return {
    ...userInfo,
    username: userInfo.username || fallback.username,
    tenantId: userInfo.tenantId ?? result.tenantId ?? fallback.tenantId,
    tenantCode: userInfo.tenantCode ?? result.tenantCode ?? fallback.tenantCode,
    tenantName: userInfo.tenantName ?? result.tenantName,
    realm: userInfo.realm ?? result.realm ?? fallback.realm,
    actorType: userInfo.actorType ?? result.actorType ?? fallback.actorType,
    partyType: userInfo.partyType ?? result.partyType ?? fallback.partyType,
    partyId: userInfo.partyId ?? result.partyId ?? fallback.partyId,
    appCode: userInfo.appCode ?? result.appCode ?? fallback.appCode,
  };
}

function persistMangoToken(token: string, result: MangoAuthResult) {
  if (typeof window === 'undefined') {
    return;
  }
  window.sessionStorage.setItem(mangoTokenKey, token);
  document.cookie = `${mangoTokenKey}=${encodeURIComponent(token)}; path=/; SameSite=Lax`;
  if (result.refreshToken) {
    window.sessionStorage.setItem(mangoRefreshTokenKey, result.refreshToken);
  }
  const expiresIn = Number(result.expiresIn);
  if (Number.isFinite(expiresIn) && expiresIn > 0) {
    window.sessionStorage.setItem(mangoTokenExpiresAtKey, String(Date.now() + expiresIn * 1000));
  }
}

function persistMangoUserInfo(userInfo: Record<string, unknown>) {
  if (typeof window === 'undefined') {
    return;
  }
  window.sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
  const tenantId = userInfo.tenantId;
  if (tenantId !== undefined && tenantId !== null && tenantId !== '') {
    window.sessionStorage.setItem('tenantId', String(tenantId));
  }
}

function clearMangoAuth() {
  mangoAuthToken.value = '';
  mangoUserInfo.value = {};
  if (typeof window === 'undefined') {
    return;
  }
  window.sessionStorage.removeItem(mangoTokenKey);
  window.sessionStorage.removeItem(mangoRefreshTokenKey);
  window.sessionStorage.removeItem(mangoTokenExpiresAtKey);
  window.sessionStorage.removeItem('userInfo');
  window.sessionStorage.removeItem('tenantId');
  document.cookie = `${mangoTokenKey}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
}

function readMangoToken() {
  if (typeof window === 'undefined') {
    return '';
  }
  return window.sessionStorage.getItem(mangoTokenKey) || readCookie(mangoTokenKey) || '';
}

function readStoredMangoUserInfo() {
  if (typeof window === 'undefined') {
    return {};
  }
  try {
    const value = window.sessionStorage.getItem('userInfo');
    const parsed = value ? JSON.parse(value) : {};
    return parsed && typeof parsed === 'object' ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
}

function readStorageValue(key: string) {
  if (typeof window === 'undefined') {
    return '';
  }
  return window.sessionStorage.getItem(key) || '';
}

function readCookie(name: string) {
  if (typeof document === 'undefined' || !document.cookie) {
    return '';
  }
  const prefix = `${name}=`;
  const cookie = document.cookie.split(';').map(item => item.trim()).find(item => item.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : '';
}

function mangoUserText(keys: string[]) {
  for (const key of keys) {
    const value = mangoUserInfo.value[key];
    if (value !== undefined && value !== null && value !== '') {
      return String(value);
    }
  }
  return '';
}

async function saveCategory() {
  await categoryFormRef.value?.validate();
  saving.value = true;
  try {
    await createPersonalCategory({ name: categoryForm.name }, requestOptions.value);
    ElMessage.success('分组已新增');
    categoryDialogVisible.value = false;
    emit('created');
    await loadPersonalCategories();
  } catch (error) {
    handleActionError(error, '新增分组失败');
  } finally {
    saving.value = false;
  }
}

async function saveLink() {
  await linkFormRef.value?.validate();
  saving.value = true;
  try {
    await createPersonalLink({
      ...linkForm,
      categoryId: linkForm.categoryId || undefined,
      tags: linkForm.tags || [],
    }, requestOptions.value);
    ElMessage.success('网址已添加');
    linkDialogVisible.value = false;
    emit('created');
    await loadLinks();
  } catch (error) {
    handleActionError(error, '添加网址失败');
  } finally {
    saving.value = false;
  }
}

async function toggleFavorite(item: LinkPublicItem) {
  if (!item.id) {
    return;
  }
  const id = String(item.id);
  if (favoritePendingIds[id]) {
    return;
  }
  if (!loggedIn.value) {
    toggleLocalFavorite(item);
    return;
  }
  favoritePendingIds[id] = true;
  try {
    if (item.favorited || item.source === 'FAVORITE') {
      await deleteFavorite(item.id, requestOptions.value);
      ElMessage.success('已取消收藏');
      updateFavoriteState(item.id, false);
    } else {
      await createFavorite(item.id, requestOptions.value);
      ElMessage.success('已收藏');
      updateFavoriteState(item.id, true);
    }
    await loadLinks();
  } catch (error) {
    handleActionError(error, '收藏操作失败');
  } finally {
    favoritePendingIds[id] = false;
  }
}

function toggleLocalFavorite(item: LinkPublicItem) {
  const id = String(item.id || '');
  if (!id) {
    return;
  }
  if (localFavoriteIds.value.has(id)) {
    localFavorites.value = localFavorites.value.filter(favorite => String(favorite.id || '') !== id);
    saveLocalFavorites();
    ElMessage.success('已取消收藏');
    return;
  }
  localFavorites.value = [
    {
      id: item.id,
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      name: item.name,
      url: item.url,
      summary: item.summary,
      iconUrl: item.iconUrl,
      tags: item.tags,
      openMode: item.openMode,
      recommended: item.recommended,
      sortNo: item.sortNo,
      source: 'FAVORITE',
      redirectUrl: item.redirectUrl,
      favorited: true,
    },
    ...localFavorites.value.filter(favorite => String(favorite.id || '') !== id),
  ];
  saveLocalFavorites();
  ElMessage.success('已收藏到本地');
}

function loadLocalFavorites() {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    const value = window.localStorage.getItem(localFavoritesKey);
    const parsed = value ? JSON.parse(value) : [];
    localFavorites.value = Array.isArray(parsed) ? parsed : [];
  } catch {
    localFavorites.value = [];
  }
}

function saveLocalFavorites() {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(localFavoritesKey, JSON.stringify(localFavorites.value));
}

function isFavoritePending(item: LinkPublicItem) {
  return Boolean(item.id && favoritePendingIds[String(item.id)]);
}

function updateFavoriteState(linkId: string, favorited: boolean) {
  links.value = links.value.map(item => String(item.id || '') === String(linkId)
    ? { ...item, favorited }
    : item);
}

function openLink(item: LinkPublicItem) {
  const target = systemRedirectUrl(item) || item.url;
  if (!target) {
    return;
  }
  window.open(resolveTargetUrl(target), '_blank', 'noopener,noreferrer');
  emit('opened', item);
}

function systemRedirectUrl(item: LinkPublicItem) {
  if (item.redirectUrl) {
    return item.redirectUrl;
  }
  if (!item.url) {
    return '';
  }
  return `/link/open/jump?url=${encodeURIComponent(item.url)}&source=${encodeURIComponent(item.source || 'PUBLIC')}`;
}

function resolveTargetUrl(target: string) {
  if (/^https?:\/\//i.test(target)) {
    return target;
  }
  const base = props.baseUrl || '';
  if (base.startsWith('http')) {
    return `${base.replace(/\/+$/, '')}${target}`;
  }
  return `${base.replace(/\/+$/, '')}${target}`;
}

function requestLogin() {
  openLoginDialog();
}

function handleActionError(error: unknown, fallback: string) {
  if (isUnauthorized(error)) {
    requestLogin();
    return;
  }
  ElMessage.error(errorMessageOf(error, fallback));
}

function errorMessageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function isUnauthorized(error: unknown) {
  return error instanceof Error && error.message.includes('401');
}

function displayIcon(item: LinkPublicItem) {
  const key = iconKey(item);
  if (failedIcons[key]) {
    return '';
  }
  return normalizeIconUrl(item.iconUrl) || faviconUrl(item.url);
}

function markIconFailed(item: LinkPublicItem) {
  failedIcons[iconKey(item)] = true;
}

function iconKey(item: LinkPublicItem) {
  return item.iconUrl || item.url || item.name || String(item.id || '');
}

function itemInitial(item: LinkPublicItem) {
  const value = (item.name || item.url || '?').trim();
  return value.slice(0, 1).toUpperCase();
}

function normalizeIconUrl(value?: string) {
  if (!value) {
    return '';
  }
  if (/^https?:\/\//i.test(value) || value.startsWith('data:')) {
    return value;
  }
  return resolveTargetUrl(value);
}

function faviconUrl(value?: string) {
  if (!value) {
    return '';
  }
  try {
    const url = new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`);
    return `${url.origin}/favicon.ico`;
  } catch {
    return '';
  }
}

onMounted(() => {
  loadLocalFavorites();
  void loadLinks();
});
</script>
