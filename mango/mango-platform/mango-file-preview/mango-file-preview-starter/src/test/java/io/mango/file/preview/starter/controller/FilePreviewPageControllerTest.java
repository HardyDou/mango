package io.mango.file.preview.starter.controller;

import io.mango.file.preview.api.enums.FilePreviewTaskStatus;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewMode;
import io.mango.file.preview.core.task.IFilePreviewTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilePreviewPageControllerTest {

    private static final String TOKEN = "entry-token";
    private static final String ENGINE_URL = "/api/onlinePreview?url=encoded-source";

    private final IFilePreviewService filePreviewService = mock(IFilePreviewService.class);
    private final IFilePreviewTaskService previewTaskService = mock(IFilePreviewTaskService.class);
    private final FilePreviewPageController controller =
            new FilePreviewPageController(filePreviewService, previewTaskService);

    @Test
    void archiveSkipsConversionAndRedirectsToEngine() throws Exception {
        when(filePreviewService.previewMode(TOKEN)).thenReturn(FilePreviewMode.ENGINE);
        when(filePreviewService.createEnginePreviewByToken(TOKEN)).thenReturn(link(ENGINE_URL));

        RenderedPage page = render(controller.redirectPreviewEntry(TOKEN, false, false));

        assertThat(page.status()).isEqualTo(302);
        assertThat(page.location()).isEqualTo(ENGINE_URL);
        verify(previewTaskService, never()).submit(anyLong(), anyBoolean());
        verify(filePreviewService, never()).previewTaskByToken(anyString(), anyBoolean());
    }

    @Test
    void nativePdfUsesPlatformPdfViewerWithOriginalContent() throws Exception {
        when(filePreviewService.previewMode(TOKEN)).thenReturn(FilePreviewMode.NATIVE_PDF);

        RenderedPage page = render(controller.redirectPreviewEntry(TOKEN, false, false));

        assertThat(page.body()).contains("/pdfjs/web/viewer.html?file=")
                .contains("preview-content?token=" + TOKEN);
        verify(filePreviewService, never()).previewTaskByToken(anyString(), anyBoolean());
    }

    @Test
    void convertibleOfficeDocumentStaysOnConversionProgressUntilArtifactIsReady() throws Exception {
        when(filePreviewService.previewMode(TOKEN)).thenReturn(FilePreviewMode.PDF_CONVERSION);
        when(filePreviewService.previewTaskByToken(TOKEN, false)).thenReturn(task(FilePreviewTaskStatus.QUEUED, null));

        RenderedPage page = render(controller.redirectPreviewEntry(TOKEN, false, false));

        assertThat(page.status()).isEqualTo(200);
        assertThat(page.body()).contains("文件正在转换").contains("preview-progress?token=");
        verify(filePreviewService, never()).createEnginePreviewByToken(anyString());
    }

    @Test
    void convertedOfficeDocumentUsesTokenProtectedArtifactInPdfViewer() throws Exception {
        when(filePreviewService.previewMode(TOKEN)).thenReturn(FilePreviewMode.PDF_CONVERSION);
        when(filePreviewService.previewTaskByToken(TOKEN, true)).thenReturn(task(FilePreviewTaskStatus.SUCCEEDED, 99L));

        RenderedPage page = render(controller.redirectPreviewEntry(TOKEN, true, true));

        assertThat(page.body()).contains("preview-artifact?token=" + TOKEN);
        verify(filePreviewService, never()).createEnginePreviewByToken(anyString());
    }

    @Test
    void conversionWithoutArtifactFallsBackToEngineRendering() throws Exception {
        when(filePreviewService.previewMode(TOKEN)).thenReturn(FilePreviewMode.PDF_CONVERSION);
        when(filePreviewService.previewTaskByToken(TOKEN, true)).thenReturn(task(FilePreviewTaskStatus.SUCCEEDED, null));
        when(filePreviewService.createEnginePreviewByToken(TOKEN)).thenReturn(link(ENGINE_URL));

        RenderedPage page = render(controller.redirectPreviewEntry(TOKEN, true, true));

        assertThat(page.status()).isEqualTo(302);
        assertThat(page.location()).isEqualTo(ENGINE_URL);
    }

    private RenderedPage render(ModelAndView modelAndView) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        modelAndView.getView().render(modelAndView.getModel(), request, response);
        return new RenderedPage(response.getStatus(), response.getRedirectedUrl(),
                response.getContentAsString());
    }

    private FilePreviewLinkVO link(String previewUrl) {
        FilePreviewLinkVO link = new FilePreviewLinkVO();
        link.setPreviewUrl(previewUrl);
        return link;
    }

    private FilePreviewTaskVO task(FilePreviewTaskStatus status, Long previewFileId) {
        FilePreviewTaskVO task = new FilePreviewTaskVO();
        task.setStatus(status);
        task.setPreviewFileId(previewFileId);
        task.setMessage("任务消息");
        return task;
    }

    private record RenderedPage(int status, String location, String body) {
    }
}
