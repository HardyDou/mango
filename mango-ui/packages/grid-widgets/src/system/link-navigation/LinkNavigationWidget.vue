<template>
  <section
    class="mango-grid-widget-link-navigation"
    data-surface="home.link-navigation"
  >
    <header class="mango-grid-widget-link-navigation__header">
      <div class="mango-grid-widget-link-navigation__heading">
        <strong>网址导航</strong>
        <span>搜索与常用入口</span>
      </div>
    </header>

    <form
      class="mango-grid-widget-link-navigation__search"
      data-action="link-navigation.search"
      @submit.prevent="handleSearchEnter"
    >
      <label class="mango-grid-widget-link-navigation__input">
        <el-icon><Search /></el-icon>
        <input
          v-model="keyword"
          :placeholder="placeholder"
          autocomplete="off"
          data-field="link-navigation.keyword"
        >
      </label>
      <div
        class="mango-grid-widget-link-navigation__engines"
        role="group"
        aria-label="搜索引擎"
      >
        <button
          v-for="engine in searchEngines"
          :key="engine.code"
          :class="{ 'is-active': activeEngine === engine.code }"
          type="button"
          :data-action="`link-navigation.search.${engine.code}`"
          @click="searchWith(engine.code)"
        >
          {{ engine.label }}
        </button>
      </div>
    </form>

    <nav
      v-if="effectiveGroups.length > 0"
      class="mango-grid-widget-link-navigation__tabs"
      aria-label="收藏分组"
    >
      <div class="mango-grid-widget-link-navigation__tabs-fixed">
        <button
          v-for="group in fixedGroups"
          :key="group.key"
          :class="{ 'is-active': activeGroupKey === group.key }"
          type="button"
          :data-state="activeGroupKey === group.key ? 'active' : undefined"
          :data-record-key="group.key"
          @click="activeGroupKey = group.key"
        >
          {{ group.title }}
        </button>
      </div>
      <div class="mango-grid-widget-link-navigation__tabs-scroll">
        <button
          v-for="group in movableGroups"
          :key="group.key"
          :class="{
            'is-active': activeGroupKey === group.key,
            'is-dragging': draggedGroupKey === group.key,
          }"
          type="button"
          :draggable="isDraggableGroup(group)"
          :data-state="activeGroupKey === group.key ? 'active' : undefined"
          :data-record-key="group.key"
          @click="activeGroupKey = group.key"
          @dragstart="handleGroupDragStart(group)"
          @dragover.prevent
          @drop="handleGroupDrop(group)"
          @dragend="handleGroupDragEnd"
        >
          {{ group.title }}
        </button>
      </div>
      <div
        v-if="canManageActiveGroup"
        class="mango-grid-widget-link-navigation__tab-actions"
      >
        <button
          type="button"
          data-action="link-navigation.category.edit"
          title="编辑分组名称"
          aria-label="编辑分组名称"
          @click="openCategoryDialog('edit')"
        >
          <el-icon><Edit /></el-icon>
        </button>
        <button
          type="button"
          data-action="link-navigation.category.delete"
          title="删除分组"
          aria-label="删除分组"
          @click="deleteActiveCategory"
        >
          <el-icon><Delete /></el-icon>
        </button>
      </div>
      <button
        class="mango-grid-widget-link-navigation__tab-add"
        type="button"
        data-action="link-navigation.category.create"
        title="新增分组"
        @click="openCategoryDialog('create')"
      >
        +
      </button>
    </nav>

    <div
      v-if="loading"
      class="mango-grid-widget-link-navigation__loading"
      data-state="loading"
    >
      加载收藏网址...
    </div>

    <div
      v-else
      class="mango-grid-widget-link-navigation__items"
      :data-state="visibleItems.length > 0 ? 'ready' : 'empty'"
    >
      <div
        v-for="item in visibleItems"
        :key="item.id"
        class="mango-grid-widget-link-navigation__item"
        :class="{ 'is-favorited': isItemFavorited(item) }"
        role="button"
        tabindex="0"
        :title="item.title"
        :data-record-key="item.id"
        @click="openItem(item)"
        @keydown.enter.prevent="openItem(item)"
        @keydown.space.prevent="openItem(item)"
      >
        <span class="mango-grid-widget-link-navigation__item-icon">
          <img
            v-if="resolveIconUrl(item)"
            :src="resolveIconUrl(item)"
            alt=""
          >
          <span v-else>{{ itemInitial(item) }}</span>
        </span>
        <span class="mango-grid-widget-link-navigation__item-text">
          <strong>{{ item.title }}</strong>
        </span>
        <button
          class="mango-grid-widget-link-navigation__favorite"
          :class="{
            'is-favorited': isItemFavorited(item),
          }"
          :title="itemActionLabel(item)"
          :aria-label="itemActionLabel(item)"
          type="button"
          :disabled="favoritePendingIds[item.id]"
          data-action="link-navigation.favorite"
          @click.stop="handleItemAction(item)"
        >
          <el-icon>
            <StarFilled v-if="isItemFavorited(item)" />
            <Star v-else />
          </el-icon>
        </button>
      </div>
      <button
        v-if="canCreateLinkInActiveGroup"
        class="mango-grid-widget-link-navigation__item mango-grid-widget-link-navigation__item-add"
        type="button"
        data-action="link-navigation.link.create"
        title="添加网址"
        aria-label="添加网址"
        @click="openLinkDialog"
      >
        <span class="mango-grid-widget-link-navigation__item-icon">+</span>
      </button>
    </div>

    <el-dialog
      v-model="linkDialogVisible"
      title="新增网址"
      width="520px"
      destroy-on-close
      append-to-body
    >
      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input
            v-model="linkForm.name"
            maxlength="128"
            show-word-limit
            data-field="link-navigation.link.name"
            placeholder="请输入标题"
          />
        </el-form-item>
        <el-form-item label="网址" required>
          <el-input
            v-model="linkForm.url"
            maxlength="1024"
            data-field="link-navigation.link.url"
            placeholder="请输入网址"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="savingLink"
          data-action="link-navigation.link.submit"
          @click="submitLink"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="categoryDialogVisible"
      :title="categoryDialogTitle"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <el-form label-width="80px">
        <el-form-item label="分组名称" required>
          <el-input
            v-model="categoryForm.name"
            maxlength="64"
            show-word-limit
            data-field="link-navigation.category.name"
            placeholder="请输入分组名称"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="savingCategory"
          :data-action="categoryDialogMode === 'create' ? 'link-navigation.category.submit' : 'link-navigation.category.update'"
          @click="submitCategory"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Edit, Search, Star, StarFilled } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type {
  LinkNavigationGroup,
  LinkNavigationItem,
  LinkNavigationSearchEngineCode,
  LinkNavigationWidgetProps,
} from '../../types';

