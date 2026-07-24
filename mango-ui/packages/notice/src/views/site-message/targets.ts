const NOTICE_TARGET_PATHS: Readonly<Record<string, string>> = {
  'notice:receive-setting': '/message-center/receive-setting',
  'notice:announcement-user': '/message-center/announcement',
  'account:profile': '/profile',
  'account:password': '/password',
};

export function resolveNoticeTargetPath(targetKey: string): string | undefined {
  return NOTICE_TARGET_PATHS[targetKey];
}
