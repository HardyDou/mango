package io.mango.notice.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.notice.api.command.ExecuteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.NoticeAnnouncementIdCommand;
import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.query.NoticeAnnouncementIdQuery;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeInboundMessagePageQuery;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeControllerAccessModeTest {

    @Test
    void inboundMessageAdministrationShouldRequireExplicitPermission() throws NoSuchMethodException {
        assertPermission("listInboundMessages", "notice:inbound:view", NoticeInboundMessagePageQuery.class);
        assertPermission("getInboundMessage", "notice:inbound:view", Long.class);
    }

    @Test
    void recipientAccountAdministrationShouldKeepExplicitPermissions() throws NoSuchMethodException {
        assertPermission("listRecipientAccounts", "notice:receive-setting:view", NoticeRecipientAccountQuery.class);
        assertPermission("saveRecipientAccount", "notice:receive-setting:edit", SaveNoticeRecipientAccountCommand.class);
        assertPermission("disableRecipientAccount", "notice:receive-setting:edit", Long.class, Long.class);
        assertPermission("setDefaultRecipientAccount", "notice:receive-setting:edit", Long.class, Long.class);
    }

    @Test
    void personalNoticeEndpointsShouldOnlyRequireLogin() throws NoSuchMethodException {
        assertLogin(NoticeController.class, "listEnabledBusinessTypes", NoticeBusinessTypePageQuery.class);
        assertLogin(NoticeController.class, "listReceivePreferences", NoticeReceivePreferenceQuery.class);
        assertLogin(NoticeController.class, "saveReceivePreference", SaveNoticeReceivePreferenceCommand.class);
        assertLogin(NoticeController.class, "listSiteMessages", NoticeSiteMessagePageQuery.class);
        assertLogin(NoticeController.class, "getSiteMessage", Long.class);
        assertLogin(NoticeController.class, "executeSiteMessageAction",
                ExecuteNoticeSiteMessageActionCommand.class);
        assertLogin(NoticeController.class, "unreadCount");
        assertLogin(NoticeController.class, "unreadCategoryStats");
        assertLogin(NoticeController.class, "markSiteMessageRead", Long.class);
        assertLogin(NoticeController.class, "markSiteMessagesRead", MarkNoticeReadCommand.class);
        assertLogin(NoticeController.class, "markAllSiteMessagesRead");
        assertLogin(NoticeController.class, "deleteSiteMessage", Long.class);
        assertLogin(NoticeAnnouncementController.class, "pageMyAnnouncements",
                MyNoticeAnnouncementPageQuery.class);
        assertLogin(NoticeAnnouncementController.class, "getMyAnnouncement",
                NoticeAnnouncementIdQuery.class);
        assertLogin(NoticeAnnouncementController.class, "confirmMyAnnouncement",
                NoticeAnnouncementIdCommand.class);
    }

    private static void assertPermission(String methodName, String permission, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = NoticeController.class.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
        assertThat(apiAccess.permission()).isEqualTo(permission);
    }

    private static void assertLogin(
            Class<?> controller, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = controller.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(apiAccess.permission()).isEmpty();
    }
}
