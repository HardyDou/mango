<template>
  <div class="m-ad" :class="[`m-ad--${variant}`]" :style="styleVars">
    <slot v-if="displayItems.length" name="default" :items="displayItems">
      <article v-for="(item, index) in displayItems" :key="itemKey(item, index)" class="m-ad__item">
        <slot name="item" :item="item" :index="index">
          <component
            :is="item.jumpUrl ? 'a' : 'div'"
            class="m-ad__link"
            :href="item.jumpUrl || undefined"
            :target="openTarget(item)"
            :rel="item.jumpUrl ? 'noopener noreferrer' : undefined"
          >
            <img v-if="imageSrc(item)" class="m-ad__image" :src="imageSrc(item)" :alt="item.title || item.adName || item.deliveryName || '广告'" />
            <video v-else-if="videoSrc(item)" class="m-ad__video" :src="videoSrc(item)" :poster="coverSrc(item)" muted playsinline controls />
            <div v-else-if="item.materialType === 'RICH_TEXT' && item.richContent" class="m-ad__rich" v-html="sanitizedHtml(item.richContent)" />
            <div v-else-if="item.materialType === 'HTML' && renderHtml && item.htmlContent" class="m-ad__rich" v-html="sanitizedHtml(item.htmlContent)" />
            <div v-else class="m-ad__text">
              <strong v-if="item.title">{{ item.title }}</strong>
              <span>{{ item.textContent || item.adName || item.deliveryName || '广告内容' }}</span>
            </div>
          </component>
        </slot>
      </article>
    </slot>
    <slot v-else name="empty" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { fileApi, fileRuntimeUrl } from '@mango/file';

export type MAdItem = {
  id?: string | number;
  adCode?: string;
  adName?: string;
  deliveryName?: string;
  position?: string;
  positionType?: string;
  adType?: string;
  materialType?: string;
  title?: string;
  textContent?: string;
  richContent?: string;
  htmlContent?: string;
  materialFileId?: string;
  imageFileId?: string;
  imageFileIds?: string | string[];
  imageUrl?: string;
  imageUrls?: string | string[];
  videoFileId?: string;
  coverFileId?: string;
  videoUrl?: string;
  coverUrl?: string;
  jumpUrl?: string;
  openTarget?: string;
  sort?: number;
};

const props = withDefaults(defineProps<{
  items?: MAdItem[];
  item?: MAdItem | null;
  variant?: 'inline' | 'card' | 'hero' | 'strip';
  imageResolver?: (fileId: string) => string;
  videoResolver?: (fileId: string) => string;
  renderHtml?: boolean;
  rounded?: string;
}>(), {
  items: () => [],
  item: null,
  variant: 'inline',
  renderHtml: false,
  rounded: '6px',
});

const displayItems = computed(() => {
  const source = props.item ? [props.item] : props.items;
  return [...source].sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0));
});

const styleVars = computed(() => ({
  '--m-ad-radius': props.rounded,
}));

const runtimeUrls = ref<Record<string, string>>({});
const loadingIds = new Set<string>();
let disposed = false;

watch(displayItems, (items) => {
  collectFileIds(items).forEach(loadRuntimeUrl);
}, { immediate: true, deep: true });

onBeforeUnmount(() => {
  disposed = true;
});

function itemKey(item: MAdItem, index: number) {
  return item.id || `${item.adCode || item.position || 'ad'}-${index}`;
}

function openTarget(item: MAdItem) {
  return item.jumpUrl && item.openTarget === 'BLANK' ? '_blank' : undefined;
}

function imageSrc(item: MAdItem) {
  const direct = firstImageUrl(item);
  if (direct) return direct;
  const fileId = firstImageFileId(item);
  return fileId ? resolveImage(fileId) : '';
}

function videoSrc(item: MAdItem) {
  if (item.videoUrl) {
    return item.videoUrl;
  }
  const fileId = item.videoFileId || (item.materialType === 'VIDEO' ? item.materialFileId : '');
  return fileId ? resolveVideo(String(fileId)) : '';
}

function coverSrc(item: MAdItem) {
  if (item.coverUrl) {
    return item.coverUrl;
  }
  return item.coverFileId ? resolveImage(String(item.coverFileId)) : '';
}

function firstImageUrl(item: MAdItem) {
  if (item.imageUrl) {
    return String(item.imageUrl);
  }
  if (Array.isArray(item.imageUrls)) {
    return item.imageUrls[0] ? String(item.imageUrls[0]) : '';
  }
  return String(item.imageUrls || '').split(',').map(value => value.trim()).filter(Boolean)[0] || '';
}

function firstImageFileId(item: MAdItem) {
  if (item.imageFileId) {
    return String(item.imageFileId);
  }
  if (item.materialFileId && item.materialType !== 'VIDEO') {
    return String(item.materialFileId);
  }
  if (Array.isArray(item.imageFileIds)) {
    return item.imageFileIds[0] ? String(item.imageFileIds[0]) : '';
  }
  return String(item.imageFileIds || '').split(',').map(value => value.trim()).filter(Boolean)[0] || '';
}

function resolveImage(fileId: string) {
  if (props.imageResolver) {
    return props.imageResolver(fileId);
  }
  loadRuntimeUrl(fileId);
  return runtimeUrls.value[fileId] || '';
}

function resolveVideo(fileId: string) {
  if (props.videoResolver) {
    return props.videoResolver(fileId);
  }
  loadRuntimeUrl(fileId);
  return runtimeUrls.value[fileId] || '';
}

function collectFileIds(items: MAdItem[]) {
  const ids = new Set<string>();
  items.forEach((item) => {
    [
      item.materialFileId,
      item.imageFileId,
      item.videoFileId,
      item.coverFileId,
      ...splitFileIds(item.imageFileIds),
    ].forEach((id) => {
      const value = String(id || '').trim();
      if (/^[1-9]\d*$/.test(value)) {
        ids.add(value);
      }
    });
  });
  return ids;
}

function splitFileIds(value?: string | string[]) {
  if (Array.isArray(value)) {
    return value;
  }
  return String(value || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

function loadRuntimeUrl(fileId: string) {
  if (!/^[1-9]\d*$/.test(fileId) || runtimeUrls.value[fileId] !== undefined || loadingIds.has(fileId)) {
    return;
  }
  loadingIds.add(fileId);
  fileApi.preview(fileId)
    .then((preview) => {
      if (disposed) return;
      runtimeUrls.value = {
        ...runtimeUrls.value,
        [fileId]: fileRuntimeUrl(preview),
      };
    })
    .catch(() => {
      if (disposed) return;
      runtimeUrls.value = {
        ...runtimeUrls.value,
        [fileId]: '',
      };
    })
    .finally(() => {
      loadingIds.delete(fileId);
    });
}

function sanitizedHtml(html: string) {
  const template = document.createElement('template');
  template.innerHTML = html;
  template.content.querySelectorAll('script, style, iframe, object, embed, link, meta').forEach(element => element.remove());
  template.content.querySelectorAll('*').forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim();
      if (name.startsWith('on') || name === 'style') {
        element.removeAttribute(attribute.name);
        return;
      }
      if ((name === 'href' || name === 'src') && /^(javascript|data):/i.test(value)) {
        element.removeAttribute(attribute.name);
      }
    });
  });
  return template.innerHTML;
}
</script>
