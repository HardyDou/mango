<template>
  <pre class="demo-code"><code>{{ code }}</code></pre>
</template>

<script setup lang="ts">
defineProps<{
  code: string;
}>();
</script>
