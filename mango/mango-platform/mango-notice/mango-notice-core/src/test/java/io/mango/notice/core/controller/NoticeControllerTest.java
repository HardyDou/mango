package io.mango.notice.core.controller;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.core.service.INoticeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeControllerTest {

 @Test
 void sendSiteMessage_forcesSiteChannelAndDelegatesToUnifiedSend() {
 INoticeService noticeService = mock(INoticeService.class);
 NoticeController controller = new NoticeController(noticeService, mock(ISecurityContextProvider.class));
 when(noticeService.send(any(SendNoticeCommand.class))).thenReturn(new NoticeSendResultVO(1, 0));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("SYSTEM_NOTICE");
 command.setUserId(1001L);
 command.setTitle("后台系统消息");
 command.setContent("后台发送内容");

 var result = controller.sendSiteMessage(command);

 assertTrue(result.isSuccess());
 assertEquals(1, result.getData().getSuccessCount());
 ArgumentCaptor<SendNoticeCommand> captor = ArgumentCaptor.forClass(SendNoticeCommand.class);
 verify(noticeService).send(captor.capture());
 assertEquals(List.of(NoticeChannelType.SITE), captor.getValue().getChannelTypes());
 assertEquals(1001L, captor.getValue().getUserId());
 assertEquals("后台系统消息", captor.getValue().getTitle());
 }
}
