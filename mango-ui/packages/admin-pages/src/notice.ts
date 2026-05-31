import type { Component } from 'vue';
import type { RealtimeOptions } from '@mango/common/utils/realtime/types';

export type MangoNoticeBellRuntimeConfig = {
  voiceEnabled: boolean;
  reminderMode: string;
  voiceText: string;
  soundType: string;
  popupEnabled: boolean;
  popupPlacement: string;
  desktopNotificationEnabled: boolean;
};

export type MangoNoticeBellProvider = {
  component: Component;
  getReminderSetting: () => Promise<Partial<MangoNoticeBellRuntimeConfig>>;
};

let noticeBellProvider: MangoNoticeBellProvider | undefined;

export function registerMangoNoticeBellProvider(provider: MangoNoticeBellProvider) {
  noticeBellProvider = provider;
}

export function getMangoNoticeBellProvider() {
  return noticeBellProvider;
}

export type MangoNoticeBellProps = {
  loadRuntimeConfig: () => Promise<MangoNoticeBellRuntimeConfig>;
  realtimeOptions: RealtimeOptions;
};
