<template>
  <section class="mango-link-page" data-page="link.home" data-component="mango-link-page">
    <header class="mango-link-page__hero" data-surface="link.hero">
      <h1 class="mango-link-page__headline">{{ displayHeadline }}</h1>
      <p v-if="subtitle" class="mango-link-page__subtitle">{{ subtitle }}</p>

      <form class="mango-link-page__searchbar" data-surface="link.search" @submit.prevent="submitSearch">
        <el-input
          v-model="keyword"
          class="mango-link-page__search"
          clearable
          :placeholder="searchPlaceholder"
          :prefix-icon="Search"
          data-field="link.keyword"
          @clear="clearSearch"
        />
        <el-button
          class="mango-link-page__search-button"
          type="primary"
          native-type="submit"
          :icon="Search"
          :loading="searching"
          data-action="link.search"
        >
          搜索
        </el-button>
      </form>
    </header>

    <main v-loading="loading" class="mango-link-page__body" data-surface="link.content">
      <section v-if="errorMessage" class="mango-link-page__state" data-state="error">
        <strong>导航加载失败</strong>
        <span>{{ errorMessage }}</span>
        <el-button type="primary" :icon="RefreshRight" @click="reload">重试</el-button>
      </section>

      <template v-else>
        <section v-if="searchQuery" class="mango-link-page__results" data-surface="link.search-results">
          <div class="mango-link-page__section-head">
            <div>
              <h2>搜索结果</h2>
              <p>关键词：{{ searchQuery }}</p>
            </div>
            <button class="mango-link-page__text-button" type="button" data-action="link.clear-search" @click="clearSearch">
              清空搜索
            </button>
          </div>

          <div v-if="searchResults.length > 0" class="mango-link-page__compact-grid">
            <a
              v-for="item in searchResults"
              :key="`search:${itemKey(item)}`"
              class="mango-link-page__compact-card"
              :href="linkTarget(item) || undefined"
              target="_blank"
              rel="noopener noreferrer"
              :title="linkTarget(item) || item.url || item.name"
              :data-record-key="itemKey(item)"
              data-action="link.open"
              @click="handleLinkClick(item, $event)"
            >
              <span class="mango-link-page__compact-logo" :class="{ 'has-initial': !displayIcon(item) }">
                <img
                  v-if="displayIcon(item)"
                  :src="displayIcon(item)"
                  :alt="`${item.name || '导航'} logo`"
                  loading="lazy"
                  @error="markIconFailed(item)"
                />
                <span v-else>{{ itemInitial(item) }}</span>
              </span>
              <span class="mango-link-page__compact-main">
                <strong>{{ item.name || '-' }}</strong>
                <span>{{ displayUrl(item.url) || '-' }}</span>
              </span>
            </a>
          </div>

          <div v-else class="mango-link-page__state is-inline" data-state="empty">
            <strong>没有找到相关导航</strong>
            <span>可以换一个关键词再试试。</span>
          </div>
        </section>

        <section v-if="hasAnyGroupItem" class="mango-link-page__groups" data-surface="link.groups">
          <section
            v-for="group in groups"
            :key="group.name"
            class="mango-link-page__group"
            :data-group="group.name"
          >
            <div class="mango-link-page__section-head">
              <div>
                <h2>{{ group.name }}</h2>
                <p>{{ group.items.length }} 个入口</p>
              </div>
            </div>

            <div v-if="group.items.length > 0" class="mango-link-page__card-grid">
              <a
                v-for="item in group.items"
                :key="itemKey(item)"
                class="mango-link-page__card"
                :href="linkTarget(item) || undefined"
                target="_blank"
                rel="noopener noreferrer"
                :title="linkTarget(item) || item.url || item.name"
                :data-record-key="itemKey(item)"
                data-action="link.open"
                @click="handleLinkClick(item, $event)"
              >
                <span class="mango-link-page__logo" :class="{ 'has-initial': !displayIcon(item) }">
                  <img
                    v-if="displayIcon(item)"
                    :src="displayIcon(item)"
                    :alt="`${item.name || '导航'} logo`"
                    loading="lazy"
                    @error="markIconFailed(item)"
                  />
                  <span v-else>{{ itemInitial(item) }}</span>
                </span>
                <span class="mango-link-page__card-main">
                  <strong>{{ item.name || '-' }}</strong>
                  <span v-if="item.recommended || visibleTags(item).length > 0" class="mango-link-page__meta">
                    <span v-if="item.recommended" class="mango-link-page__tag is-recommended">推荐</span>
                    <span v-for="tag in visibleTags(item)" :key="`${itemKey(item)}:${tag}`" class="mango-link-page__tag">
                      {{ tag }}
                    </span>
                  </span>
                  <span class="mango-link-page__url" :title="item.url">{{ displayUrl(item.url) || '-' }}</span>
                  <span v-if="item.summary" class="mango-link-page__summary">{{ item.summary }}</span>
                </span>
              </a>
            </div>

            <div v-else class="mango-link-page__state is-inline" data-state="empty">
              <strong>暂无{{ group.name }}入口</strong>
              <span>数据库中还没有启用的导航卡片。</span>
            </div>
          </section>
        </section>

        <section v-else class="mango-link-page__state" data-state="empty">
          <strong>暂无导航</strong>
          <span>当前没有可展示的内置导航卡片。</span>
        </section>
      </template>
    </main>
  </section>
