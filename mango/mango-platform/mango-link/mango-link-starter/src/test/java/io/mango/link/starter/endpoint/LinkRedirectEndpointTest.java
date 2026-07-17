package io.mango.link.starter.endpoint;

import io.mango.link.core.service.ILinkOpenService;
import io.mango.link.core.service.LinkJumpContext;
import io.mango.link.core.service.LinkRedirectContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.routerFunctions;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

class LinkRedirectEndpointTest {

    private ILinkOpenService linkOpenService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        linkOpenService = mock(ILinkOpenService.class);
        LinkRedirectEndpoint endpoint = new LinkRedirectEndpoint(linkOpenService);
        RouterFunction<ServerResponse> routes = route(GET("/link/open/redirect"), endpoint::redirect)
                .andRoute(GET("/link/open/jump"), endpoint::jump);
        mockMvc = routerFunctions(routes).build();
    }

    @Test
    void redirectReturnsRaw302AndTransfersRequestMetadata() throws Exception {
        when(linkOpenService.resolveRedirectUrl(any())).thenReturn("https://target.example.com/path");

        mockMvc.perform(get("/link/open/redirect")
                        .queryParam("id", "123")
                        .queryParam("source", "COMPANY")
                        .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
                        .header("User-Agent", "endpoint-test")
                        .header("Referer", "https://origin.example.com"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://target.example.com/path"));

        ArgumentCaptor<LinkRedirectContext> captor = ArgumentCaptor.forClass(LinkRedirectContext.class);
        org.mockito.Mockito.verify(linkOpenService).resolveRedirectUrl(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(123L);
        assertThat(captor.getValue().getSource()).isEqualTo("COMPANY");
        assertThat(captor.getValue().getClientIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void jumpExcludesSensitiveParametersFromRecordedMetadata() throws Exception {
        when(linkOpenService.resolveJumpUrl(any())).thenReturn("https://target.example.com");

        mockMvc.perform(get("/link/open/jump")
                        .queryParam("url", "https://target.example.com")
                        .queryParam("uid", "visitor")
                        .queryParam("source", "PUBLIC")
                        .queryParam("campaign", "summer")
                        .queryParam("token", "must-not-leak"))
                .andExpect(status().isFound());

        ArgumentCaptor<LinkJumpContext> captor = ArgumentCaptor.forClass(LinkJumpContext.class);
        org.mockito.Mockito.verify(linkOpenService).resolveJumpUrl(captor.capture());
        assertThat(captor.getValue().getExtraParams()).isEqualTo("campaign=summer");
        assertThat(captor.getValue().getVisitorId()).isEqualTo("visitor");
    }
}
