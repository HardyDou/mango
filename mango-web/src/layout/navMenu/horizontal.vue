<template>
  <el-menu
    mode="horizontal"
    router
    :default-active="state.defaultActive"
    class="nav-menu-horizontal"
  >
    <template v-for="val in menuList" :key="val.path">
      <el-menu-item :index="val.path" v-if="!val.children || val.children.length === 0">
        <span>{{ val.meta?.title || val.name }}</span>
      </el-menu-item>
      <el-sub-menu :index="val.path" v-else>
        <template #title>
          <span>{{ val.meta?.title || val.name }}</span>
        </template>
        <el-menu-item
          v-for="child in val.children"
          :key="child.path"
          :index="child.path"
        >
          {{ child.meta?.title || child.name }}
        </el-menu-item>
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<script setup lang="ts" name="navMenuHorizontal">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

defineProps<{
  menuList: any[];
}>();

const route = useRoute();
const state = reactive({
  defaultActive: route.path,
});

onMounted(() => {
  state.defaultActive = route.path;
});
</script>

<style scoped lang="scss">
.nav-menu-horizontal {
  border-bottom: 1px solid var(--mango-border-color);
}
</style>