</template>

<script setup lang="ts">
import { RefreshRight, Search } from '@element-plus/icons-vue';
import {
  isLinkOpenApiNotFoundError,
  listPublicLinks,
  type LinkOpenApiClientOptions,
  type LinkPublicItem,
} from '@mango/link-openapi';
import { computed, onMounted, reactive, ref } from 'vue';
import type { LinkPageProps } from '../types';

type LinkGroup = {
  name: string;
  items: LinkPublicItem[];
};

const categoryNames = ['业务相关', '工具相关', '其他'] as const;
const categoryNameSet = new Set<string>(categoryNames);

const props = withDefaults(defineProps<LinkPageProps>(), {
  credentials: 'same-origin',
  title: '保函业务导航',
  headline: '保函业务快捷入口',
  searchPlaceholder: '搜索网站、工具或关键词',
  jumpEnabled: undefined,
});

const emit = defineEmits<{
  opened: [item: LinkPublicItem];
}>();

const loading = ref(false);
const searching = ref(false);
const errorMessage = ref('');
const keyword = ref('');
const searchQuery = ref('');
const links = ref<LinkPublicItem[]>([]);
const searchItems = ref<LinkPublicItem[]>([]);
const failedIcons = reactive<Record<string, boolean>>({});

const displayHeadline = computed(() => props.headline || props.title || '保函业务快捷入口');
const subtitle = computed(() => props.subtitle || '');
const searchPlaceholder = computed(() => props.searchPlaceholder || '搜索网站、工具或关键词');
const requestOptions = computed<LinkOpenApiClientOptions>(() => ({
  baseUrl: props.baseUrl,
  headers: props.headers,
  credentials: props.credentials,
}));
const visibleLinks = computed(() => normalizeItems(links.value));
const searchResults = computed(() => normalizeItems(searchItems.value));
const groups = computed<LinkGroup[]>(() => categoryNames.map(name => ({
  name,
  items: visibleLinks.value
    .filter(item => item.categoryName === name)
    .sort(itemComparator),
})));
const hasAnyGroupItem = computed(() => groups.value.some(group => group.items.length > 0));

onMounted(() => {
  void loadLinks();
});

