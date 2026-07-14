package io.mango.notice.core.service;

import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.vo.NoticeReceivePreferenceVO;
import io.mango.notice.api.vo.NoticeRecipientAccountVO;
import io.mango.notice.api.vo.NoticeSettingsVO;

import java.util.List;

/** User-facing recipient accounts, preferences and reminder settings. */
public interface INoticeRecipientSettingService {

    NoticeSettingsVO getSettings();

    boolean saveSettings(SaveNoticeSettingsCommand command);

    List<NoticeRecipientAccountVO> listRecipientAccounts(NoticeRecipientAccountQuery query);

    NoticeRecipientAccountVO saveRecipientAccount(SaveNoticeRecipientAccountCommand command);

    boolean disableRecipientAccount(Long id, Long userId);

    boolean setDefaultRecipientAccount(Long id, Long userId);

    List<NoticeReceivePreferenceVO> listReceivePreferences(NoticeReceivePreferenceQuery query);

    NoticeReceivePreferenceVO saveReceivePreference(SaveNoticeReceivePreferenceCommand command);
}
