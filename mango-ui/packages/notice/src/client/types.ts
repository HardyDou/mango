export interface NoticeClientBellRuntimeConfig {
  voiceEnabled?: boolean;
  voiceText?: string;
  soundEnabled?: boolean;
  soundText?: string;
  popupEnabled?: boolean;
  popupPlacement?: 'top-right' | 'bottom-right';
  desktopNotificationEnabled?: boolean;
}
