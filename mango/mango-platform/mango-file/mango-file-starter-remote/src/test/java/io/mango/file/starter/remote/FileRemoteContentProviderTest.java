package io.mango.file.starter.remote;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileRemoteContentProviderTest {

    private final FileBinaryFeignClient client = mock(FileBinaryFeignClient.class);
    private final FileRemoteContentProvider provider = new FileRemoteContentProvider(client);

    @Test
    void saveForwardsBinaryContentAndBusinessMetadata() throws Exception {
        SaveFileCommand command = command();
        FileRecordVO expected = new FileRecordVO();
        expected.setId(99L);
        when(client.upload(any(), eq("contract"), eq("PRIVATE"), eq("ORDER"), eq("A-1"),
                eq("{\"source\":\"test\"}"), eq(8L))).thenReturn(R.ok(expected));

        FileRecordVO actual = provider.save(command);

        ArgumentCaptor<MultipartFile> file = ArgumentCaptor.forClass(MultipartFile.class);
        verify(client).upload(file.capture(), eq("contract"), eq("PRIVATE"), eq("ORDER"), eq("A-1"),
                eq("{\"source\":\"test\"}"), eq(8L));
        assertThat(actual).isSameAs(expected);
        assertThat(file.getValue().getOriginalFilename()).isEqualTo("contract.txt");
        assertThat(file.getValue().getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(file.getValue().getBytes()).isEqualTo("file-body".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void saveRejectsFailedRemoteResponse() {
        when(client.upload(any(), any(), any(), any(), any(), any(), any())).thenReturn(R.fail("remote failed"));

        assertThatThrownBy(() -> provider.save(command()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("remote failed");
    }

    @Test
    void downloadPreservesResponseBodyAndHeaders() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("report.pdf").build());
        when(client.download(7L)).thenReturn(ResponseEntity.ok().headers(headers).body(new byte[]{1, 2, 3}));

        FileDownloadVO download = provider.downloadForService(7L);

        assertThat(download.fileName()).isEqualTo("report.pdf");
        assertThat(download.contentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(download.contentLength()).isEqualTo(3L);
        assertThat(download.inputStream().readAllBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void binaryFeignContractKeepsPublishedRootAndDownloadPath() throws Exception {
        FeignClient feignClient = FileBinaryFeignClient.class.getAnnotation(FeignClient.class);
        GetMapping download = FileBinaryFeignClient.class.getMethod("download", Long.class)
                .getAnnotation(GetMapping.class);

        assertThat(feignClient.path()).isEqualTo("/file/files");
        assertThat(download.value()).containsExactly("/download");
    }

    private SaveFileCommand command() {
        SaveFileCommand command = new SaveFileCommand();
        command.setInputStream(new ByteArrayInputStream("file-body".getBytes(StandardCharsets.UTF_8)));
        command.setFileName("contract.txt");
        command.setFileSize(9L);
        command.setContentType(MediaType.TEXT_PLAIN_VALUE);
        command.setPurpose("contract");
        command.setAccessLevel("PRIVATE");
        command.setBizType("ORDER");
        command.setBizId("A-1");
        command.setBizMeta("{\"source\":\"test\"}");
        command.setDirectoryId(8L);
        return command;
    }
}
