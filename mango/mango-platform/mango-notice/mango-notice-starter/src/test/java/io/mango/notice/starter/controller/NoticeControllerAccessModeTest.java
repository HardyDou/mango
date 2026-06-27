package io.mango.notice.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeControllerAccessModeTest {

    @Test
    void personalNoticeEndpointsShouldUseLoginBaselineAccess() throws NoSuchMethodException {
        assertLoginBaseline("listRecipientAccounts", NoticeRecipientAccountQuery.class);
        assertLoginBaseline("saveRecipientAccount", SaveNoticeRecipientAccountCommand.class);
        assertLoginBaseline("disableRecipientAccount", Long.class, Long.class);
        assertLoginBaseline("setDefaultRecipientAccount", Long.class, Long.class);
        assertLoginBaseline("listReceivePreferences", NoticeReceivePreferenceQuery.class);
        assertLoginBaseline("saveReceivePreference", SaveNoticeReceivePreferenceCommand.class);
        assertLoginBaseline("listSiteMessages", NoticeSiteMessagePageQuery.class);
        assertLoginBaseline("getSiteMessage", Long.class);
        assertLoginBaseline("unreadCount");
        assertLoginBaseline("markSiteMessageRead", Long.class);
        assertLoginBaseline("markSiteMessagesRead", MarkNoticeReadCommand.class);
        assertLoginBaseline("markAllSiteMessagesRead");
        assertLoginBaseline("deleteSiteMessage", Long.class);
    }

    private static void assertLoginBaseline(String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = NoticeController.class.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(apiAccess.permission()).isBlank();
    }
}
