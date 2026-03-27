<template>
  <el-dialog v-model="visible" title="选择图标" width="800px" destroy-on-close>
    <div class="icon-selector">
      <el-input v-model="keyword" placeholder="搜索图标" clearable class="search-input" />
      <div class="icon-list">
        <div
          v-for="icon in filteredIcons"
          :key="icon"
          :class="['icon-item', { active: modelValue === icon }]"
          @click="handleSelect(icon)"
        >
          <el-icon :size="24">
            <component :is="icon" />
          </el-icon>
          <span class="icon-name">{{ icon }}</span>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts" name="IconSelector">
import { ref, computed, watch } from 'vue';

// Element Plus 图标列表（常用图标）
const elementIcons = [
  'Plus', 'Minus', 'CirclePlus', 'Search', 'Edit', 'Delete', 'Check', 'Close',
  'Plus', 'Refresh', 'RefreshRight', 'RefreshLeft', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight',
  'Upload', 'Download', 'UploadFilled', 'DownloadFilled',
  'User', 'UserFilled', 'Avatar', 'Setting', 'Tools', 'Gear',
  'Home', 'House', 'OfficeBuilding', 'School',
  'Menu', 'Grid', 'List', 'Sort', 'Filter', 'Sunny', 'Moon', 'Lightning',
  'View', 'Hide', 'Show', 'Document', 'DocumentChecked', 'DocumentCopy',
  'Tickets', 'Ticket', 'Collection', 'Folder', 'FolderOpened', 'Files',
  'Link', 'Connection', 'Share', 'Position', 'Location', 'LocationInformation',
  'Phone', 'PhoneFilled', 'Message', 'ChatDotRound', 'ChatLineRound', 'ChatLineSquare', 'ChatRound', 'ChatSquare',
  'Postcard', 'MessageBox', 'ChatCentered', 'Promotion', 'Coin', 'Money', 'CreditCard',
  'Card', 'Aim', 'AlarmClock', 'Clock', 'Timer', 'Stopwatch',
  'Key', 'Lock', 'Unlock', 'Safety', 'SafetyButton', 'Shield', 'Protect', 'Bug',
  'Bell', 'BellFilled', 'Mute', 'MuteNotification', 'Microphone', 'MicrophoneClosed',
  'Headset', 'VideoCamera', 'VideoCameraFilled', 'VideoPause', 'VideoPlay',
  'Monitor', 'Display', 'Projector', 'DataLine', 'DataAnalysis', 'DataBoard',
  'PieChart', 'Histogram', 'LineChart', 'TrendCharts', 'Growth', 'Guide',
  'Coordinate', 'MapLocation', 'Map', 'Place', 'LocationFilled', 'Van', 'Bicycle',
  'Truck', 'Ship', 'Airplane', 'Train', 'Bus', 'Car', 'Motorcycle',
  'Currency', 'PriceTag', 'ShoppingCart', 'ShoppingCartFull', 'ShoppingTrolley', 'Goods', 'SellCar',
  'OfficeBuilding', 'Factory', 'Store', 'Restaurant', 'Cinema', 'Hotel', 'Hospital',
  'Ticket', 'Melon', 'Apple', 'Grape', 'Pear', 'Orange', 'Cherry', 'Watermelon',
  'Dish', 'DishDot', 'Knife', 'Spoon', 'Bowl', 'Cup', 'ColdDrink', 'IceCream',
  'Sugar', 'Bread', 'Goblet', 'Glass', 'ForkSpoon', 'Scissor',
  'Brush', 'Palette', 'PaintBucket', 'Pen', 'Pencil', 'EditPen', 'Memo', 'Stamp',
  'Stamp', 'Finished', 'Clock', 'ClockFilled', 'Reminder', 'Stopwatch', 'Timer',
  'Sunrise', 'Sunset', 'Cloudy', 'PartlyCloudy', 'Sunny', 'Moon', 'Moonlit',
  'Wind', 'Umbrella', 'Snowy', 'HeavySnow', 'HeavyRain', 'Lightning', 'Pouring',
  'Drizzling', 'Sifting', 'Sunrise', 'Sunset', 'WindPower', 'Coin', 'Money',
];

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits(['update:modelValue', 'change']);

const visible = ref(false);
const keyword = ref('');

const filteredIcons = computed(() => {
  if (!keyword.value) return elementIcons;
  return elementIcons.filter((icon) =>
    icon.toLowerCase().includes(keyword.value.toLowerCase())
  );
});

watch(visible, (val) => {
  if (val) {
    keyword.value = '';
  }
});

const handleSelect = (icon: string) => {
  emit('update:modelValue', icon);
  emit('change', icon);
  visible.value = false;
};

// 暴露方法
defineExpose({
  open: () => {
    visible.value = true;
  },
  close: () => {
    visible.value = false;
  },
});
</script>

<style scoped lang="scss">
.icon-selector {
  .search-input {
    margin-bottom: 16px;
  }

  .icon-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
    max-height: 400px;
    overflow-y: auto;
  }

  .icon-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 12px 8px;
    border: 1px solid var(--mango-border-color);
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: var(--mango-color-primary);
      background: var(--mango-color-menu-hover);
    }

    &.active {
      border-color: var(--mango-color-primary);
      background: var(--mango-color-primary-lighter);
      color: var(--mango-color-primary);
    }

    .icon-name {
      margin-top: 4px;
      font-size: 12px;
      color: var(--mango-text-color-regular);
    }
  }
}
</style>
