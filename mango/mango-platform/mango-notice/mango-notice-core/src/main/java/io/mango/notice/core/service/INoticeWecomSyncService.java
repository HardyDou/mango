package io.mango.notice.core.service;

import io.mango.notice.api.command.SyncWecomUsersCommand;
import io.mango.notice.api.vo.WecomUserSyncResultVO;

/** Synchronizes WeCom departments, users, login identities and notice accounts. */
public interface INoticeWecomSyncService {

    WecomUserSyncResultVO syncWecomUsers(SyncWecomUsersCommand command);
}
