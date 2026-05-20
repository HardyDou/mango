<template>
  <div class="component-doc-page">
    <div class="doc-layout">
      <div class="doc-content" :class="{ 'is-boxed': contentBox }">
        <header class="page-header">
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </header>

        <main class="examples">
          <slot />
        </main>
      </div>
      <aside class="article-toc" aria-label="页面导航">
        <div class="article-toc-title">页面导航</div>
        <button
          v-for="item in tocItems"
          :key="item.id"
          type="button"
          :class="{ active: activeToc === item.id }"
          @click="scrollToSection(item.id)"
        >
          {{ item.label }}
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{
  title: string;
  subtitle: string;
  contentBox?: boolean;
  tocItems: Array<{
    id: string;
    label: string;
  }>;
}>();

const activeToc = ref('');
let tocObserver: IntersectionObserver | undefined;

function scrollToSection(id: string) {
  activeToc.value = id;
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

onMounted(async () => {
  activeToc.value = props.tocItems[0]?.id ?? '';

  await nextTick();

  const sections = props.tocItems
    .map((item) => document.getElementById(item.id))
    .filter((element): element is HTMLElement => Boolean(element));

  if (sections.length === 0) {
    return;
  }

  tocObserver = new IntersectionObserver(
    (entries) => {
      const visibleEntry = entries
        .filter((entry) => entry.isIntersecting)
        .sort((current, next) => current.boundingClientRect.top - next.boundingClientRect.top)[0];

      if (visibleEntry?.target.id) {
        activeToc.value = visibleEntry.target.id;
      }
    },
    {
      rootMargin: '-96px 0px -55% 0px',
      threshold: [0, 0.2, 0.6],
    },
  );

  sections.forEach((section) => tocObserver?.observe(section));
});

onBeforeUnmount(() => {
  tocObserver?.disconnect();
});
</script>

<style lang="scss">
@use './demo-page.scss';
</style>
