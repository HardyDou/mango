import { shallowRef } from 'vue';
import type { Component } from 'vue';
import type { RealtimeOptions } from '@mango/common/utils/realtime/types';

export type MangoNoticeBellRuntimeConfig = {
  voiceEnabled?: boolean;
  reminderMode?: 'SOUND' | 'VOICE';
  voiceText?: string;
  soundEnabled?: boolean;
  soundText?: string;
  soundType?: 'IM' | 'SOFT' | 'DOUBLE' | 'NONE';
  popupEnabled?: boolean;
  popupPlacement?: 'top-right' | 'bottom-right';
  desktopNotificationEnabled?: boolean;
};

export type MangoNoticeBellProvider = {
  component: Component;
  getReminderSetting: () => Promise<Partial<MangoNoticeBellRuntimeConfig>>;
};

const noticeBellProvider = shallowRef<MangoNoticeBellProvider>();

export function registerMangoNoticeBellProvider(provider: MangoNoticeBellProvider) {
  noticeBellProvider.value = provider;
}

export function getMangoNoticeBellProvider() {
  return noticeBellProvider.value;
}

export type MangoNoticeBellProps = {
  loadRuntimeConfig: () => Promise<MangoNoticeBellRuntimeConfig>;
  realtimeOptions: RealtimeOptions;
};
