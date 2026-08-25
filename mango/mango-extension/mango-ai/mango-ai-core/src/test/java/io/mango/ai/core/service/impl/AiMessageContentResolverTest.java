package io.mango.ai.core.service.impl;

import io.mango.ai.api.command.AiMessageContentPartCommand;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiMessageContentType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import io.mango.ai.core.service.AiAssistantMediaInput;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.common.exception.BizException;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiMessageContentResolverTest {

    @Test
    void normalize_文件无权读取时明确拒绝() {
        IFileContentProvider provider = mock(IFileContentProvider.class);
        when(provider.downloadForService(10L)).thenReturn(null);
        AiMessageContentResolver resolver = resolver(provider);

        assertThrows(BizException.class, () -> resolver.normalize(List.of(fileCommand(
                AiMessageContentType.IMAGE, 10L))));
    }

    @Test
    void normalize_文件类型与内容块不匹配时明确拒绝() {
        IFileContentProvider provider = mock(IFileContentProvider.class);
        when(provider.downloadForService(10L)).thenReturn(download("contract.pdf", "application/pdf", 8));
        AiMessageContentResolver resolver = resolver(provider);

        assertThrows(BizException.class, () -> resolver.normalize(List.of(fileCommand(
                AiMessageContentType.IMAGE, 10L))));
    }

    @Test
    void normalize_单文件超过20MB时明确拒绝() {
        IFileContentProvider provider = mock(IFileContentProvider.class);
        when(provider.downloadForService(10L)).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(new byte[] {1}), "large.png", "image/png", 20L * 1024L * 1024L + 1L));
        AiMessageContentResolver resolver = resolver(provider);

        assertThrows(BizException.class, () -> resolver.normalize(List.of(fileCommand(
                AiMessageContentType.IMAGE, 10L))));
    }

    @Test
    void toUserMessage_模型不支持图片时在调用前拒绝() {
        IFileContentProvider provider = mock(IFileContentProvider.class);
        when(provider.downloadForService(10L)).thenAnswer(invocation -> download("photo.png", "image/png", 8));
        AiMessageContentResolver resolver = resolver(provider);
        List<AiMessageContentPartVO> parts = resolver.normalize(List.of(fileCommand(AiMessageContentType.IMAGE, 10L)));

        assertThrows(BizException.class, () -> resolver.toUserMessage(parts, resolution(Set.of(AiModality.TEXT))));
    }

    @Test
    void validateContextFileBudget_多轮累计超过80MB时拒绝() {
        AiMessageContentResolver resolver = resolver();
        AiMessageContentPartVO file20Mb = storedFile(20L * 1024L * 1024L);
        AiMessageContentPartVO oneByte = storedFile(1L);

        assertThrows(BizException.class,
                () -> resolver.validateContextFileBudget(List.of(
                        List.of(file20Mb), List.of(file20Mb), List.of(file20Mb), List.of(file20Mb),
                        List.of(oneByte))));
    }

    @Test
    void saveAssistantMedia_保存到文件中心且只返回文件标识与元数据() {
        IFileContentProvider provider = mock(IFileContentProvider.class);
        FileRecordVO saved = new FileRecordVO();
        saved.setId(99L);
        saved.setFileName("answer.png");
        saved.setFileSize(3L);
        saved.setContentType("image/png");
        when(provider.save(any(SaveFileCommand.class))).thenReturn(saved);
        AiMessageContentResolver resolver = resolver(provider);
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new ByteArrayResource(new byte[] {1, 2, 3}))
                .name("answer.png")
                .build();

        AiMessageContentPartVO part = resolver.saveAssistantMedia(new AiAssistantMediaInput(media, "request-1", 1));

        ArgumentCaptor<SaveFileCommand> captor = ArgumentCaptor.forClass(SaveFileCommand.class);
        verify(provider).save(captor.capture());
        assertEquals("PRIVATE", captor.getValue().getAccessLevel());
        assertEquals("AI_CHAT", captor.getValue().getBizType());
        assertEquals(99L, part.getFileId());
        assertEquals(AiMessageContentType.IMAGE, part.getType());
        assertTrue(part.getContentType().startsWith("image/"));
    }

    private AiMessageContentResolver resolver(IFileContentProvider... providers) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        for (int index = 0; index < providers.length; index++) {
            beans.addBean("fileContentProvider" + index, providers[index]);
        }
        return new AiMessageContentResolver(beans.getBeanProvider(IFileContentProvider.class));
    }

    private AiMessageContentPartCommand fileCommand(AiMessageContentType type, Long fileId) {
        AiMessageContentPartCommand command = new AiMessageContentPartCommand();
        command.setType(type);
        command.setFileId(fileId);
        return command;
    }

    private FileDownloadVO download(String fileName, String contentType, int size) {
        byte[] content = new byte[size];
        return new FileDownloadVO(new ByteArrayInputStream(content), fileName, contentType, content.length);
    }

    private AiMessageContentPartVO storedFile(long fileSize) {
        AiMessageContentPartVO part = new AiMessageContentPartVO();
        part.setType(AiMessageContentType.FILE);
        part.setFileId(10L);
        part.setFileName("large.pdf");
        part.setContentType("application/pdf");
        part.setFileSize(fileSize);
        return part;
    }

    private AiModelResolution resolution(Set<AiModality> inputModalities) {
        return new AiModelResolution(
                1L, mock(ChatModel.class), "provider", "model", AiApiProtocol.CHAT_COMPLETIONS,
                false, inputModalities, Set.of(AiModality.TEXT));
    }
}