async function loadLinks() {
  loading.value = true;
  errorMessage.value = '';
  try {
    links.value = await listPublicLinks({}, requestOptions.value);
  } catch (error) {
    links.value = [];
    errorMessage.value = readableError(error, '导航加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

async function reload() {
  await loadLinks();
  if (searchQuery.value) {
    await searchByKeyword(searchQuery.value);
  }
}

async function submitSearch() {
  const term = keyword.value.trim();
  if (!term) {
    clearSearch();
    return;
  }
  await searchByKeyword(term);
}

async function searchByKeyword(term: string) {
  searching.value = true;
  errorMessage.value = '';
  searchQuery.value = term;
  try {
    searchItems.value = await listPublicLinks({ keyword: term }, requestOptions.value);
  } catch (error) {
    searchItems.value = [];
    errorMessage.value = readableError(error, '搜索失败，请稍后重试');
  } finally {
    searching.value = false;
  }
}

function clearSearch() {
  keyword.value = '';
  searchQuery.value = '';
  searchItems.value = [];
}

function normalizeItems(items: LinkPublicItem[]) {
  const result = new Map<string, LinkPublicItem>();
  for (const item of items) {
    if (!item.categoryName || !categoryNameSet.has(item.categoryName)) {
      continue;
    }
    const key = itemKey(item);
    if (!result.has(key)) {
      result.set(key, item);
    }
  }
  return Array.from(result.values()).sort(itemComparator);
}

function itemComparator(left: LinkPublicItem, right: LinkPublicItem) {
  const leftCategoryIndex = categoryNames.indexOf(left.categoryName as typeof categoryNames[number]);
  const rightCategoryIndex = categoryNames.indexOf(right.categoryName as typeof categoryNames[number]);
  if (leftCategoryIndex !== rightCategoryIndex) {
    return leftCategoryIndex - rightCategoryIndex;
  }
  return (left.sortNo ?? 0) - (right.sortNo ?? 0);
}

function itemKey(item: LinkPublicItem) {
  return String(item.id || `${item.categoryName || 'none'}:${item.name || ''}:${item.url || ''}`);
}

function handleLinkClick(item: LinkPublicItem, event: MouseEvent) {
  const target = linkTarget(item);
  if (!target) {
    event.preventDefault();
    errorMessage.value = '当前导航地址无效';
    return;
  }
  emit('opened', item);
}

function linkTarget(item: LinkPublicItem) {
  const target = systemRedirectUrl(item) || item.url;
  if (!target) {
    return '';
  }
  return resolveTargetUrl(target);
}

function systemRedirectUrl(item: LinkPublicItem) {
  if (props.jumpEnabled === false) {
    return '';
  }
  if (item.redirectUrl) {
    return item.redirectUrl;
  }
  if (props.jumpEnabled === undefined || !item.url) {
    return '';
  }
  return `/link/open/jump?url=${encodeURIComponent(item.url)}&source=${encodeURIComponent(item.source || 'PUBLIC')}`;
}

function resolveTargetUrl(target: string) {
  if (/^https?:\/\//i.test(target)) {
    return target;
  }
  return target;
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
  return item.iconUrl || item.url || item.name || itemKey(item);
}

function itemInitial(item: LinkPublicItem) {
  const value = (item.name || item.url || '?').trim();
  const latin = value.match(/[A-Za-z0-9]+/);
  if (latin?.[0] && latin.index === 0) {
    return latin[0].slice(0, 2).toUpperCase();
  }
  return value.slice(0, 1).toUpperCase();
}

function visibleTags(item: LinkPublicItem) {
  return (item.tags || []).filter(Boolean).slice(0, 4);
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
  if (!value || value.startsWith('/')) {
    return '';
  }
  try {
    const url = new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`);
    return `${url.origin}/favicon.ico`;
  } catch {
    return '';
  }
}

function displayUrl(value?: string) {
  if (!value) {
    return '';
  }
  return value.replace(/^https?:\/\//i, '');
}

function readableError(error: unknown, fallback: string) {
  if (isLinkOpenApiNotFoundError(error)) {
    return '网址导航服务未开通或接口不可用';
  }
  return error instanceof Error && error.message ? error.message : fallback;
}
</script>
