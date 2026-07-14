package io.mango.cms.starter.controller;

import io.mango.cms.api.query.SiteResolveQuery;
import io.mango.cms.core.service.ICmsSiteService;
import io.mango.file.api.vo.FileDownloadVO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CmsPublicFileHttpContractTest {

    @Test
    void publicPreview_keepsContentTypeLengthDispositionAndBody() throws Exception {
        ICmsSiteService service = mock(ICmsSiteService.class);
        SiteResolveQuery query = new SiteResolveQuery();
        query.setDomain("www.example.test");
        byte[] bytes = new byte[]{1, 2, 3, 4};
        when(service.publicFile(99L, query)).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(bytes), "站点 logo.png", "image/png", bytes.length));
        CmsSiteController controller = new CmsSiteController(service);

        ResponseEntity<InputStreamResource> response = controller.publicFile(99L, query);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(bytes.length);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("inline;")
                .contains("filename*=UTF-8''");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).containsExactly(bytes);
    }

    @Test
    void publicPreview_fallsBackToOctetStreamForMissingContentType() {
        ICmsSiteService service = mock(ICmsSiteService.class);
        SiteResolveQuery query = new SiteResolveQuery();
        query.setDomain("www.example.test");
        when(service.publicFile(100L, query)).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(new byte[0]), "empty.bin", " ", 0L));
        CmsSiteController controller = new CmsSiteController(service);

        ResponseEntity<InputStreamResource> response = controller.publicFile(100L, query);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getContentLength()).isZero();
    }
}
