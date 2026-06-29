import { normalizeTagsViewRoutes } from '@mango/common/utils/tagsView';

const TAGS_VIEW_STORAGE_KEY = 'mango-tags-view-routes';

function readPersistedTagsView() {
  try {
    return JSON.parse(localStorage.getItem(TAGS_VIEW_STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

/**
 * 清空当前浏览器中的标签导航缓存，只保留固定首页标签。
 *
 * TagsView 属于 admin-shell 的导航状态，登录页由 @mango/auth 提供。
 * 因此这里在 shell 层统一处理缓存，避免 auth 包反向依赖 shell store。
 */
export function resetPersistedTagsView() {
  if (typeof localStorage === 'undefined') {
    return;
  }
  const persisted = readPersistedTagsView();
  localStorage.setItem(
    TAGS_VIEW_STORAGE_KEY,
    JSON.stringify({
      ...persisted,
      tagsViewRoutes: normalizeTagsViewRoutes([]),
      isTagsViewCurrenFull: false,
    })
  );
}
