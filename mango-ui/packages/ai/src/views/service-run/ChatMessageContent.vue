<template>
  <!-- eslint-disable-next-line vue/no-v-html -- markdown-it escapes raw HTML and rejects unsafe links. -->
  <div class="mango-ai-message-content" v-html="renderedContent" />
</template>

<script setup lang="ts">
import MarkdownIt from 'markdown-it';
import { computed } from 'vue';

defineOptions({ name: 'AiChatMessageContent' });

const props = defineProps<{ content: string }>();

const markdown = new MarkdownIt({
  breaks: true,
  html: false,
  linkify: true,
  typographer: false,
});
const defaultLinkOpen = markdown.renderer.rules.link_open;
markdown.renderer.rules.link_open = (tokens, index, options, environment, self) => {
  tokens[index].attrSet('target', '_blank');
  tokens[index].attrSet('rel', 'noopener noreferrer');
  return defaultLinkOpen
    ? defaultLinkOpen(tokens, index, options, environment, self)
    : self.renderToken(tokens, index, options);
};

const renderedContent = computed(() => markdown.render(props.content || ''));
</script>
