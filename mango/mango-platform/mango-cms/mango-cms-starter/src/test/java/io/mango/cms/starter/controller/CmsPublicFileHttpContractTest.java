package io.mango.cms.starter.controller;

import io.mango.cms.core.service.ICmsSiteService;
import io.mango.cms.starter.endpoint.CmsPublicFileEndpoint;
import io.mango.file.api.vo.FileDownloadVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CmsPublicFileHttpContractTest {

    @Test
    void publicPreview_keepsContentTypeLengthDispositionAndBody() throws Exception {
        ICmsSiteService service = mock(ICmsSiteService.class);
        byte[] bytes = new byte[]{1, 2, 3, 4};
        when(service.publicFile(eq(99L), any())).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(bytes), "站点 logo.png", "image/png", bytes.length));
        CmsPublicFileEndpoint endpoint = new CmsPublicFileEndpoint(service);
        MockMvc mvc = MockMvcBuilders.routerFunctions(
                route(GET("/cms/open/files/public-preview"), endpoint::handle)).build();

        byte[] body = mvc.perform(get("/cms/open/files/public-preview")
                        .queryParam("id", "99")
                        .queryParam("domain", "www.example.test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, bytes.length))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(body).containsExactly(bytes);
    }

    @Test
    void publicPreview_fallsBackToOctetStreamForMissingContentType() throws Exception {
        ICmsSiteService service = mock(ICmsSiteService.class);
        when(service.publicFile(eq(100L), any())).thenReturn(new FileDownloadVO(
                new ByteArrayInputStream(new byte[0]), "empty.bin", " ", 0L));
        CmsPublicFileEndpoint endpoint = new CmsPublicFileEndpoint(service);
        MockMvc mvc = MockMvcBuilders.routerFunctions(
                route(GET("/cms/open/files/public-preview"), endpoint::handle)).build();

        mvc.perform(get("/cms/open/files/public-preview")
                        .queryParam("id", "100")
                        .queryParam("domain", "www.example.test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 0L));
    }
}
