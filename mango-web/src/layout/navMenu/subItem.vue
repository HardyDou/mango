<template>
  <template
    v-for="val in chil"
    :key="val.path"
  >
    <el-menu-item
      v-if="!val.children || val.children.length === 0"
      :index="val.path"
    >
      <el-icon
        v-if="val.meta?.icon"
        class="menu-icon"
      >
        <HomeFilled v-if="val.meta.icon === 'HomeFilled'" />
        <User v-else-if="val.meta.icon === 'User'" />
        <Lock v-else-if="val.meta.icon === 'Lock'" />
        <Search v-else-if="val.meta.icon === 'Search'" />
        <Setting v-else-if="val.meta.icon === 'Setting'" />
        <Fold v-else-if="val.meta.icon === 'Fold'" />
        <Expand v-else-if="val.meta.icon === 'Expand'" />
        <Close v-else-if="val.meta.icon === 'Close'" />
        <Box v-else-if="val.meta.icon === 'Box'" />
        <Document v-else-if="val.meta.icon === 'Document'" />
        <Edit v-else-if="val.meta.icon === 'Edit'" />
        <Upload v-else-if="val.meta.icon === 'Upload'" />
        <DataLine v-else-if="val.meta.icon === 'DataLine'" />
        <Key v-else-if="val.meta.icon === 'Key'" />
        <Grid v-else-if="val.meta.icon === 'Grid'" />
        <Coin v-else-if="val.meta.icon === 'Coin'" />
        <List v-else-if="val.meta.icon === 'List'" />
      </el-icon>
      <span>{{ val.meta?.title || val.name }}</span>
    </el-menu-item>
    <el-sub-menu
      v-else
      :index="val.path"
    >
      <template #title>
        <el-icon
          v-if="val.meta?.icon"
          class="menu-icon"
        >
          <HomeFilled v-if="val.meta.icon === 'HomeFilled'" />
          <User v-else-if="val.meta.icon === 'User'" />
          <Lock v-else-if="val.meta.icon === 'Lock'" />
          <Search v-else-if="val.meta.icon === 'Search'" />
          <Setting v-else-if="val.meta.icon === 'Setting'" />
          <Fold v-else-if="val.meta.icon === 'Fold'" />
          <Expand v-else-if="val.meta.icon === 'Expand'" />
          <Close v-else-if="val.meta.icon === 'Close'" />
          <Box v-else-if="val.meta.icon === 'Box'" />
          <Document v-else-if="val.meta.icon === 'Document'" />
          <Edit v-else-if="val.meta.icon === 'Edit'" />
          <Upload v-else-if="val.meta.icon === 'Upload'" />
          <DataLine v-else-if="val.meta.icon === 'DataLine'" />
          <Key v-else-if="val.meta.icon === 'Key'" />
          <Grid v-else-if="val.meta.icon === 'Grid'" />
          <Coin v-else-if="val.meta.icon === 'Coin'" />
          <List v-else-if="val.meta.icon === 'List'" />
        </el-icon>
        <span>{{ val.meta?.title || val.name }}</span>
      </template>
      <SubItem :chil="val.children" />
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts" name="navMenuSubItem">
import {
  HomeFilled,
  User,
  Lock,
  Search,
  Setting,
  Close,
  Fold,
  Expand,
  Box,
  Document,
  Edit,
  Upload,
  DataLine,
  Key,
  Grid,
  Coin,
  List,
} from '@element-plus/icons-vue';

defineProps<{
  chil: any[];
}>();
</script>

<style scoped>
.menu-icon {
  margin-right: 8px;
}
</style>
