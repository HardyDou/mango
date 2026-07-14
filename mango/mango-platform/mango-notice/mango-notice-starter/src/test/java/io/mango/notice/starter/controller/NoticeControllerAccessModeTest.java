package io.mango.notice.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.notice.api.command.ExecuteNoticeSiteMessageActionCommand;
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
    void personalNoticeEndpointsShouldUseLoginDefaultRolePermissions() throws NoSuchMethodException {
        assertPermission("listRecipientAccounts", "notice:receive-setting:view", NoticeRecipientAccountQuery.class);
        assertPermission("saveRecipientAccount", "notice:receive-setting:edit", SaveNoticeRecipientAccountCommand.class);
        assertPermission("disableRecipientAccount", "notice:receive-setting:edit", Long.class, Long.class);
        assertPermission("setDefaultRecipientAccount", "notice:receive-setting:edit", Long.class, Long.class);
        assertPermission("listReceivePreferences", "notice:receive-setting:view", NoticeReceivePreferenceQuery.class);
        assertPermission("saveReceivePreference", "notice:receive-setting:edit", SaveNoticeReceivePreferenceCommand.class);
        assertPermission("listSiteMessages", "notice:site:view", NoticeSiteMessagePageQuery.class);
        assertPermission("getSiteMessage", "notice:site:view", Long.class);
        assertPermission("executeSiteMessageAction", "notice:site:edit",
                ExecuteNoticeSiteMessageActionCommand.class);
        assertPermission("unreadCount", "notice:site:view");
        assertPermission("markSiteMessageRead", "notice:site:edit", Long.class);
        assertPermission("markSiteMessagesRead", "notice:site:edit", MarkNoticeReadCommand.class);
        assertPermission("markAllSiteMessagesRead", "notice:site:edit");
        assertPermission("deleteSiteMessage", "notice:site:edit", Long.class);
    }

    private static void assertPermission(String methodName, String permission, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = NoticeController.class.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
        assertThat(apiAccess.permission()).isEqualTo(permission);
    }
}
