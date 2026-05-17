<template>
  <div class="layout-columns-aside">
    <el-scrollbar>
      <ul @mouseleave="onColumnsAsideMenuMouseleave()">
        <li
          v-for="(v, k) in columnsAsideList"
          :key="k"
          :ref="(el) => { if (el) columnsAsideOffsetTopRefs[k] = el as HTMLElement }"
          :class="{ 'layout-columns-active': liIndex === k }"
          :title="v.meta?.title || v.name"
          @click="onColumnsAsideMenuClick(v, k)"
          @mouseenter="onColumnsAsideMenuMouseenter(v, k)"
        >
          <div :class="layoutStore.columnsAsideLayout">
            <el-icon
              v-if="v.meta?.icon && iconMap[v.meta.icon]"
              class="menu-icon"
            >
              <component :is="iconMap[v.meta.icon]" />
            </el-icon>
            <div class="columns-vertical-title font12">
              {{ getTitle(v) }}
            </div>
          </div>
        </li>
        <div
          ref="columnsAsideActiveRef"
          :class="layoutStore.columnsAsideStyle"
        />
      </ul>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts" name="layoutColumnsAside">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useLayoutStore } from '@/stores/layout';
import { useRoutesList } from '@/stores/routesList';
import { mittBus } from '@mango/common/utils/mitt';
import { iconMap } from '@/config/iconConfig';

interface MenuItem {
  path: string;
  name?: string;
  meta?: { title?: string; icon?: string; isLink?: string; isHide?: boolean };
  children?: MenuItem[];
  k?: number;
}

const columnsAsideOffsetTopRefs = ref<HTMLElement[]>([]);
const columnsAsideActiveRef = ref<HTMLElement>();
const route = useRoute();
const router = useRouter();
const layoutStore = useLayoutStore();
const storesRoutesList = useRoutesList();
const { routesList, activeTopRoutePath } = storeToRefs(storesRoutesList);

const liIndex = ref(0);
const difference = computed(() => (layoutStore.columnsAsideStyle === 'columns-round' ? 3 : 0));

const columnsAsideList = computed(() => {
  return filterRoutes(routesList.value);
});

const getTitle = (v: MenuItem) => {
  const title = v.meta?.title || v.name || '';
  const maxLen = layoutStore.columnsAsideLayout === 'columns-vertical' ? 4 : 3;
  return title.length >= maxLen ? title.substring(0, maxLen) : title;
};

const filterRoutes = <T extends MenuItem>(arr: T[]): T[] => {
  return arr
    .filter((item: T) => !item.meta?.isHide)
    .map((item: T) => {
      item = { ...item };
      if (item.children) item.children = filterRoutes(item.children);
      return item;
    });
};

const setColumnsAsideMove = (k: number) => {
  liIndex.value = k;
  nextTick(() => {
    if (columnsAsideActiveRef.value && columnsAsideOffsetTopRefs.value[k]) {
      columnsAsideActiveRef.value.style.top = `${columnsAsideOffsetTopRefs.value[k].offsetTop + difference.value}px`;
    }
  });
};

const onColumnsAsideMenuClick = (v: MenuItem, k: number) => {
  setColumnsAsideMove(k);
  storesRoutesList.setActiveTopRoutePath(v.path);
  if (v.children && v.children.length > 0) {
    mittBus.emit('setSendColumnsChildren', setSendChildren(v.path));
    return;
  }
  router.push(v.path);
};

const onColumnsAsideMenuMouseenter = (_v: MenuItem, _k: number) => {};

const onColumnsAsideMenuMouseleave = async () => {};

const setSendChildren = (path: string) => {
  const parentRoute = searchParent(routesList.value, path);
  let currentData: { children: MenuItem[]; item?: MenuItem } = { children: [] };
  columnsAsideList.value.map((v: MenuItem, k: number) => {
    if (v.path === parentRoute?.path) {
      v.k = k;
      currentData.item = { ...v };
      currentData.children = [{ ...v }];
      if (v.children) currentData.children = v.children;
    }
  });
  return currentData;
};

const searchParent = (routesList: MenuItem[], path: string): MenuItem | undefined => {
  let route: MenuItem | undefined;
  routesList.forEach((item) => {
    if (item.path === path) {
      route = item;
      return;
    }
    if (item.children && searchParent(item.children, path)) {
      route = item;
      return;
    }
  });
  return route;
};

const setColumnsMenuHighlight = (path: string) => {
  const parentRoute = searchParent(routesList.value, path);
  const currentSplitRoute = columnsAsideList.value.find((v: MenuItem) => v.path === parentRoute?.path);
  if (!currentSplitRoute) return false;
  setTimeout(() => {
    setColumnsAsideMove(currentSplitRoute.k || 0);
  }, 0);
};

let cleanupRestore: (() => void) | undefined;

onMounted(() => {
  setFilterRoutes();
  cleanupRestore = mittBus.on('restoreDefault', () => {
    setFilterRoutes();
  });
});

onUnmounted(() => {
  cleanupRestore?.();
});

const setFilterRoutes = () => {
  const resData = setSendChildren(activeTopRoutePath.value || route.path);
  if (Object.keys(resData).length <= 0) return false;
  setColumnsAsideMove(resData.item?.k || 0);
  mittBus.emit('setSendColumnsChildren', resData);
};

watch(
  () => route.path,
  () => {
    setColumnsMenuHighlight(route.path);
    mittBus.emit('setSendColumnsChildren', setSendChildren(route.path));
  }
);

watch(
  () => activeTopRoutePath.value,
  (value) => {
    if (!value) return;
    mittBus.emit('setSendColumnsChildren', setSendChildren(value));
  }
);

watch(
  () => layoutStore.columnsAsideStyle,
  () => {
    if (layoutStore.columnsAsideStyle === 'columns-round') {
      difference.value;
    }
  }
);
</script>

<style scoped lang="scss">
.layout-columns-aside {
  width: 64px;
  height: 100%;
  background: var(--mango-bg-columns-menu-bar);

  ul {
    position: relative;
    list-style: none;
    padding: 0;
    margin: 0;

    .layout-columns-active {
      color: #fff;
      transition: 0.3s ease-in-out;
    }
    li {
      color: #fff;
      width: 100%;
      height: 50px;
      text-align: center;
      display: flex;
      cursor: pointer;
      position: relative;
      z-index: 1;

      &:hover {
        color: #fff;
      }

      .columns-vertical {
        margin: auto;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 100%;

        .columns-vertical-title {
          padding-top: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 100%;
        }
      }

      .columns-horizontal {
        display: flex;
        height: 50px;
        width: 100%;
        align-items: center;
        padding: 0 5px;

        .menu-icon {
          margin-right: 3px;
        }
      }

      a {
        text-decoration: none;
        color: var(--mango-color-menu-bar);
        display: flex;
        align-items: center;
        width: 100%;
        justify-content: center;
      }

      .menu-icon {
        font-size: 18px;
        margin-right: 4px;
      }
    }

    .columns-round {
      background: var(--mango-color-primary);
      color: #fff;
      position: absolute;
      left: 50%;
      top: 2px;
      height: 44px;
      width: 58px;
      transform: translateX(-50%);
      z-index: 0;
      transition: 0.3s ease-in-out;
      border-radius: 8px;
    }

    .columns-card {
      @extend .columns-round;
      top: 0;
      height: 50px;
      width: 100%;
      border-radius: 0;
    }
  }
}

.font12 {
  font-size: 12px;
}
</style>
