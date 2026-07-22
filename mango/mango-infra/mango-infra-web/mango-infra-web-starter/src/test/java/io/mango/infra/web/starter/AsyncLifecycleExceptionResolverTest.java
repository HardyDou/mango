package io.mango.infra.web.starter;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AsyncLifecycleExceptionResolverTest {

    private final AsyncLifecycleExceptionResolver resolver = new AsyncLifecycleExceptionResolver();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void timeoutBeforeCommit_preservesSpringServiceUnavailableSemantics() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView result = resolver.resolveException(request, response, null,
                new AsyncRequestTimeoutException());

        assertNotNull(result);
        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }

    @Test
    void timeoutAfterCommit_doesNotRewriteTheStreamingResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCommitted(true);

        ModelAndView result = resolver.resolveException(request, response, null,
                new AsyncRequestTimeoutException());

        assertNotNull(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void unusableAsyncResponseAndDisconnectedClient_areHandledWithoutJson() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertNotNull(resolver.resolveException(request, response, null,
                new AsyncRequestNotUsableException("response is no longer usable")));
        assertNotNull(resolver.resolveException(request, response, null,
                new IOException("Broken pipe")));
    }

    @Test
    void unrelatedException_isDelegatedToNormalExceptionResolvers() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertNull(resolver.resolveException(request, response, null,
                new IllegalStateException("business failure")));
    }
}
