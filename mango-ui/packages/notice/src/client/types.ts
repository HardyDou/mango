export interface NoticeClientBellRuntimeConfig {
  voiceEnabled?: boolean;
  reminderMode?: 'SOUND' | 'VOICE';
  voiceText?: string;
  soundEnabled?: boolean;
  soundText?: string;
  soundType?: 'IM' | 'SOFT' | 'DOUBLE' | 'NONE';
  popupEnabled?: boolean;
  popupPlacement?: 'top-right' | 'bottom-right';
  desktopNotificationEnabled?: boolean;
}