defineOptions({
  name: 'MangoLinkNavigationWidget',
});

interface SearchEngine {
  code: LinkNavigationSearchEngineCode;
  label: string;
  urlTemplate: string;
}

interface ResolvedGroup {
  key: string;
  title: string;
  items: LinkNavigationItem[];
  categoryId?: string;
  aggregate?: boolean;
  fixed?: boolean;
  owned?: boolean;
  system?: boolean;
}

const UNGROUPED_GROUP_KEY = 'personal-ungrouped';
const UNGROUPED_GROUP_TITLE = '未分组';

const props = withDefaults(defineProps<LinkNavigationWidgetProps>(), {
  items: () => [],
  groups: () => [],
  maxGroups: 24,
  maxItemsPerGroup: 200,
  placeholder: '搜索或输入网址',
  defaultSearchEngine: 'baidu',
  loadItems: undefined,
  navigate: undefined,
});

const searchEngines: SearchEngine[] = [
  {
    code: 'baidu',
    label: '百度',
    urlTemplate: 'https://www.baidu.com/s?wd={query}',
  },
  {
    code: 'google',
    label: '谷歌',
    urlTemplate: 'https://www.google.com/search?q={query}',
  },
];

const keyword = ref('');
const activeEngine = ref<LinkNavigationSearchEngineCode>(props.defaultSearchEngine);
const activeGroupKey = ref('');
const remoteFavoriteItems = ref<LinkNavigationItem[]>([]);
const remoteNavigationItems = ref<LinkNavigationItem[]>([]);
const remoteGroups = ref<LinkNavigationGroup[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const linkDialogVisible = ref(false);
const categoryDialogVisible = ref(false);
const categoryDialogMode = ref<'create' | 'edit'>('create');
const savingLink = ref(false);
const savingCategory = ref(false);
const favoritePendingIds = reactive<Record<string, boolean>>({});
const groupOrder = ref<string[]>([]);
const draggedGroupKey = ref('');
const linkForm = reactive({
  name: '',
  url: '',
});
const categoryForm = reactive({
  name: '',
});

const sourceFavoriteItems = computed(() => props.items.length ? props.items : remoteFavoriteItems.value);
const sourceNavigationItems = computed(() => {
  if (props.items.length) {
    return props.items;
  }
  return remoteNavigationItems.value.length ? remoteNavigationItems.value : remoteFavoriteItems.value;
});
const sourceGroups = computed(() => props.groups.length ? props.groups : remoteGroups.value);
const effectiveGroups = computed(() => {
  const groups = resolveGroups(
    sourceGroups.value,
    sourceFavoriteItems.value,
    sourceNavigationItems.value,
    props.maxGroups,
    props.maxItemsPerGroup,
  );
  return reorderGroups(groups, groupOrder.value);
});
const activeGroup = computed(() => {
  return effectiveGroups.value.find(group => group.key === activeGroupKey.value);
});
const fixedGroups = computed(() => effectiveGroups.value.filter(group => group.aggregate || group.fixed));
const movableGroups = computed(() => effectiveGroups.value.filter(group => !group.aggregate && !group.fixed));
const visibleItems = computed(() => activeGroup.value?.items || []);
const activeIsFavoritesGroup = computed(() => activeGroup.value?.aggregate === true);
const canCreateLinkInActiveGroup = computed(() => activeGroup.value?.owned === true);
const canManageActiveGroup = computed(() => {
  return activeGroup.value?.owned === true
    && activeGroup.value.system !== true
    && Boolean(activeGroup.value.categoryId);
});
const categoryDialogTitle = computed(() => categoryDialogMode.value === 'create' ? '新增分组' : '编辑分组');

watch(
  () => props.defaultSearchEngine,
  (engine) => {
    if (isSearchEngineCode(engine)) {
      activeEngine.value = engine;
    }
  },
);

watch(
  effectiveGroups,
  (groups) => {
    if (!groups.some(group => group.key === activeGroupKey.value)) {
      activeGroupKey.value = groups[0]?.key || '';
    }
  },
  { immediate: true },
);

onMounted(() => {
  void loadFavorites();
});

function handleSearchEnter(): void {
  searchWith(activeEngine.value);
}

function searchWith(engineCode: LinkNavigationSearchEngineCode): void {
  activeEngine.value = engineCode;
  const value = keyword.value.trim();
  if (!value) {
    return;
  }
  openExternalUrl(resolveDirectUrl(value) || resolveSearchUrl(engineCode, value));
}

async function openItem(item: LinkNavigationItem): Promise<void> {
  if (props.navigate) {
    await props.navigate(item);
    return;
  }
  if (item.redirectUrl) {
    openExternalUrl(item.redirectUrl);
    return;
  }
  if (item.id && (item.source === 'FAVORITE' || !item.path)) {
    openExternalUrl(`/api/link/open/redirect/${encodeURIComponent(item.id)}?source=FAVORITE`);
    return;
  }
  if (item.pageType === 'EXTERNAL_LINK' && item.url) {
    openExternalUrl(item.url);
    return;
  }
  if (item.path) {
    await props.runtime?.navigate?.({
      path: item.path,
      url: item.url,
      pageType: item.pageType,
      raw: item.raw || item,
    });
    return;
  }
  if (item.url) {
    openExternalUrl(item.url);
  }
}

function resolveSearchUrl(engineCode: LinkNavigationSearchEngineCode, value: string): string {
  const engine = searchEngines.find(item => item.code === engineCode) || searchEngines[0];
  return engine.urlTemplate.replace('{query}', encodeURIComponent(value));
}

function resolveDirectUrl(value: string): string | undefined {
  if (/^https?:\/\//i.test(value)) {
    return value;
  }
  if (/^[\w.-]+\.[a-z]{2,}([/:?#].*)?$/i.test(value)) {
    return `https://${value}`;
  }
  return undefined;
}

function openExternalUrl(url: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.open(url, '_blank', 'noopener,noreferrer');
}

async function loadFavorites(): Promise<void> {
  if (props.items.length) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    if (props.loadItems || props.groups.length) {
      const favoriteItems = props.loadItems ? await props.loadItems(undefined, props.runtime) : [];
      remoteFavoriteItems.value = dedupeItems(favoriteItems.map(item => ({ ...item, favorited: true })));
      remoteNavigationItems.value = mergeNavigationItems([], remoteFavoriteItems.value);
      remoteGroups.value = props.groups;
      return;
    }
    const data = await loadDefaultWidgetData();
    remoteFavoriteItems.value = dedupeItems(data.favoriteItems.map(item => ({ ...item, favorited: true })));
    remoteNavigationItems.value = mergeNavigationItems([...data.companyItems, ...data.personalItems], remoteFavoriteItems.value);
    remoteGroups.value = data.categories;
  } catch {
    errorMessage.value = '收藏网址加载失败';
    remoteGroups.value = props.groups.length || props.loadItems ? props.groups : remoteGroups.value;
    if (!remoteFavoriteItems.value.length) {
      remoteFavoriteItems.value = [];
    }
    if (!remoteNavigationItems.value.length && !remoteFavoriteItems.value.length) {
      remoteNavigationItems.value = [];
    }
  } finally {
    loading.value = false;
  }
}

function openLinkDialog(): void {
  linkForm.name = '';
  linkForm.url = '';
  linkDialogVisible.value = true;
}

function openCategoryDialog(mode: 'create' | 'edit' = 'create'): void {
  categoryDialogMode.value = mode;
  categoryForm.name = mode === 'edit' ? activeGroup.value?.title || '' : '';
  categoryDialogVisible.value = true;
}

async function submitLink(): Promise<void> {
  const name = linkForm.name.trim();
  const url = linkForm.url.trim();
  if (!name) {
    ElMessage.warning('请输入标题');
    return;
  }
  if (!url) {
    ElMessage.warning('请输入网址');
    return;
  }
  savingLink.value = true;
  try {
    const categoryId = resolveCreateCategoryId();
    if (activeGroup.value?.owned !== true) {
      ElMessage.warning('请选择具体分组');
      return;
    }
    await createPersonalLink({
      name,
      url,
      categoryId,
    });
    ElMessage.success('已新增网址');
    linkDialogVisible.value = false;
    await loadFavorites();
  } catch (error) {
    ElMessage.error(errorMessageOf(error, '新增网址失败'));
  } finally {
    savingLink.value = false;
  }
}

function resolveCreateCategoryId(): string | undefined {
  if (activeGroup.value?.key === UNGROUPED_GROUP_KEY) {
    return undefined;
  }
  return activeGroup.value?.owned ? activeGroup.value.categoryId || activeGroup.value.key : undefined;
}

async function submitCategory(): Promise<void> {
  const name = categoryForm.name.trim();
  if (!name) {
    ElMessage.warning('请输入分组名称');
    return;
  }
  savingCategory.value = true;
  try {
    if (categoryDialogMode.value === 'edit') {
      const categoryId = activeGroup.value?.categoryId;
      if (!categoryId) {
        ElMessage.warning('请选择可编辑分组');
        return;
      }
      await updatePersonalCategory(categoryId, name);
      ElMessage.success('已更新分组');
      categoryDialogVisible.value = false;
      await loadFavorites();
      activeGroupKey.value = categoryId;
    } else {
      const categoryId = await createPersonalCategory(name);
      ElMessage.success('已新增分组');
      categoryDialogVisible.value = false;
      await loadFavorites();
      if (categoryId) {
        activeGroupKey.value = categoryId;
        groupOrder.value = reorderKeysWithNewKey(groupOrder.value, categoryId);
      }
    }
  } catch (error) {
    ElMessage.error(errorMessageOf(error, categoryDialogMode.value === 'create' ? '新增分组失败' : '更新分组失败'));
  } finally {
    savingCategory.value = false;
  }
}

async function deleteActiveCategory(): Promise<void> {
  const categoryId = activeGroup.value?.categoryId;
  if (!categoryId || !canManageActiveGroup.value) {
    return;
  }
  try {
    await ElMessageBox.confirm('确认删除当前分组？分组下存在网址时不能删除。', '删除分组', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await deletePersonalCategory(categoryId);
    ElMessage.success('已删除分组');
    activeGroupKey.value = 'enterprise';
    await loadFavorites();
  } catch (error) {
    if (isCancelError(error)) {
      return;
    }
    ElMessage.error(errorMessageOf(error, '删除分组失败'));
  }
}

async function loadDefaultWidgetData(): Promise<LinkNavigationWidgetData> {
  if (typeof fetch === 'undefined') {
    return {
      companyItems: [],
      personalItems: [],
      favoriteItems: [],
      categories: [],
    };
  }
  const result = await requestJson<LinkNavigationWidgetApiResult>('/api/link/navigation-widget/data', {
    method: 'GET',
  });
  return {
    companyItems: (result.companyItems || []).map(toCompanyNavigationItem),
    personalItems: (result.personalItems || []).map(toPersonalNavigationItem),
    favoriteItems: (result.favoriteItems || []).map(toFavoriteNavigationItem),
    categories: (result.categories || []).map(toCategoryGroup).filter(group => group.key),
  };
}

async function createPersonalCategory(name: string): Promise<string> {
  return toString(await requestJson<string | number>('/api/link/personal-categories/create', {
    method: 'POST',
    body: JSON.stringify({ name }),
  }));
}

async function updatePersonalCategory(id: string, name: string): Promise<void> {
  await requestJson<boolean>('/api/link/personal-categories/update', {
    method: 'PUT',
    body: JSON.stringify({ id, name }),
  });
}

async function deletePersonalCategory(id: string): Promise<void> {
  await requestJson<boolean>(`/api/link/personal-categories/delete?id=${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

async function createPersonalLink(input: { name: string; url: string; categoryId?: string }): Promise<string> {
  const payload: Record<string, unknown> = {
    name: input.name,
    url: input.url,
  };
  if (input.categoryId) {
    payload.categoryId = input.categoryId;
  }
  return toString(await requestJson<string | number>('/api/link/personal-links/create', {
    method: 'POST',
    body: JSON.stringify(payload),
  }));
}

async function createFavorite(linkId: string): Promise<void> {
  await requestJson<boolean>('/api/link/favorites/create', {
    method: 'POST',
    body: JSON.stringify({ linkId }),
  });
}

async function deleteFavorite(linkId: string): Promise<void> {
  await requestJson<boolean>('/api/link/favorites/delete', {
    method: 'DELETE',
    body: JSON.stringify({ linkId }),
  });
}

async function handleItemAction(item: LinkNavigationItem): Promise<void> {
  if (isDeleteFavoriteAction(item)) {
    await removeFavoriteItem(item);
    return;
  }
  if (isItemFavorited(item)) {
    await removeFavoriteItem(item);
    return;
  }
  if (!isItemFavorited(item)) {
    await favoriteItem(item);
  }
}

async function favoriteItem(item: LinkNavigationItem): Promise<void> {
  if (!item.id || favoritePendingIds[item.id]) {
    return;
  }
  favoritePendingIds[item.id] = true;
  try {
    updateItemFavoriteState(item.id, true);
    await createFavorite(item.id);
    ElMessage.success('已收藏');
    await loadFavorites();
  } catch (error) {
    updateItemFavoriteState(item.id, false);
    ElMessage.error(errorMessageOf(error, '收藏失败'));
  } finally {
    favoritePendingIds[item.id] = false;
  }
}

async function removeFavoriteItem(item: LinkNavigationItem): Promise<void> {
  if (!item.id || favoritePendingIds[item.id]) {
    return;
  }
  favoritePendingIds[item.id] = true;
  try {
    updateItemFavoriteState(item.id, false);
    await deleteFavorite(item.id);
    ElMessage.success('已移出我的收藏');
    await loadFavorites();
  } catch (error) {
    updateItemFavoriteState(item.id, true);
    ElMessage.error(errorMessageOf(error, '移出收藏失败'));
  } finally {
    favoritePendingIds[item.id] = false;
  }
}

function updateItemFavoriteState(id: string, favorited: boolean): void {
  remoteNavigationItems.value = remoteNavigationItems.value.map(item => item.id === id ? { ...item, favorited } : item);
  if (favorited) {
    const item = remoteNavigationItems.value.find(candidate => candidate.id === id);
    if (item && !remoteFavoriteItems.value.some(candidate => candidate.id === id)) {
      remoteFavoriteItems.value = [{ ...item, favorited: true, source: 'FAVORITE' }, ...remoteFavoriteItems.value];
    }
  } else {
    remoteFavoriteItems.value = remoteFavoriteItems.value.filter(item => item.id !== id);
  }
}

async function requestJson<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, {
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers || {}),
    },
    ...init,
  });
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }
  const result = await response.json() as { success?: boolean; code?: number | string; msg?: string; message?: string; data?: T };
  if (!(result.success === true || result.code === 0 || result.code === 200 || result.code === '0' || result.code === '200')) {
    throw new Error(result.message || result.msg || '请求失败');
  }
  return (result.data ?? ([] as T)) as T;
}

function resolveGroups(
  configuredGroups: LinkNavigationGroup[],
  favoriteItems: LinkNavigationItem[],
  navigationItems: LinkNavigationItem[],
  maxGroups: number,
  maxItemsPerGroup: number,
): ResolvedGroup[] {
  const favoritesGroup: ResolvedGroup = {
    key: 'favorites',
    title: '我的收藏',
    items: favoriteItems.slice(0, maxItemsPerGroup),
    aggregate: true,
    fixed: true,
  };
  const enterpriseGroup: ResolvedGroup = {
    key: 'enterprise',
    title: '企业导航',
    items: [],
    fixed: true,
    owned: false,
  };
  const ungroupedGroup: ResolvedGroup = {
    key: UNGROUPED_GROUP_KEY,
    title: UNGROUPED_GROUP_TITLE,
    items: [],
    owned: true,
    system: true,
  };
  const groupMap = new Map<string, ResolvedGroup>();
  const groupItemsSource = mergeNavigationItems(navigationItems, favoriteItems);
  configuredGroups.forEach((group) => {
    const groupItems = group.items?.length
      ? group.items
      : groupItemsSource.filter(item => item.groupKey === group.key);
    groupMap.set(group.key, {
      key: group.key,
      title: group.title,
      categoryId: group.categoryId || group.key,
      items: groupItems.slice(0, maxItemsPerGroup),
      fixed: isEnterpriseGroupTitle(group.title),
      owned: group.owned !== false,
    });
  });
  groupItemsSource.forEach((item) => {
    const title = item.groupTitle || item.categoryName || (item.source === 'PERSONAL' ? UNGROUPED_GROUP_TITLE : '我的收藏');
    const key = item.groupKey || (item.source === 'PERSONAL' ? UNGROUPED_GROUP_KEY : title);
    const group = groupMap.get(key) || {
      key,
      title,
      items: [],
      fixed: isEnterpriseGroupTitle(title),
      owned: key === UNGROUPED_GROUP_KEY,
      system: key === UNGROUPED_GROUP_KEY,
    };
    group.fixed = group.fixed || isEnterpriseGroupTitle(title);
    const exists = group.items.some(groupItem => groupItem.id === item.id);
    if (!exists && group.items.length < maxItemsPerGroup) {
      group.items.push(item);
    }
    groupMap.set(key, group);
  });

  const groups = Array.from(groupMap.values()).filter(group => group.key !== favoritesGroup.key);
  const enterpriseGroups = groups.filter(group => group.fixed);
  const mergedEnterpriseGroup = mergeEnterpriseGroups(enterpriseGroup, enterpriseGroups, maxItemsPerGroup);
  const otherGroups = groups.filter(group => !group.fixed);
  const personalUngroupedGroup = {
    ...ungroupedGroup,
    items: dedupeItems([
      ...ungroupedGroup.items,
      ...groupItemsSource.filter(item => item.source === 'PERSONAL' && item.groupKey === UNGROUPED_GROUP_KEY),
      ...(groupMap.get(UNGROUPED_GROUP_KEY)?.items || []),
    ]).slice(0, maxItemsPerGroup),
  };
  return [favoritesGroup, mergedEnterpriseGroup, personalUngroupedGroup, ...otherGroups.filter(group => group.key !== UNGROUPED_GROUP_KEY)]
    .slice(0, maxGroups);
}

interface LinkFavoriteApiItem {
  id?: string | number;
  name?: string;
  url?: string;
  redirectUrl?: string;
  iconUrl?: string;
  categoryId?: string | number;
  categoryName?: string;
  summary?: string;
  tags?: string[];
  favoriteTime?: string;
  source?: string;
  favorited?: boolean;
}

interface LinkCompanyApiItem {
  id?: string | number;
  name?: string;
  url?: string;
  redirectUrl?: string;
  iconUrl?: string;
  categoryId?: string | number;
  categoryName?: string;
  summary?: string;
  tags?: string[];
  source?: string;
  favorited?: boolean;
}

interface LinkPersonalApiItem {
  id?: string | number;
  name?: string;
  url?: string;
  redirectUrl?: string;
  iconUrl?: string;
  categoryId?: string | number;
  categoryName?: string;
  summary?: string;
  tags?: string[];
  source?: string;
  favorited?: boolean;
}

interface LinkNavigationWidgetApiResult {
  companyItems?: LinkCompanyApiItem[];
  personalItems?: LinkPersonalApiItem[];
  favoriteItems?: LinkFavoriteApiItem[];
  categories?: LinkCategoryApiItem[];
}

interface LinkNavigationWidgetData {
  companyItems: LinkNavigationItem[];
  personalItems: LinkNavigationItem[];
  favoriteItems: LinkNavigationItem[];
  categories: LinkNavigationGroup[];
}

interface LinkCategoryApiItem {
  id?: string | number;
  name?: string;
}

function toCategoryGroup(item: LinkCategoryApiItem): LinkNavigationGroup {
  const categoryId = toString(item.id);
  return {
    key: categoryId || item.name || '',
    title: item.name || '未命名分组',
    categoryId,
    owned: true,
  };
}

function toFavoriteNavigationItem(item: LinkFavoriteApiItem): LinkNavigationItem {
  const id = toString(item.id) || item.url || item.name || '';
  const source = item.source || 'FAVORITE';
  const categoryName = item.categoryName || (source === 'PERSONAL' ? UNGROUPED_GROUP_TITLE : '我的收藏');
  return {
    id,
    title: item.name || item.url || '未命名网址',
    url: item.url,
    redirectUrl: item.redirectUrl,
    iconUrl: item.iconUrl,
    groupKey: toString(item.categoryId) || (source === 'PERSONAL' ? UNGROUPED_GROUP_KEY : categoryName),
    groupTitle: categoryName,
    categoryName,
    summary: item.summary,
    tags: item.tags,
    favoriteTime: item.favoriteTime,
    source,
    favorited: true,
    raw: item,
  };
}

function toCompanyNavigationItem(item: LinkCompanyApiItem): LinkNavigationItem {
  const id = toString(item.id) || item.url || item.name || '';
  const categoryName = item.categoryName || '企业导航';
  return {
    id,
    title: item.name || item.url || '未命名网址',
    url: item.url,
    redirectUrl: item.redirectUrl,
    iconUrl: item.iconUrl,
    groupKey: toString(item.categoryId) || categoryName,
    groupTitle: categoryName,
    categoryName,
    summary: item.summary,
    tags: item.tags,
    source: item.source || 'COMPANY',
    favorited: item.favorited === true,
    raw: item,
  };
}

function toPersonalNavigationItem(item: LinkPersonalApiItem): LinkNavigationItem {
  const id = toString(item.id) || item.url || item.name || '';
  const categoryId = toString(item.categoryId);
  const categoryName = item.categoryName || UNGROUPED_GROUP_TITLE;
  return {
    id,
    title: item.name || item.url || '未命名网址',
    url: item.url,
    redirectUrl: item.redirectUrl,
    iconUrl: item.iconUrl,
    groupKey: categoryId || UNGROUPED_GROUP_KEY,
    groupTitle: categoryName,
    categoryName,
    summary: item.summary,
    tags: item.tags,
    source: item.source || 'PERSONAL',
    favorited: item.favorited === true,
    raw: item,
  };
}

function resolveIconUrl(item: LinkNavigationItem): string {
  if (item.iconUrl) {
    return item.iconUrl;
  }
  const domain = resolveDomain(item.url);
  return domain ? `https://www.google.com/s2/favicons?domain=${encodeURIComponent(domain)}&sz=64` : '';
}

function resolveDomain(url?: string): string {
  if (!url) {
    return '';
  }
  try {
    return new URL(/^https?:\/\//i.test(url) ? url : `https://${url}`).hostname;
  } catch {
    return '';
  }
}

function itemInitial(item: LinkNavigationItem): string {
  return (item.title || item.url || '?').trim().slice(0, 1).toUpperCase();
}

function dedupeItems(items: LinkNavigationItem[]): LinkNavigationItem[] {
  const itemMap = new Map<string, LinkNavigationItem>();
  items.forEach((item) => {
    if (!itemMap.has(item.id)) {
      itemMap.set(item.id, item);
    }
  });
  return Array.from(itemMap.values());
}

function mergeNavigationItems(items: LinkNavigationItem[], favoriteItems: LinkNavigationItem[]): LinkNavigationItem[] {
  const favoriteIds = new Set(favoriteItems.map(item => item.id));
  const itemMap = new Map<string, LinkNavigationItem>();
  items.forEach((item) => {
    itemMap.set(item.id, {
      ...item,
      favorited: item.favorited === true || favoriteIds.has(item.id),
    });
  });
  favoriteItems.forEach((item) => {
    const existing = itemMap.get(item.id);
    itemMap.set(item.id, {
      ...item,
      ...(existing || {}),
      source: existing?.source || item.source,
      favorited: true,
    });
  });
  return Array.from(itemMap.values());
}

function isItemFavorited(item: LinkNavigationItem): boolean {
  return item.favorited === true || item.source === 'FAVORITE';
}

function isDeleteFavoriteAction(item: LinkNavigationItem): boolean {
  return activeIsFavoritesGroup.value && isItemFavorited(item);
}

function itemActionLabel(item: LinkNavigationItem): string {
  if (isDeleteFavoriteAction(item)) {
    return '取消收藏';
  }
  return isItemFavorited(item) ? '取消收藏' : '收藏';
}

function reorderGroups(groups: ResolvedGroup[], order: string[]): ResolvedGroup[] {
  const fixedGroups = groups.filter(group => group.aggregate || group.fixed);
  const movable = groups.filter(group => !group.aggregate && !group.fixed);
  const orderIndex = new Map(order.map((key, index) => [key, index]));
  const sorted = movable
    .map((group, index) => ({ group, index }))
    .sort((left, right) => {
      const leftOrder = orderIndex.has(left.group.key) ? orderIndex.get(left.group.key) as number : Number.MAX_SAFE_INTEGER;
      const rightOrder = orderIndex.has(right.group.key) ? orderIndex.get(right.group.key) as number : Number.MAX_SAFE_INTEGER;
      return leftOrder === rightOrder ? left.index - right.index : leftOrder - rightOrder;
    })
    .map(item => item.group);
  return [...fixedGroups, ...sorted];
}

function handleGroupDragStart(group: ResolvedGroup): void {
  if (!isDraggableGroup(group)) {
    return;
  }
  draggedGroupKey.value = group.key;
}

function handleGroupDrop(target: ResolvedGroup): void {
  if (!draggedGroupKey.value || !isDraggableGroup(target) || draggedGroupKey.value === target.key) {
    return;
  }
  const keys = effectiveGroups.value.filter(isDraggableGroup).map(group => group.key);
  const fromIndex = keys.indexOf(draggedGroupKey.value);
  const toIndex = keys.indexOf(target.key);
  if (fromIndex < 0 || toIndex < 0) {
    return;
  }
  const nextKeys = [...keys];
  const [moved] = nextKeys.splice(fromIndex, 1);
  nextKeys.splice(toIndex, 0, moved);
  groupOrder.value = nextKeys;
}

function handleGroupDragEnd(): void {
  draggedGroupKey.value = '';
}

function reorderKeysWithNewKey(keys: string[], key: string): string[] {
  return keys.includes(key) ? keys : [...keys, key];
}

function isDraggableGroup(group: ResolvedGroup): boolean {
  return !group.aggregate && !group.fixed;
}

function isEnterpriseGroupTitle(title: string): boolean {
  return title === '企业导航';
}

function isCancelError(error: unknown): boolean {
  return error === 'cancel' || error === 'close';
}

function mergeEnterpriseGroups(
  fallback: ResolvedGroup,
  groups: ResolvedGroup[],
  maxItemsPerGroup: number,
): ResolvedGroup {
  if (!groups.length) {
    return fallback;
  }
  const [first] = groups;
  return {
    ...first,
    key: fallback.key,
    title: fallback.title,
    categoryId: first.categoryId,
    fixed: true,
    owned: false,
    items: dedupeItems(groups.flatMap(group => group.items)).slice(0, maxItemsPerGroup),
  };
}

function isSearchEngineCode(value: unknown): value is LinkNavigationSearchEngineCode {
  return value === 'baidu' || value === 'google';
}

function errorMessageOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

function toString(value: unknown): string {
  return typeof value === 'string' || typeof value === 'number' ? String(value) : '';
}
</script>
